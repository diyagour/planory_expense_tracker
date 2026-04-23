package com.example.planory.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.planory.R
import com.example.planory.ui.auth.BiometricHelper
import com.google.firebase.auth.FirebaseAuth

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({

            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser == null) {
                // 🔓 Not logged in → go to login
                findNavController().navigate(
                    R.id.loginFragment,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.splashFragment, true)
                        .build()
                )
                return@postDelayed
            }

            // 🔐 Logged in → check biometric
            val biometricHelper = BiometricHelper(this)

            if (biometricHelper.isBiometricAvailable()) {
                biometricHelper.authenticate(
                    onSuccess = {
                        findNavController().navigate(
                            R.id.dashboardFragment,
                            null,
                            androidx.navigation.NavOptions.Builder()
                                .setPopUpTo(R.id.splashFragment, true)
                                .build()
                        )
                    },
                    onError = {
                        // User cancelled / failed → close app
                        requireActivity().finish()
                    }
                )
            } else {
                // 📱 Device has no biometric → allow access
                findNavController().navigate(
                    R.id.dashboardFragment,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.splashFragment, true)
                        .build()
                )
            }

        }, 4000)
    }
}
