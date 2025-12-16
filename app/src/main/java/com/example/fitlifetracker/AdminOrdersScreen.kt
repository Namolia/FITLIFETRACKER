package com.example.fitlifetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

data class AdminOrderUi(
    val id: String = "",
    val uid: String = "",
    val total: Long = 0L,
    val status: String = "pending",
    val itemCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var orders by remember { mutableStateOf(emptyList<AdminOrderUi>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("orders")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener

                orders = snap.documents.map { d ->
                    val items = d.get("items") as? List<*>
                    AdminOrderUi(
                        id = d.id,
                        uid = d.getString("uid") ?: "",
                        total = d.getLong("total") ?: 0L,
                        status = d.getString("status") ?: "pending",
                        itemCount = items?.size ?: 0
                    )
                }.sortedByDescending { it.id }

                loading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Siparişler", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(text = "Geri") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (loading) {
                Text("Yükleniyor...")
                return@Column
            }

            if (orders.isEmpty()) {
                Text("Henüz sipariş yok.")
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(orders, key = { it.id }) { o ->
                    OrderCard(
                        o = o,
                        onApprove = {
                            db.collection("orders").document(o.id)
                                .update("status", "approved")
                        },
                        onDelete = {
                            db.collection("orders").document(o.id).delete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    o: AdminOrderUi,
    onApprove: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Üst satır: Sipariş ID + durum etiketi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sipariş • ${shortId(o.id)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "User: ${shortUid(o.uid)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                StatusPill(o.status)
            }

            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))

            // Orta: toplam + ürün sayısı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Toplam", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${o.total} TL",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Ürün Sayısı", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${o.itemCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Alt: aksiyonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val approved = o.status.lowercase() == "approved"

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !approved
                ) {
                    Text(if (approved) "Onaylandı" else "Onayla")
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Sil")
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val s = status.lowercase()
    val text = when (s) {
        "approved" -> "Onaylandı"
        "pending" -> "Beklemede"
        "cancelled" -> "İptal"
        else -> status
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun shortId(id: String): String {
    if (id.length <= 8) return id
    return id.take(6) + "..." + id.takeLast(2)
}

private fun shortUid(uid: String): String {
    if (uid.isBlank()) return "-"
    if (uid.length <= 10) return uid
    return uid.take(6) + "..." + uid.takeLast(2)
}
