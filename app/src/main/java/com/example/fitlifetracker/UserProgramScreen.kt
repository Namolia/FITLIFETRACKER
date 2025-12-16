package com.example.fitlifetracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun UserProgramScreen(
    plan: Plan,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var daysCount by remember { mutableStateOf(0L) }
    var selectedDay by remember { mutableStateOf(1) }
    var dayTitle by remember { mutableStateOf("") }
    var exercises by remember { mutableStateOf(listOf<ExerciseItem>()) }

    fun loadDaysCount() {
        db.collection("plans").document(plan.id).get()
            .addOnSuccessListener { doc ->
                daysCount = doc.getLong("daysCount") ?: 0L
                if (daysCount > 0 && selectedDay > daysCount.toInt()) selectedDay = 1
            }
    }

    fun listenDay(dayIndex: Int) {
        val dayRef = db.collection("plans").document(plan.id)
            .collection("days").document(dayIndex.toString())

        dayRef.addSnapshotListener { doc, _ ->
            if (doc == null || !doc.exists()) {
                dayTitle = "Gün $dayIndex"
                exercises = emptyList()
                return@addSnapshotListener
            }
            dayTitle = doc.getString("dayTitle") ?: "Gün $dayIndex"
            val arr = doc.get("exercises") as? List<*>
            exercises = arr?.mapNotNull { it as? Map<*, *> }?.map { m ->
                ExerciseItem(
                    name = (m["name"] as? String) ?: "",
                    sets = (m["sets"] as? String) ?: "",
                    reps = (m["reps"] as? String) ?: "",
                    note = (m["note"] as? String) ?: ""
                )
            } ?: emptyList()
        }
    }

    LaunchedEffect(Unit) { loadDaysCount() }
    LaunchedEffect(selectedDay) { listenDay(selectedDay) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Program", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text("Plan: ${plan.name}")
        Spacer(Modifier.height(12.dp))

        if (daysCount <= 0) {
            Text("Bu plan için henüz program oluşturulmamış.")
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { if (selectedDay > 1) selectedDay -= 1 }, enabled = selectedDay > 1) {
                    Text("<")
                }

                Text("Gün $selectedDay / $daysCount", style = MaterialTheme.typography.titleMedium)

                Button(
                    onClick = { if (selectedDay < daysCount.toInt()) selectedDay += 1 },
                    enabled = selectedDay < daysCount.toInt()
                ) { Text(">") }
            }

            Spacer(Modifier.height(12.dp))
            Text(dayTitle, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            if (exercises.isEmpty()) {
                Text("Bu güne egzersiz eklenmemiş.")
            } else {
                exercises.forEachIndexed { i, ex ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${i + 1}) ${ex.name}", style = MaterialTheme.typography.titleMedium)
                            Text("${ex.sets} set | ${ex.reps} tekrar")
                            if (ex.note.isNotBlank()) Text("Not: ${ex.note}")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Geri")
        }
    }
}
