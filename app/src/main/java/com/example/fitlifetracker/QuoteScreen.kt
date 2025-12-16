package com.example.fitlifetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class TrQuote(val text: String, val author: String = "")

private val TR_QUOTES = listOf(
    TrQuote("Bugün attığın küçük adım, yarınki büyük değişimin başlangıcıdır.", "Anonim"),
    TrQuote("İstikrar, yetenekten daha güçlüdür.", "Anonim"),
    TrQuote("Yavaş git ama asla durma.", "Konfüçyüs"),
    TrQuote("Kendine inan. Başlamak, bitirmenin yarısıdır.", "Anonim"),
    TrQuote("Başarı, her gün tekrar edilen küçük çabaların toplamıdır.", "Robert Collier"),
    TrQuote("Disiplin, hedeflerle arandaki köprüdür.", "Jim Rohn"),
    TrQuote("Zor günler güçlü insanları yaratır.", "Anonim"),
    TrQuote("Pes etme. Bugünün yorgunluğu yarının gücü olur.", "Anonim"),
    TrQuote("Mükemmel olmanı bekleme; devam etmeni bekle.", "Anonim"),
    TrQuote("İlk adım atılmadan yol bitmez.", "Anonim"),
    TrQuote("Bugün kendin için bir şey yap.", "Anonim"),
    TrQuote("Kazanmak istiyorsan, önce vazgeçmemeyi öğren.", "Anonim"),
    TrQuote("Hedefin netse, yol kendini gösterir.", "Anonim"),
    TrQuote("Bahane ararsan bulursun; çözüm ararsan da bulursun.", "Anonim"),
    TrQuote("Dününle yarış, başkasıyla değil.", "Anonim"),
    TrQuote("Başladığın işi bitir. Bitirmek özgüven kazandırır.", "Anonim"),
    TrQuote("Motivasyon gelir geçer; disiplin kalır.", "Anonim"),
    TrQuote("Küçük ilerleme bile ilerlemedir.", "Anonim"),
    TrQuote("Bugün yap. Yarın sadece bir bahanedir.", "Anonim"),
    TrQuote("Zihin inanırsa, beden takip eder.", "Anonim")
)

@Composable
fun QuoteScreen(onBack: () -> Unit) {
    var quote by remember { mutableStateOf("Hazır mısın? 💪") }
    var author by remember { mutableStateOf("") }

    fun load() {
        val q = TR_QUOTES[Random.nextInt(TR_QUOTES.size)]
        quote = q.text
        author = q.author
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Motivasyon Sözü", style = MaterialTheme.typography.headlineMedium)

        Card(
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(quote, style = MaterialTheme.typography.titleMedium)
                if (author.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("— $author", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Button(
            onClick = { load() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Yeni Söz Getir")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Geri")
        }
    }
}
