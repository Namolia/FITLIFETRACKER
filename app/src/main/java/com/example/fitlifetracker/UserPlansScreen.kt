package com.example.fitlifetracker

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun UserPlansScreen(
    onBack: () -> Unit,
    onSelectPlan: (Plan) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var plans by remember { mutableStateOf(listOf<Plan>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("plans")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener

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
                }.filter { it.onSale }

                loading = false
            }
    }

    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 6.dp) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Geri Çık")
                }
            }
        }
    ) { padding ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Programlar", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Hedefine uygun programı seç ve hemen başla",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            if (loading) {
                Text("Yükleniyor...")
            } else if (plans.isEmpty()) {
                Text("Şu anda satışta program bulunmuyor.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(plans) { p ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                                .clickable { onSelectPlan(p) },
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {

                                // Kapak görseli + fiyat etiketi
                                if (p.imageUrl.isNotBlank()) {
                                    Box {
                                        AsyncImage(
                                            model = p.imageUrl,
                                            contentDescription = "Kapak",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(170.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                        )

                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            tonalElevation = 4.dp
                                        ) {
                                            Text(
                                                text = "${p.price} TL",
                                                modifier = Modifier.padding(
                                                    horizontal = 10.dp,
                                                    vertical = 6.dp
                                                ),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }

                                // Metinler
                                Text(p.name, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    p.desc,
                                    maxLines = 2,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(Modifier.height(12.dp))

                                // Alt satır
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Stok: ${p.stock}",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Button(onClick = { onSelectPlan(p) }) {
                                        Text("İncele")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
