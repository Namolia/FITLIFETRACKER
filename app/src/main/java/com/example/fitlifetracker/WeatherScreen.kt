package com.example.fitlifetracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun WeatherScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var temp by remember { mutableStateOf<Double?>(null) }
    var wind by remember { mutableStateOf<Double?>(null) }
    var err by remember { mutableStateOf<String?>(null) }

    fun load() {
        err = null
        temp = null
        wind = null
        scope.launch {
            try {
                // İstanbul (yaklaşık)
                val res = WeatherApi.service.getCurrentWeather(
                    lat = 41.0082,
                    lon = 28.9784
                )
                temp = res.current_weather?.temperature
                wind = res.current_weather?.windspeed
            } catch (e: Exception) {
                err = e.message
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Hava Durumu (API)", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                if (err != null) {
                    Text("Hata: $err")
                } else if (temp == null) {
                    Text("Yükleniyor...")
                } else {
                    Text("Şehir: İstanbul")
                    Spacer(Modifier.height(8.dp))
                    Text("Sıcaklık: $temp °C", style = MaterialTheme.typography.titleMedium)
                    Text("Rüzgar: $wind km/h")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { load() }, modifier = Modifier.fillMaxWidth()) {
            Text("Yenile")
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Geri")
        }
    }
}
