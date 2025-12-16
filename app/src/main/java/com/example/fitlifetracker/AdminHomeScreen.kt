package com.example.fitlifetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun AdminHomeScreen(
    email: String,
    onGoPlans: () -> Unit,
    onGoOrders: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // HEADER
        Card(
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        tonalElevation = 6.dp,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🛡️", style = MaterialTheme.typography.headlineSmall)
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text("Admin Panel", style = MaterialTheme.typography.titleLarge)
                        Text(email, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    "Planları yönet, siparişleri kontrol et ve sistemi güncel tut.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ACTION CARDS
        ActionCardWide(
            title = "Plan Yönetimi",
            subtitle = "Programları ekle/düzenle",
            icon = "📦",
            onClick = onGoPlans
        )

        Spacer(Modifier.height(12.dp))

        ActionCardWide(
            title = "Siparişler",
            subtitle = "Gelen siparişleri görüntüle",
            icon = "🧾",
            onClick = onGoOrders
        )

        Spacer(Modifier.height(18.dp))

        // LOGOUT (ayrı ve net)
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Çıkış Yap")
        }
    }
}

@Composable
private fun ActionCardWide(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }

            Text("›", style = MaterialTheme.typography.headlineMedium)
        }
    }
}


