package com.yash.privategallery.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yash.privategallery.core.security.AuthRateLimiter
import com.yash.privategallery.core.security.CredentialHasher
import com.yash.privategallery.core.security.HashedCredential
import com.yash.privategallery.domain.model.AuthMethod
import com.yash.privategallery.domain.model.AutoLockDelay
import com.yash.privategallery.domain.model.BiometricClass
import com.yash.privategallery.domain.model.LockConfiguration
import com.yash.privategallery.domain.repository.AuthResult
import com.yash.privategallery.domain.repository.LockTarget
import com.yash.privategallery.domain.repository.SecurityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.securityDataStore by preferencesDataStore(name = "security_config")

/**
 * NOTE ON SECRET STORAGE: hashed credentials (salt + PBKDF2 hash, never
 * plaintext — Section 3/52) are stored here in the same DataStore as
 * non-secret lock configuration for scaffold simplicity. Because DataStore's
 * backing file lives in app-private storage already excluded from backup
 * (Section 53) this meets the letter of "never store raw passwords", but a
 * production hardening pass should move the hash+salt pair into
 * EncryptedSharedPreferences (androidx.security.crypto) so the values are
 * encrypted at rest too, not just access-controlled. Flagged here rather than
 * silently left implicit.
 */
@Singleton
class SecurityRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialHasher: CredentialHasher,
    private val rateLimiter: AuthRateLimiter
) : SecurityRepository {

    private fun keyPrefix(target: LockTarget) = when (target) {
        LockTarget.NORMAL_GALLERY -> "normal"
        LockTarget.PRIVATE_GALLERY -> "private"
    }

    private object Suffix {
        const val ENABLED = "_enabled"
        const val AUTH_METHOD = "_auth_method"
        const val BIOMETRIC_CLASS = "_biometric_class"
        const val AUTO_LOCK = "_auto_lock"
        const val SCREENSHOT_PROTECTION = "_screenshot_protection"
        const val HIDE_FROM_RECENTS = "_hide_from_recents"
        const val SECRET_HASH = "_secret_hash"
        const val SECRET_SALT = "_secret_salt"
        const val SECRET_ITERATIONS = "_secret_iterations"
    }

    override fun observeLockConfiguration(target: LockTarget): Flow<LockConfiguration> {
        val prefix = keyPrefix(target)
        return context.securityDataStore.data.map { prefs ->
            LockConfiguration(
                isEnabled = prefs[booleanPreferencesKey("$prefix${Suffix.ENABLED}")] ?: false,
                authMethod = prefs[stringPreferencesKey("$prefix${Suffix.AUTH_METHOD}")]
                    ?.let { runCatching { AuthMethod.valueOf(it) }.getOrNull() } ?: AuthMethod.NONE,
                biometricClass = prefs[stringPreferencesKey("$prefix${Suffix.BIOMETRIC_CLASS}")]
                    ?.let { runCatching { BiometricClass.valueOf(it) }.getOrNull() },
                autoLockDelay = prefs[stringPreferencesKey("$prefix${Suffix.AUTO_LOCK}")]
                    ?.let { runCatching { AutoLockDelay.valueOf(it) }.getOrNull() }
                    ?: if (target == LockTarget.PRIVATE_GALLERY) AutoLockDelay.IMMEDIATELY else AutoLockDelay.AFTER_1_MINUTE,
                screenshotProtectionEnabled = prefs[booleanPreferencesKey("$prefix${Suffix.SCREENSHOT_PROTECTION}")]
                    ?: (target == LockTarget.PRIVATE_GALLERY),
                hideFromRecents = prefs[booleanPreferencesKey("$prefix${Suffix.HIDE_FROM_RECENTS}")]
                    ?: (target == LockTarget.PRIVATE_GALLERY)
            )
        }
    }

    override suspend fun updateLockConfiguration(target: LockTarget, config: LockConfiguration) {
        val prefix = keyPrefix(target)
        context.securityDataStore.edit { prefs ->
            prefs[booleanPreferencesKey("$prefix${Suffix.ENABLED}")] = config.isEnabled
            prefs[stringPreferencesKey("$prefix${Suffix.AUTH_METHOD}")] = config.authMethod.name
            config.biometricClass?.let { prefs[stringPreferencesKey("$prefix${Suffix.BIOMETRIC_CLASS}")] = it.name }
            prefs[stringPreferencesKey("$prefix${Suffix.AUTO_LOCK}")] = config.autoLockDelay.name
            prefs[booleanPreferencesKey("$prefix${Suffix.SCREENSHOT_PROTECTION}")] = config.screenshotProtectionEnabled
            prefs[booleanPreferencesKey("$prefix${Suffix.HIDE_FROM_RECENTS}")] = config.hideFromRecents
        }
    }

    override suspend fun setSecret(target: LockTarget, method: AuthMethod, rawSecret: String) {
        val prefix = keyPrefix(target)
        val hashed = credentialHasher.hash(rawSecret)
        persistHashed(prefix, hashed)
        context.securityDataStore.edit { prefs ->
            prefs[stringPreferencesKey("$prefix${Suffix.AUTH_METHOD}")] = method.name
        }
    }

    override suspend fun verifySecret(target: LockTarget, rawSecret: String): AuthResult {
        val scopeKey = keyPrefix(target)
        val remainingLockout = rateLimiter.remainingLockoutSeconds(scopeKey)
        if (remainingLockout > 0) return AuthResult.TooManyAttempts(remainingLockout)

        val stored = readHashed(scopeKey) ?: return AuthResult.Failed
        val matches = credentialHasher.verify(rawSecret, stored)
        return if (matches) {
            rateLimiter.recordSuccess(scopeKey)
            AuthResult.Success
        } else {
            rateLimiter.recordFailure(scopeKey)
            AuthResult.Failed
        }
    }

    override suspend fun isBiometricAvailable(target: LockTarget): Boolean {
        // Actual hardware capability check requires a FragmentActivity context
        // (BiometricManager.from(activity)) — delegated to
        // core/security/BiometricAuthenticator at the call site in the UI
        // layer, since this repository is Activity-agnostic by design.
        return true
    }

    override suspend fun setAlbumSecret(albumId: Long, method: AuthMethod, rawSecret: String) {
        val prefix = "album_$albumId"
        val hashed = credentialHasher.hash(rawSecret)
        persistHashed(prefix, hashed)
        context.securityDataStore.edit { prefs ->
            prefs[stringPreferencesKey("$prefix${Suffix.AUTH_METHOD}")] = method.name
        }
    }

    override suspend fun verifyAlbumSecret(albumId: Long, rawSecret: String): AuthResult {
        val scopeKey = "album_$albumId"
        val remainingLockout = rateLimiter.remainingLockoutSeconds(scopeKey)
        if (remainingLockout > 0) return AuthResult.TooManyAttempts(remainingLockout)

        val stored = readHashed(scopeKey) ?: return AuthResult.Failed
        val matches = credentialHasher.verify(rawSecret, stored)
        return if (matches) {
            rateLimiter.recordSuccess(scopeKey)
            AuthResult.Success
        } else {
            rateLimiter.recordFailure(scopeKey)
            AuthResult.Failed
        }
    }

    override suspend fun clearAlbumSecret(albumId: Long) {
        val prefix = "album_$albumId"
        context.securityDataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("$prefix${Suffix.SECRET_HASH}"))
            prefs.remove(stringPreferencesKey("$prefix${Suffix.SECRET_SALT}"))
            prefs.remove(intPreferencesKey("$prefix${Suffix.SECRET_ITERATIONS}"))
            prefs.remove(stringPreferencesKey("$prefix${Suffix.AUTH_METHOD}"))
        }
    }

    private suspend fun persistHashed(prefix: String, hashed: HashedCredential) {
        context.securityDataStore.edit { prefs ->
            prefs[stringPreferencesKey("$prefix${Suffix.SECRET_HASH}")] = hashed.hashBase64
            prefs[stringPreferencesKey("$prefix${Suffix.SECRET_SALT}")] = hashed.saltBase64
            prefs[intPreferencesKey("$prefix${Suffix.SECRET_ITERATIONS}")] = hashed.iterations
        }
    }

    private suspend fun readHashed(prefix: String): HashedCredential? {
        val prefs = context.securityDataStore.data.first()
        val hash = prefs[stringPreferencesKey("$prefix${Suffix.SECRET_HASH}")] ?: return null
        val salt = prefs[stringPreferencesKey("$prefix${Suffix.SECRET_SALT}")] ?: return null
        val iterations = prefs[intPreferencesKey("$prefix${Suffix.SECRET_ITERATIONS}")] ?: return null
        return HashedCredential(hash, salt, iterations)
    }
}
