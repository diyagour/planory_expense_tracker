package com.example.planory.ui.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class BiometricHelper(private val fragment: Fragment) {

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(fragment.requireContext())
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())

        val biometricPrompt = BiometricPrompt(
            fragment,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    onError()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Planory")
            .setSubtitle("Authenticate to access your expenses")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
