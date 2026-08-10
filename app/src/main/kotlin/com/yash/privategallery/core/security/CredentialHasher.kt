package com.yash.privategallery.core.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/** A hashed credential ready for storage — never contains the original secret. */
data class HashedCredential(
    val hashBase64: String,
    val saltBase64: String,
    val iterations: Int
)

/**
 * Hashes and verifies PIN/password/pattern secrets using PBKDF2WithHmacSHA256
 * (Section 3, 52: "Never store raw passwords/PINs. Use secure hashing/key
 * derivation"). A pattern is first normalized to its dot-sequence string (e.g.
 * "0-1-2-5-8") by the caller before being passed here — this class only deals
 * in strings, agnostic to which UI produced them.
 *
 * PBKDF2 (rather than raw SHA-256) is used specifically because it's
 * deliberately slow and salted, making brute-force and rainbow-table attacks
 * on a stolen hash impractical — appropriate for a short, low-entropy secret
 * like a 4-6 digit PIN.
 */
@Singleton
class CredentialHasher @Inject constructor() {

    fun hash(rawSecret: String): HashedCredential {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(rawSecret, salt, ITERATIONS)
        return HashedCredential(
            hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP),
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            iterations = ITERATIONS
        )
    }

    fun verify(rawSecret: String, stored: HashedCredential): Boolean {
        val salt = Base64.decode(stored.saltBase64, Base64.NO_WRAP)
        val candidateHash = pbkdf2(rawSecret, salt, stored.iterations)
        val storedHash = Base64.decode(stored.hashBase64, Base64.NO_WRAP)
        return constantTimeEquals(candidateHash, storedHash)
    }

    private fun pbkdf2(secret: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    /** Constant-time comparison to avoid leaking hash-match info via timing side channels. */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    private companion object {
        const val ITERATIONS = 120_000
        const val KEY_LENGTH_BITS = 256
    }
}
