package com.example.fitlifetracker

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPlanProgramScreen(
    plan: Plan,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    // Plan düzeyi
    var daysCountText by remember { mutableStateOf("") }
    var currentDaysCount by remember { mutableStateOf(0L) }

    // ✅ Kapak fotoğrafı state
    var currentCoverUrl by remember { mutableStateOf(plan.imageUrl ?: "") }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    var uploadingCover by remember { mutableStateOf(false) }

    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedCoverUri = uri
    }

    // Gün düzeyi
    var selectedDay by remember { mutableStateOf(1) }
    var dayTitle by remember { mutableStateOf("") }

    // Egzersiz form
    var exName by remember { mutableStateOf("") }
    var exSets by remember { mutableStateOf("") }
    var exReps by remember { mutableStateOf("") }
    var exNote by remember { mutableStateOf("") }

    // Seçili günün egzersiz listesi
    var exercises by remember { mutableStateOf(listOf<ExerciseItem>()) }

    // ---- Firestore OKUMA ----
    fun loadPlanDaysCountAndCover() {
        db.collection("plans").document(plan.id).get()
            .addOnSuccessListener { doc ->
                val dc = doc.getLong("daysCount") ?: 0L
                currentDaysCount = dc
                if (daysCountText.isBlank() && dc > 0) {
                    daysCountText = dc.toString()
                }
                if (dc > 0 && selectedDay > dc.toInt()) selectedDay = 1

                // ✅ kapak url’i db’den güncelle
                currentCoverUrl = doc.getString("imageUrl") ?: ""
            }
            .addOnFailureListener {
                Toast.makeText(ctx, "Plan okunamadı: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    fun loadSelectedDay(dayIndex: Int) {
        val dayRef = db.collection("plans").document(plan.id)
            .collection("days").document(dayIndex.toString())

        dayRef.get()
            .addOnSuccessListener { doc ->
                if (doc == null || !doc.exists()) {
                    dayTitle = "Gün $dayIndex"
                    exercises = emptyList()
                    return@addOnSuccessListener
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
            .addOnFailureListener {
                Toast.makeText(ctx, "Gün verisi okunamadı: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ✅ Kapak yükleme
    fun uploadCover(uri: Uri) {
        uploadingCover = true
        val ref = storage.reference.child("plan_covers/${plan.id}_${System.currentTimeMillis()}.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        val url = downloadUri.toString()
                        db.collection("plans").document(plan.id)
                            .set(mapOf("imageUrl" to url), SetOptions.merge())
                            .addOnSuccessListener {
                                currentCoverUrl = url
                                selectedCoverUri = null
                                uploadingCover = false
                                Toast.makeText(ctx, "Kapak güncellendi ✅", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                uploadingCover = false
                                Toast.makeText(ctx, "Kapak kaydetme hatası: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        uploadingCover = false
                        Toast.makeText(ctx, "URL alma hatası: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                uploadingCover = false
                Toast.makeText(ctx, "Yükleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ✅ Kapak kaldırma
    fun removeCover() {
        db.collection("plans").document(plan.id)
            .set(mapOf("imageUrl" to ""), SetOptions.merge())
            .addOnSuccessListener {
                currentCoverUrl = ""
                selectedCoverUri = null
                Toast.makeText(ctx, "Kapak kaldırıldı ✅", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(ctx, "Kapak kaldırılamadı: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ---- Firestore KAYDET (GENEL KAYDET) ----
    fun saveAll() {
        val dc = daysCountText.toLongOrNull()

        if (dc == null || dc < 1) {
            Toast.makeText(ctx, "Gün sayısı en az 1 olmalı", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedDay > dc.toInt()) {
            Toast.makeText(ctx, "Seçili gün, gün sayısından büyük olamaz", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("plans").document(plan.id)
            .set(mapOf("daysCount" to dc), SetOptions.merge())
            .addOnSuccessListener {
                currentDaysCount = dc

                val dayRef = db.collection("plans").document(plan.id)
                    .collection("days").document(selectedDay.toString())

                val mapList = exercises.map {
                    mapOf(
                        "name" to it.name,
                        "sets" to it.sets,
                        "reps" to it.reps,
                        "note" to it.note
                    )
                }

                val payload = mapOf(
                    "dayTitle" to (dayTitle.ifBlank { "Gün $selectedDay" }),
                    "exercises" to mapList
                )

                dayRef.set(payload, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(ctx, "Genel Kaydet başarılı ✅", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(ctx, "Gün kaydetme hatası: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(ctx, "Plan kaydetme hatası: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // İlk açılış
    LaunchedEffect(Unit) {
        loadPlanDaysCountAndCover()
        loadSelectedDay(selectedDay)
    }

    // Gün değişince
    LaunchedEffect(selectedDay) {
        loadSelectedDay(selectedDay)
    }

    // ---- UI ----
    val scrollState = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {

        Text("Program Düzenle (Admin)", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text("Plan: ${plan.name}")
        Spacer(Modifier.height(12.dp))

        // ✅ KAPAK FOTOĞRAF BLOĞU
        Card(
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kapak Fotoğrafı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                val previewUrl = currentCoverUrl
                val previewUri = selectedCoverUri

                if (previewUri != null) {
                    AsyncImage(
                        model = previewUri,
                        contentDescription = "Seçilen Kapak",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text("Yeni kapak seçildi ✅", style = MaterialTheme.typography.bodySmall)
                } else if (previewUrl.isNotBlank()) {
                    AsyncImage(
                        model = previewUrl,
                        contentDescription = "Mevcut Kapak",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Kapak fotoğrafı yok.", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickCoverLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !uploadingCover
                    ) {
                        Text(if (currentCoverUrl.isBlank()) "Kapak Seç" else "Değiştir")
                    }

                    if (currentCoverUrl.isNotBlank() || selectedCoverUri != null) {
                        OutlinedButton(
                            onClick = { removeCover() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !uploadingCover
                        ) {
                            Text("Kaldır")
                        }
                    }
                }

                if (selectedCoverUri != null) {
                    Button(
                        onClick = { uploadCover(selectedCoverUri!!) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !uploadingCover
                    ) {
                        Text(if (uploadingCover) "Yükleniyor..." else "Kaydet (Kapak)")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Gün sayısı
        OutlinedTextField(
            value = daysCountText,
            onValueChange = { daysCountText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Haftada kaç gün? (örn: 3, 5, 7)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        // Gün seçimi
        Text("Gün Seçimi", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        val dcInt = currentDaysCount.toInt()

        if (dcInt <= 0) {
            Text("Günlere geçmeden önce gün sayısını girip 'Genel Kaydet' ile kaydet.")
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { if (selectedDay > 1) selectedDay -= 1 },
                    enabled = selectedDay > 1
                ) { Text("<") }

                Text("Gün $selectedDay / $dcInt", style = MaterialTheme.typography.titleMedium)

                Button(
                    onClick = { if (selectedDay < dcInt) selectedDay += 1 },
                    enabled = selectedDay < dcInt
                ) { Text(">") }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Gün başlığı
        OutlinedTextField(
            value = dayTitle,
            onValueChange = { dayTitle = it },
            label = { Text("Gün Başlığı (örn: Gün 1 - Göğüs/Triceps)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        // Egzersiz formu
        Text("Egzersiz Ekle", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = exName,
            onValueChange = { exName = it },
            label = { Text("Egzersiz adı (örn: Bench Press)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = exSets,
                onValueChange = { exSets = it },
                label = { Text("Set") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = exReps,
                onValueChange = { exReps = it },
                label = { Text("Tekrar") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = exNote,
            onValueChange = { exNote = it },
            label = { Text("Not (opsiyonel)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (exName.isBlank() || exSets.isBlank() || exReps.isBlank()) {
                    Toast.makeText(ctx, "Ad/Set/Tekrar boş olamaz", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                exercises = exercises + ExerciseItem(
                    name = exName.trim(),
                    sets = exSets.trim(),
                    reps = exReps.trim(),
                    note = exNote.trim()
                )

                exName = ""
                exSets = ""
                exReps = ""
                exNote = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Egzersizi Listeye Ekle") }

        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        // Egzersiz listesi
        Text("Bu Günün Egzersizleri", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (exercises.isEmpty()) {
            Text("Bu güne henüz egzersiz eklenmemiş.")
        } else {
            exercises.forEachIndexed { idx, ex ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${idx + 1}) ${ex.name}", style = MaterialTheme.typography.titleMedium)
                        Text("${ex.sets} set | ${ex.reps} tekrar")
                        if (ex.note.isNotBlank()) Text("Not: ${ex.note}")

                        Spacer(Modifier.height(6.dp))

                        TextButton(onClick = {
                            val newList = exercises.toMutableList()
                            newList.removeAt(idx)
                            exercises = newList
                        }) { Text("Sil") }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { saveAll() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Genel Kaydet")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Geri Çık")
        }
    }
}
