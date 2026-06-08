package com.posbah.app.security

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object BiometricHelper {

    sealed class Result {
        object Success : Result()
        object Cancelled : Result()
        object Unavailable : Result()
        object NotEnrolled : Result()
        data class Failure(val code: Int, val message: String) : Result()
    }

    enum class Availability {
        AVAILABLE, NO_HARDWARE, HW_UNAVAILABLE, NONE_ENROLLED, SECURITY_UPDATE_REQUIRED, UNKNOWN
    }

    fun availability(activity: FragmentActivity): Availability {
        val manager = BiometricManager.from(activity)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
        return when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Availability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Availability.HW_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> Availability.SECURITY_UPDATE_REQUIRED
            else -> Availability.UNKNOWN
        }
    }

    /** Suspending biometric prompt. Returns once user authenticates or cancels. */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeText: String
    ): Result = suspendCancellableCoroutine { cont ->
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (cont.isActive) cont.resume(Result.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!cont.isActive) return
                val r = when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> Result.Cancelled
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> Result.NotEnrolled
                    BiometricPrompt.ERROR_HW_UNAVAILABLE,
                    BiometricPrompt.ERROR_HW_NOT_PRESENT -> Result.Unavailable
                    else -> Result.Failure(errorCode, errString.toString())
                }
                cont.resume(r)
            }

            override fun onAuthenticationFailed() {
                // The user attempted but failed — keep prompt visible, don't resume.
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }

        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            infoBuilder.setNegativeButtonText(negativeText)
        }

        prompt.authenticate(infoBuilder.build())
    }
}
