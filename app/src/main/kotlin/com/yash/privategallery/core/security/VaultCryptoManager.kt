package com.yash.privategallery.core.security

import android.security.keystore.KeyProperties
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of encrypting a source stream into the vault: where it was written,
 * its verified size, and a SHA-256 of the *plaintext* used to verify integrity
 * after a round-trip decrypt (Section 57's "verify" step).
 */
data class EncryptionResult(
    val encryptedFile: File,
    val plaintextSizeBytes: Long,
    val plaintextSha256: String,
    val wrappedDekBase64: String,
    val ivBase64: String
)

/**
 * Handles the actual AES-256-GCM envelope encryption for private media files
 * (Section 2, 41, 52).
 *
 * Envelope encryption: each file gets its own random 256-bit Data Encryption
 * Key (DEK). The DEK itself is encrypted ("wrapped") by the Keystore-resident
 * key from [KeystoreManager] and stored (as ciphertext) alongside the file's
 * metadata row in the private database — never in plaintext, and the DEK
 * plaintext exists only transiently in memory during a single encrypt/decrypt
 * operation. This means:
 *   - Bulk file crypto uses a fast software AES implementation (no per-byte
 *     Keystore round-trip, which would be far too slow for photo/video files).
 *   - The DEK is still only ever recoverable via the hardware-backed Keystore
 *     key, so the security property of "keys never leave secure hardware in
 *     usable form" is preserved for what actually matters (the key that can
 *     unwrap everything).
 */
@Singleton
class VaultCryptoManager @Inject constructor(
    private val keystoreManager: KeystoreManager
) {
    private val secureRandom = SecureRandom()

    /**
     * Encrypts [input] into [destinationFile] using a freshly generated DEK,
     * which is itself wrapped by the Keystore key and returned (base64) for
     * the caller to persist in the private DB row. Returns everything needed
     * later both to decrypt the file and to verify it (Section 57 step 2).
     */
    fun encryptToVault(input: InputStream, destinationFile: File): EncryptionResult {
        val dek = ByteArray(32).also { secureRandom.nextBytes(it) }
        val dekKeySpec = SecretKeySpec(dek, KeyProperties.KEY_ALGORITHM_AES)

        val iv = ByteArray(12).also { secureRandom.nextBytes(it) } // 96-bit GCM IV
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, dekKeySpec, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        val digest = MessageDigest.getInstance("SHA-256")
        var plaintextSize = 0L

        destinationFile.outputStream().use { rawOut ->
            CipherOutputStream(rawOut, cipher).use { cipherOut ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                    cipherOut.write(buffer, 0, read)
                    plaintextSize += read
                }
            }
        }

        val wrappedDek = wrapDek(dek)
        dek.fill(0) // best-effort scrub of the plaintext DEK from memory

        return EncryptionResult(
            encryptedFile = destinationFile,
            plaintextSizeBytes = plaintextSize,
            plaintextSha256 = digest.digest().joinToString("") { "%02x".format(it) },
            wrappedDekBase64 = android.util.Base64.encodeToString(wrappedDek, android.util.Base64.NO_WRAP),
            ivBase64 = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP)
        )
    }

    /**
     * Opens an encrypted vault file for streaming plaintext read. Used both
     * for actually displaying/playing private media AND for the Section 57
     * verification step (re-read + re-hash immediately after encrypt, before
     * the public original is ever deleted).
     */
    fun decryptFromVault(encryptedFile: File, wrappedDekBase64: String, ivBase64: String): CipherInputStream {
        val dek = unwrapDek(android.util.Base64.decode(wrappedDekBase64, android.util.Base64.NO_WRAP))
        val iv = android.util.Base64.decode(ivBase64, android.util.Base64.NO_WRAP)
        val dekKeySpec = SecretKeySpec(dek, KeyProperties.KEY_ALGORITHM_AES)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, dekKeySpec, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        return CipherInputStream(encryptedFile.inputStream(), cipher)
    }

    /**
     * Verifies a just-encrypted file by decrypting it back and comparing the
     * SHA-256 against [expectedSha256] — the concrete implementation of
     * Section 57's mandatory "verify" step before any source deletion happens.
     */
    fun verify(encryptedFile: File, wrappedDekBase64: String, ivBase64: String, expectedSha256: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            decryptFromVault(encryptedFile, wrappedDekBase64, ivBase64).use { stream ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = stream.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            actual == expectedSha256
        } catch (e: Exception) {
            // Any failure (corrupt file, auth tag mismatch, IO error) means
            // verification failed — treated as "not safe to delete original".
            false
        }
    }

    private fun wrapDek(dek: ByteArray): ByteArray {
        val wrappingKey = keystoreManager.getOrCreateVaultWrappingKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
        val ciphertext = cipher.doFinal(dek)
        // Prepend the IV Keystore generated, since we need it to unwrap later.
        return cipher.iv + ciphertext
    }

    private fun unwrapDek(wrapped: ByteArray): ByteArray {
        val wrappingKey = keystoreManager.getOrCreateVaultWrappingKey()
        val iv = wrapped.copyOfRange(0, 12)
        val ciphertext = wrapped.copyOfRange(12, wrapped.size)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private companion object {
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
