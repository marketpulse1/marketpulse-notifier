package com.xaotec.marketpulse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.xaotec.marketpulse.data.Prefs
import com.xaotec.marketpulse.data.Symbol
import com.xaotec.marketpulse.work.PriceCheckWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        PriceCheckWorker.CHANNEL_ID, "MarketPulse Alerts",
        NotificationManager.IMPORTANCE_DEFAULT
      )
      getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    val request = PeriodicWorkRequestBuilder<PriceCheckWorker>(2, TimeUnit.HOURS)
      .setConstraints(constraints).build()
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
      PriceCheckWorker.WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
    )

    setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { HomeScreen() } } }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
  val ctx = androidx.compose.ui.platform.LocalContext.current
  var symbols by remember { mutableStateOf(Prefs(ctx).getSymbols()) }
  var ticker by remember { mutableStateOf("") }
  var type by remember { mutableStateOf("STOCK") }
  var coingeckoId by remember { mutableStateOf("") }

  Scaffold(topBar = { TopAppBar(title = { Text("MarketPulse Notifier", fontWeight = FontWeight.Bold) }) }) { p ->
    Column(Modifier.padding(p).padding(16.dp)) {
      Text("Tracked Symbols", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(8.dp))
      LazyColumn(Modifier.weight(1f)) {
        items(symbols) { s ->
          Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${s.ticker} • ${s.type}", Modifier.weight(1f))
            if (s.type == "CRYPTO" && s.coingeckoId.isNotEmpty()) {
              AssistChip(onClick = {}, label = { Text("CG: ${s.coingeckoId}") })
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { Prefs(ctx).removeSymbol(s.ticker); symbols = Prefs(ctx).getSymbols() }) { Text("Remove") }
          }
        }
      }
      Divider(); Spacer(Modifier.height(8.dp))
      Text("Add Symbol", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
      OutlinedTextField(value = ticker, onValueChange = { ticker = it.uppercase() }, label = { Text("Ticker (e.g., NVDA, BTC)") })
      Spacer(Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        FilterChip(selected = type=="STOCK", onClick = { type = "STOCK" }, label = { Text("STOCK") })
        Spacer(Modifier.width(8.dp))
        FilterChip(selected = type=="CRYPTO", onClick = { type = "CRYPTO" }, label = { Text("CRYPTO") })
      }
      if (type == "CRYPTO") {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = coingeckoId, onValueChange = { coingeckoId = it.lowercase() }, label = { Text("CoinGecko ID (bitcoin, solana)") })
      }
      Spacer(Modifier.height(12.dp))
      Row {
        Button(onClick = {
          if (ticker.isNotBlank()) {
            Prefs(ctx).addSymbol(Symbol(ticker, type, coingeckoId))
            symbols = Prefs(ctx).getSymbols(); ticker = ""; coingeckoId = ""
          }
        }) { Text("Add") }
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
          val c = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
          val req = OneTimeWorkRequestBuilder<PriceCheckWorker>().setConstraints(c).build()
          WorkManager.getInstance(ctx).enqueue(req)
        }) { Text("Check Now") }
      }
      Spacer(Modifier.height(8.dp))
      Text("Stocks use Alpha Vantage (add key in strings.xml). Crypto uses CoinGecko. Alerts run every 2 hours.")
    }
  }
}
