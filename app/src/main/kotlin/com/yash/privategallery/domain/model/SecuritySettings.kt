package com.yash.privategallery.domain.model

/** How long the app may sit in the background before re-authentication is required. */
enum class AutoLockDelay(val seconds: Long) {
    IMMEDIATELY(0L),
    AFTER_30_SECONDS(30L),
    AFTER_1_MINUTE(60L),
    AFTER_5_MINUTES(300L),
    NEVER(-1L)
}

/**
 * Which enrolled biometric class the user selected for a given lock target
 * (Section 4). We never touch raw biometric data ourselves — BiometricPrompt
 * handles matching against whatever the OS has enrolled — this only records
 * the user's *intended* class so BiometricPrompt can be configured with the
 * right allowed authenticators.
 */
enum class BiometricClass {
    CLASS_STRONG,   // fingerprint / face on hardware meeting BIOMETRIC_STRONG
    CLASS_WEAK,     // weaker on-device biometric implementations
    DEVICE_CREDENTIAL_FALLBACK
}

/**
 * Full lock configuration for one target: the normal gallery or the private gallery.
 * The two are configured completely independently (Section 3) — a
 * [LockConfiguration] instance always belongs to exactly one target and never
 * shares secret material with the other target's instance.
 */
data class LockConfiguration(
    val isEnabled: Boolean,
    val authMethod: AuthMethod,
    val biometricClass: BiometricClass?,
    val autoLockDelay: AutoLockDelay,
    val screenshotProtectionEnabled: Boolean,
    val hideFromRecents: Boolean
)
