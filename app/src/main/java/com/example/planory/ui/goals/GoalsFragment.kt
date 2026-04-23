package com.example.planory.ui.goals

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.planory.R
import com.example.planory.data.AppDatabase
import com.example.planory.data.entity.GoalEntity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GoalsFragment : Fragment(R.layout.fragment_goals) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etGoalName = view.findViewById<EditText>(R.id.etGoalName)
        val etGoalTotal = view.findViewById<EditText>(R.id.etGoalTotal)
        val etGoalSaved = view.findViewById<EditText>(R.id.etGoalSaved)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveGoal)
        val rvGoals = view.findViewById<RecyclerView>(R.id.rvGoals)

        val goalDao = AppDatabase.getDatabase(requireContext()).goalDao()

        val adapter = GoalsAdapter(mutableListOf())
        rvGoals.layoutManager = LinearLayoutManager(requireContext())
        rvGoals.adapter = adapter

        val user = FirebaseAuth.getInstance().currentUser ?: return

        // ✅ Observe only current user's goals
        viewLifecycleOwner.lifecycleScope.launch {
            goalDao.getGoalsForUser(user.uid).collectLatest { goals ->
                adapter.updateData(goals)
            }
        }

        btnSave.setOnClickListener {

            val title = etGoalName.text.toString().trim()
            val total = etGoalTotal.text.toString().toIntOrNull()
            val saved = etGoalSaved.text.toString().toIntOrNull()

            if (title.isEmpty() || total == null || saved == null) {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (saved > total) {
                Toast.makeText(requireContext(), "Saved cannot exceed total", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {

                val user = FirebaseAuth.getInstance().currentUser ?: return@launch

                goalDao.insertGoal(
                    GoalEntity(
                        title = title,
                        totalAmount = total,
                        savedAmount = saved,
                        userId = user.uid   // ✅ REQUIRED
                    )
                )

                etGoalName.text.clear()
                etGoalTotal.text.clear()
                etGoalSaved.text.clear()
            }
        }
    }
}
