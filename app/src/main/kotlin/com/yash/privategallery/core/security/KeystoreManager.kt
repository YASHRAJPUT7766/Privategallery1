package com.yash.privategallery.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns creation and retrieval of AES-256-GCM keys inside the Android Keystore
 * (Section 3, 41, 52: "Use Android Keystore for cryptographic keys"). Keys
 * generated here are hardware-backed where the device supports it and are
 * NEVER exportable — only usable in-place for encrypt/decrypt operations,
 * which is exactly the property that makes it safe to use Keystore as the
 * root of trust for the private vault's data-encryption key.
 *
 * One key alias per logical purpose, so compromising/rotating one (e.g. the
 * private-vault media key) never affects another (e.g. a future backup key).
 */
@Singleton
class KeystoreManager @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Returns the private vault's data-encryption key, generating it on first
     * use. This key encrypts the DEK (data encryption key) used for bulk file
     * encryption — see [VaultCryptoManager] — rather than encrypting every
     * media file directly with a Keystore key, since Keystore key operations
     * are comparatively slow for large payloads. This is the standard
     * envelope-encryption pattern.
     */
    fun getOrCreateVaultWrappingKey(): SecretKey {
        (keyStore.getKey(VAULT_WRAPPING_KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            VAULT_WRAPPING_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // Not tied to biometric auth: the vault must remain readable by the
            // app's own logic even when the user hasn't just authenticated
            // (e.g. a background rescan), since access control to *content* is
            // enforced at the UI/navigation layer (lock screens), not by gating
            // the raw decrypt key on every read. This mirrors how full-disk
            // encryption separates "device unlocked" from "app unlocked".
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /** Deletes the vault wrapping key — used only by a full "reset private space" flow. */
    fun deleteVaultWrappingKey() {
        if (keyStore.containsAlias(VAULT_WRAPPING_KEY_ALIAS)) {
            keyStore.deleteEntry(VAULT_WRAPPING_KEY_ALIAS)
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val VAULT_WRAPPING_KEY_ALIAS = "private_gallery_vault_wrapping_key_v1"
    }
}
