package com.example.planory.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.planory.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val tvSignup = view.findViewById<View>(R.id.tvSignup)

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        Toast.makeText(
                            requireContext(),
                            "Login successful",
                            Toast.LENGTH_SHORT
                        ).show()

                        // TEMP navigation (Splash will handle this later)
                        findNavController().navigate(
                            R.id.dashboardFragment,
                            null,
                            androidx.navigation.NavOptions.Builder()
                                .setPopUpTo(R.id.loginFragment, true)
                                .build()
                        )

                    } else {
                        Toast.makeText(
                            requireContext(),
                            task.exception?.message ?: "Login failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        tvSignup.setOnClickListener {
            findNavController().navigate(R.id.signupFragment)
        }
    }
}
