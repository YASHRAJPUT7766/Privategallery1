package com.yash.privategallery.core.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.rateLimitDataStore by preferencesDataStore(name = "auth_rate_limit")

/**
 * Enforces exponential-backoff lockout after repeated failed authentication
 * attempts (Section 43: "wrong PIN handling, secure lockout/rate limiting").
 * State is persisted (survives process death) and keyed per lock scope string
 * so the normal gallery, private gallery, and each locked album maintain
 * independent attempt counters — a lockout on one never blocks another.
 *
 * Backoff schedule: attempts 1-4 have no delay, attempt 5 triggers 30s, then
 * doubles each subsequent failure up to a 30-minute cap. This mirrors common
 * mobile OS lock-screen behavior and balances usability against brute-force
 * resistance for a low-entropy secret like a PIN.
 */
@Singleton
class AuthRateLimiter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun recordFailure(scopeKey: String) {
        val countKey = intPreferencesKey("${scopeKey}_fail_count")
        val lockedUntilKey = longPreferencesKey("${scopeKey}_locked_until")

        context.rateLimitDataStore.edit { prefs ->
            val newCount = (prefs[countKey] ?: 0) + 1
            prefs[countKey] = newCount
            if (newCount >= FREE_ATTEMPTS) {
                val backoffIndex = (newCount - FREE_ATTEMPTS).coerceAtMost(BACKOFF_SECONDS.size - 1)
                val delaySeconds = BACKOFF_SECONDS[backoffIndex]
                prefs[lockedUntilKey] = System.currentTimeMillis() + delaySeconds * 1000L
            }
        }
    }

    suspend fun recordSuccess(scopeKey: String) {
        val countKey = intPreferencesKey("${scopeKey}_fail_count")
        val lockedUntilKey = longPreferencesKey("${scopeKey}_locked_until")
        context.rateLimitDataStore.edit { prefs ->
            prefs.remove(countKey)
            prefs.remove(lockedUntilKey)
        }
    }

    /** Returns seconds remaining until [scopeKey] may attempt auth again, or 0 if unlocked. */
    suspend fun remainingLockoutSeconds(scopeKey: String): Long {
        val lockedUntilKey = longPreferencesKey("${scopeKey}_locked_until")
        val lockedUntil = context.rateLimitDataStore.data.first()[lockedUntilKey] ?: 0L
        val remainingMs = lockedUntil - System.currentTimeMillis()
        return if (remainingMs > 0) (remainingMs / 1000L) + 1 else 0L
    }

    private companion object {
        const val FREE_ATTEMPTS = 5
        val BACKOFF_SECONDS = listOf(30L, 60L, 120L, 300L, 600L, 1800L)
    }
}
