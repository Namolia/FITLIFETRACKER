package com.example.fitlifetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UserHomeScreen(
    email: String,
    onGoPlans: () -> Unit,
    onGoMotivation: () -> Unit,
    onGoWeather: () -> Unit,
    onGoCart: () -> Unit,      // ✅ YENİ
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // HEADER KARTI
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
                            Text("👤", style = MaterialTheme.typography.headlineSmall)
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text("Hoş geldin!", style = MaterialTheme.typography.titleLarge)
                        Text(email, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Bugün hedeflerine bir adım daha yaklaşalım 💪",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ACTION GRID
        Row(Modifier.fillMaxWidth()) {
            ActionCard(
                title = "Planlar",
                subtitle = "Programları incele",
                icon = "🏋️",
                onClick = onGoPlans,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            ActionCard(
                title = "Sepet",
                subtitle = "Seçtiklerini gör",
                icon = "🛒",
                onClick = onGoCart,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth()) {
            ActionCard(
                title = "Motivasyon",
                subtitle = "Günün sözü",
                icon = "🔥",
                onClick = onGoMotivation,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            ActionCard(
                title = "Hava Durumu",
                subtitle = "Bugün nasıl?",
                icon = "⛅",
                onClick = onGoWeather,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ALT BİLGİ KARTI
        Card(
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Mini İpucu", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Her gün 2L su hedefi koy. Küçük alışkanlıklar büyük değişim yaratır.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ✅ ÇIKIŞ - altta geniş buton daha şık durur
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
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = modifier.height(120.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(icon, style = MaterialTheme.typography.headlineMedium)

            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
