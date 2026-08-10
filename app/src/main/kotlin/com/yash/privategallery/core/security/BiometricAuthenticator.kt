package com.yash.privategallery.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.yash.privategallery.domain.model.BiometricClass
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class BiometricAuthOutcome {
    data object Success : BiometricAuthOutcome()
    data object Cancelled : BiometricAuthOutcome()
    data class Error(val message: String) : BiometricAuthOutcome()
}

/**
 * Thin coroutine-friendly wrapper over androidx.biometric.BiometricPrompt
 * (Section 3, 4). Never reads, stores, or has any access to actual biometric
 * templates — that's entirely handled by the OS's biometric subsystem behind
 * BiometricPrompt's opaque success/failure callback, which is exactly why the
 * app can support "Fingerprint A → Normal, Fingerprint B → Private" framing in
 * its UI copy while never actually distinguishing individual fingerprints
 * itself (the OS does that matching internally; the app only asks "is the
 * user who's currently authenticated on this device allowed in?").
 */
@Singleton
class BiometricAuthenticator @Inject constructor() {

    fun availability(activity: FragmentActivity, biometricClass: BiometricClass): Int {
        val manager = BiometricManager.from(activity)
        val authenticators = authenticatorsFor(biometricClass)
        return manager.canAuthenticate(authenticators)
    }

    fun isAvailable(activity: FragmentActivity, biometricClass: BiometricClass): Boolean {
        return availability(activity, biometricClass) == BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        biometricClass: BiometricClass
    ): BiometricAuthOutcome = suspendCoroutine { continuation ->
        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val authenticators = authenticatorsFor(biometricClass)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                continuation.resume(BiometricAuthOutcome.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val outcome = if (
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    BiometricAuthOutcome.Cancelled
                } else {
                    BiometricAuthOutcome.Error(errString.toString())
                }
                continuation.resume(outcome)
            }

            // onAuthenticationFailed (a single failed match attempt, not a terminal
            // error) is intentionally not resumed here — BiometricPrompt keeps the
            // sheet open for retry, exactly matching Section 43's "wrong PIN
            // handling" expectation applied to biometrics.
        }

        val prompt = BiometricPrompt(activity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)

        // A negative/cancel button is only valid when DEVICE_CREDENTIAL is NOT
        // among the allowed authenticators (the two are mutually exclusive in
        // the BiometricPrompt API).
        if (authenticators and DEVICE_CREDENTIAL == 0) {
            promptInfoBuilder.setNegativeButtonText("Cancel")
        }

        prompt.authenticate(promptInfoBuilder.build())
    }

    private fun authenticatorsFor(biometricClass: BiometricClass): Int = when (biometricClass) {
        BiometricClass.CLASS_STRONG -> BIOMETRIC_STRONG
        BiometricClass.CLASS_WEAK -> BIOMETRIC_WEAK or BIOMETRIC_STRONG
        BiometricClass.DEVICE_CREDENTIAL_FALLBACK -> BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    }
}
