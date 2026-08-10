package com.yash.privategallery.domain.repository

import com.yash.privategallery.domain.model.AuthMethod
import com.yash.privategallery.domain.model.LockConfiguration
import kotlinx.coroutines.flow.Flow

/** Which lock target a security operation applies to (Section 3: fully independent). */
enum class LockTarget {
    NORMAL_GALLERY,
    PRIVATE_GALLERY
}

/** Outcome of a credential verification attempt. Rate limiting (Section 43) is enforced
 *  by the implementation, not the caller — [TooManyAttempts] surfaces the cooldown. */
sealed class AuthResult {
    data object Success : AuthResult()
    data object Failed : AuthResult()
    data class TooManyAttempts(val retryAfterSeconds: Long) : AuthResult()
}

/**
 * Manages lock configuration and credential verification for the normal gallery,
 * private gallery, and (via [LockTarget]-independent album-scoped methods below)
 * individual locked albums.
 *
 * IMPORTANT: this interface never accepts or returns raw PIN/password/pattern
 * strings beyond the single call that sets or verifies them in the same
 * operation — nothing here persists a plaintext secret anywhere, ever
 * (Section 3, 52). The actual hashing/key-derivation lives behind the
 * implementation, backed by core/security (Android Keystore).
 */
interface SecurityRepository {

    fun observeLockConfiguration(target: LockTarget): Flow<LockConfiguration>

    suspend fun updateLockConfiguration(target: LockTarget, config: LockConfiguration)

    /**
     * Sets or replaces the secret for [target]. [rawSecret] is consumed
     * (hashed via a secure KDF, e.g. Argon2id or PBKDF2 backed by Keystore-
     * wrapped key material) and never retained or logged in plaintext form.
     */
    suspend fun setSecret(target: LockTarget, method: AuthMethod, rawSecret: String)

    /** Verifies a PIN/password/pattern against the stored hash for [target]. */
    suspend fun verifySecret(target: LockTarget, rawSecret: String): AuthResult

    /** Whether biometric hardware capable of the configured [BiometricClass] is available. */
    suspend fun isBiometricAvailable(target: LockTarget): Boolean

    // --- Per-album locking uses the same underlying secure storage, keyed by albumId. ---

    suspend fun setAlbumSecret(albumId: Long, method: AuthMethod, rawSecret: String)

    suspend fun verifyAlbumSecret(albumId: Long, rawSecret: String): AuthResult

    suspend fun clearAlbumSecret(albumId: Long)
}
