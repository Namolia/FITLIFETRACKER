package com.example.fitlifetracker

import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var cartIds by remember { mutableStateOf(listOf<String>()) }
    var plans by remember { mutableStateOf(listOf<Plan>()) }
    var total by remember { mutableStateOf(0L) }

    var loading by remember { mutableStateOf(true) }
    var ordering by remember { mutableStateOf(false) }

    // ✅ Siparişe engel durumlar (UI uyarısı)
    val outOfStockPlans = remember(plans) { plans.filter { it.stock <= 0L } }
    val notOnSalePlans = remember(plans) { plans.filter { !it.onSale } }
    val canOrder = plans.isNotEmpty() && outOfStockPlans.isEmpty() && notOnSalePlans.isEmpty()

    // Sepet id listesini dinle
    LaunchedEffect(Unit) {
        if (uid.isBlank()) {
            loading = false
            return@LaunchedEffect
        }
        db.collection("carts").document(uid).addSnapshotListener { doc, _ ->
            if (doc == null) return@addSnapshotListener
            val items = doc.get("items") as? List<*>
            cartIds = items?.mapNotNull { it as? String } ?: emptyList()
        }
    }

    // Sepetteki id'lerden planları çek
    LaunchedEffect(cartIds) {
        loading = true

        if (cartIds.isEmpty()) {
            plans = emptyList()
            total = 0L
            loading = false
            return@LaunchedEffect
        }

        // whereIn max 10; sende şimdilik yeter demiştin
        db.collection("plans")
            .whereIn(FieldPath.documentId(), cartIds)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.map { d ->
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
                plans = list
                total = list.sumOf { it.price }
                loading = false
            }
            .addOnFailureListener { e ->
                loading = false
                Toast.makeText(ctx, "Sepet çekme hatası: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sepet", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Geri") }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 10.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (plans.isNotEmpty()) {
                        SummaryCard(total = total, itemCount = plans.size)

                        // ✅ Uyarılar
                        if (notOnSalePlans.isNotEmpty()) {
                            AssistMessage(
                                text = "Bazı planlar satışta değil. Sepetten çıkarıp tekrar dene.",
                                isError = true
                            )
                        }
                        if (outOfStockPlans.isNotEmpty()) {
                            AssistMessage(
                                text = "Bazı planların stoğu bitmiş. Sepetten çıkarıp tekrar dene.",
                                isError = true
                            )
                        }

                        Button(
                            onClick = {
                                if (uid.isBlank() || ordering) return@Button
                                if (!canOrder) {
                                    Toast.makeText(ctx, "Sipariş verilemedi: stok/satış durumu kontrol et", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                ordering = true

                                val planIdsToBuy = cartIds.toList() // snapshot
                                val orderId = UUID.randomUUID().toString()

                                db.runTransaction { tx: Transaction ->
                                    // 1) her planın stok/satış kontrolü + stok düşür
                                    for (pid in planIdsToBuy) {
                                        val planRef = db.collection("plans").document(pid)
                                        val planSnap = tx.get(planRef)

                                        val onSale = planSnap.getBoolean("onSale") ?: true
                                        val stock = planSnap.getLong("stock") ?: 0L

                                        if (!onSale) {
                                            throw IllegalStateException("Satışta olmayan plan var.")
                                        }
                                        if (stock <= 0L) {
                                            throw IllegalStateException("Stok yetersiz.")
                                        }

                                        tx.update(planRef, "stock", stock - 1L)
                                    }

                                    // 2) order yaz (transaction içinde)
                                    val orderRef = db.collection("orders").document(orderId)
                                    val orderMap = hashMapOf(
                                        "uid" to uid,
                                        "items" to planIdsToBuy,
                                        "total" to total,
                                        "status" to "pending",
                                        "createdAt" to FieldValue.serverTimestamp()
                                    )
                                    tx.set(orderRef, orderMap)

                                    null
                                }.addOnSuccessListener {
                                    // 3) transaction başarılıysa sepeti temizle
                                    db.collection("carts").document(uid)
                                        .set(mapOf("uid" to uid, "items" to emptyList<String>()))
                                        .addOnSuccessListener {
                                            ordering = false
                                            Toast.makeText(ctx, "Sipariş verildi (pending) ✅", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            ordering = false
                                            Toast.makeText(ctx, "Sipariş oluşturuldu ama sepet temizlenemedi: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                }.addOnFailureListener { e ->
                                    ordering = false
                                    val msg = e.message ?: "Bilinmeyen hata"
                                    Toast.makeText(ctx, "Sipariş verilemedi: $msg", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !ordering && canOrder
                        ) {
                            Text(if (ordering) "Sipariş Veriliyor..." else "Sipariş Ver")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Alışverişe Devam Et")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (plans.isEmpty()) {
                EmptyCartCard()
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(plans, key = { it.id }) { p ->
                    CartItemCard(
                        plan = p,
                        onRemove = {
                            if (uid.isBlank()) return@CartItemCard
                            db.collection("carts").document(uid)
                                .update("items", FieldValue.arrayRemove(p.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(
    plan: Plan,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (plan.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = plan.imageUrl,
                    contentDescription = "Plan Kapak",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(plan.desc, style = MaterialTheme.typography.bodyMedium, maxLines = 2)

                    Spacer(Modifier.height(6.dp))

                    // ✅ küçük durum yazısı
                    val statusText =
                        if (!plan.onSale) "Satış kapalı"
                        else if (plan.stock <= 0L) "Stok yok"
                        else "Stok: ${plan.stock}"

                    Text(statusText, style = MaterialTheme.typography.bodySmall)
                }

                TextButton(onClick = onRemove) { Text("Çıkar") }
            }

            Divider()

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fiyat", style = MaterialTheme.typography.bodySmall)
                Text("${plan.price} TL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryCard(total: Long, itemCount: Int) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Toplam", style = MaterialTheme.typography.bodySmall)
                Text("$total TL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "$itemCount ürün",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AssistMessage(text: String, isError: Boolean) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun EmptyCartCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Sepet boş", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Plan sayfasından bir plan ekleyerek sepetini doldurabilirsin.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
