package com.example.fitlifetracker

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPlanDetailScreen(
    plan: Plan,
    onGoCart: () -> Unit,
    onGoProgram: () -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var isInCart by remember { mutableStateOf(false) }
    var loadingCart by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }

    // Sepette var mı kontrol et
    LaunchedEffect(plan.id) {
        if (uid.isBlank()) {
            loadingCart = false
            return@LaunchedEffect
        }
        db.collection("carts").document(uid).get()
            .addOnSuccessListener { doc ->
                val items = doc.get("items") as? List<*>
                isInCart = items?.contains(plan.id) == true
                loadingCart = false
            }
            .addOnFailureListener {
                loadingCart = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan Detayı", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Geri") }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Kapak fotoğrafı (varsa)
            if (plan.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = plan.imageUrl,
                    contentDescription = "Plan Kapak",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Başlık + rozetler
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Satışta / Stok rozetleri
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = if (plan.onSale) "Satışta" else "Satış Kapalı",
                            container = if (plan.onSale)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )

                        StatusPill(
                            text = if (plan.stock > 0) "Stok: ${plan.stock}" else "Stok Yok",
                            container = if (plan.stock > 0)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    }

                    Text(
                        text = plan.desc,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Fiyat kartı
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Fiyat", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${plan.price} TL",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Sepet durumu
                    if (loadingCart) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        StatusPill(
                            text = if (isInCart) "Sepette" else "Sepette Değil",
                            container = if (isInCart)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // Aksiyonlar
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    val canBuy = plan.onSale && plan.stock > 0

                    Button(
                        onClick = {
                            if (busy) return@Button
                            if (uid.isBlank()) {
                                Toast.makeText(ctx, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!canBuy) {
                                Toast.makeText(ctx, "Bu plan şu an satın alınamaz", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            busy = true
                            val cartRef = db.collection("carts").document(uid)

                            if (!isInCart) {
                                cartRef.set(mapOf("uid" to uid), SetOptions.merge())
                                cartRef.update("items", FieldValue.arrayUnion(plan.id))
                                    .addOnSuccessListener {
                                        isInCart = true
                                        busy = false
                                        Toast.makeText(ctx, "Sepete eklendi", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        busy = false
                                        Toast.makeText(ctx, "Hata: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                cartRef.update("items", FieldValue.arrayRemove(plan.id))
                                    .addOnSuccessListener {
                                        isInCart = false
                                        busy = false
                                        Toast.makeText(ctx, "Sepetten çıkarıldı", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        busy = false
                                        Toast.makeText(ctx, "Hata: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !loadingCart && !busy
                    ) {
                        Text(if (isInCart) "Sepetten Çıkar" else "Sepete Ekle")
                    }

                    OutlinedButton(
                        onClick = onGoCart,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Sepete Git")
                    }

                    OutlinedButton(
                        onClick = onGoProgram,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Programı Gör")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    container: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = container,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
