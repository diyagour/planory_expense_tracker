package com.example.planory.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.planory.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase

class SignupFragment : Fragment(R.layout.fragment_signup) {

    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        val etUsername = view.findViewById<EditText>(R.id.etUsername) // Name
        val etEmail = view.findViewById<EditText>(R.id.etEmail)       // Email
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etConfirm = view.findViewById<EditText>(R.id.etConfirmPassword)
        val btnSignup = view.findViewById<MaterialButton>(R.id.btnSignup)

        btnSignup.setOnClickListener {

            val name = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm = etConfirm.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidPassword(password)) {
                Toast.makeText(
                    requireContext(),
                    "Password must be 8+ chars with uppercase, lowercase, number & symbol",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {

                        val user = auth.currentUser
                        val profileUpdates = userProfileChangeRequest {
                            displayName = name
                        }
                        user?.updateProfile(profileUpdates)

                        Toast.makeText(
                            requireContext(),
                            "Signup successful. Please login.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // ❌ DO NOT navigate yet
                        // ❌ DO NOT save session here
                    } else {
                        Toast.makeText(
                            requireContext(),
                            task.exception?.message ?: "Signup failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun isValidPassword(password: String): Boolean {
        val regex =
            Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!]).{8,}\$")
        return regex.matches(password)
    }
}
