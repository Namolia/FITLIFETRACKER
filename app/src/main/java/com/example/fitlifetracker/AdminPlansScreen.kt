package com.example.fitlifetracker

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun AdminPlansScreen(
    onBack: () -> Unit,
    onEditProgram: (Plan) -> Unit
) {
    val ctx = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    // Form state
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }

    // Image picker state
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    // List state
    var plans by remember { mutableStateOf(listOf<Plan>()) }

    LaunchedEffect(Unit) {
        db.collection("plans").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener

            plans = snap.documents.map { d ->
                Plan(
                    id = d.id,
                    name = d.getString("name") ?: "",
                    desc = d.getString("desc") ?: "",
                    price = d.getLong("price") ?: 0L,
                    stock = d.getLong("stock") ?: 0L,
                    onSale = d.getBoolean("onSale") ?: true,
                    imageUrl = d.getString("imageUrl") ?: ""
                )
            }
        }
    }

    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Geri Çık")
                }
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Text("Plan Yönetimi", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Yeni plan ekleyebilir, satış durumunu değiştirebilir ve program detaylarını düzenleyebilirsin.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // FORM KARTI
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {

                        Text("Yeni Plan Ekle", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Plan Adı") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            label = { Text("Açıklama") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Fiyat") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(Modifier.width(10.dp))

                            OutlinedTextField(
                                value = stock,
                                onValueChange = { stock = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Stok") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { pickImageLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                if (selectedImageUri == null) "Kapak Fotoğrafı Seç"
                                else "Kapak Seçildi ✅"
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (name.isBlank() || desc.isBlank()) {
                                    Toast.makeText(ctx, "Ad ve açıklama boş olamaz", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                fun savePlan(imageUrl: String) {
                                    val planMap = hashMapOf(
                                        "name" to name.trim(),
                                        "desc" to desc.trim(),
                                        "price" to (price.toLongOrNull() ?: 0L),
                                        "stock" to (stock.toLongOrNull() ?: 0L),
                                        "onSale" to true,
                                        "daysCount" to 0L,
                                        "imageUrl" to imageUrl
                                    )

                                    db.collection("plans").add(planMap)
                                        .addOnSuccessListener {
                                            Toast.makeText(ctx, "Plan eklendi", Toast.LENGTH_SHORT).show()
                                            name = ""
                                            desc = ""
                                            price = ""
                                            stock = ""
                                            selectedImageUri = null
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(ctx, "Firestore hata: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                }

                                val uri = selectedImageUri
                                if (uri == null) {
                                    savePlan("")
                                } else {
                                    val ref = storage.reference.child("plan_covers/${System.currentTimeMillis()}.jpg")
                                    ref.putFile(uri)
                                        .addOnSuccessListener {
                                            ref.downloadUrl
                                                .addOnSuccessListener { downloadUri: Uri ->
                                                    savePlan(downloadUri.toString())
                                                }
                                                .addOnFailureListener { e: Exception ->
                                                    Toast.makeText(ctx, "URL alma hatası: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                        }
                                        .addOnFailureListener { e: Exception ->
                                            Toast.makeText(ctx, "Yükleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Plan Ekle")
                        }
                    }
                }
            }

            item {
                Text("Mevcut Planlar", style = MaterialTheme.typography.titleMedium)
            }

            items(plans, key = { it.id }) { p ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(5.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {

                        // ✅ Fotoğraf varsa göster
                        if (p.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = p.imageUrl,
                                contentDescription = "Plan Kapak",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(p.desc, style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(10.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fiyat: ${p.price}")
                            Text("Stok: ${p.stock}")
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Satışta")

                            Switch(
                                checked = p.onSale,
                                onCheckedChange = { checked ->
                                    db.collection("plans").document(p.id)
                                        .update("onSale", checked)
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { onEditProgram(p) },
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Program") }

                            TextButton(
                                onClick = { db.collection("plans").document(p.id).delete() }
                            ) { Text("Sil") }
                        }
                    }
                }
            }
        }
    }
}
