package com.xaotec.marketpulse.work

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xaotec.marketpulse.R
import com.xaotec.marketpulse.data.Prefs
import com.xaotec.marketpulse.net.ApiClients

class PriceCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
  companion object { const val WORK_NAME = "marketpulse_worker"; const val CHANNEL_ID = "marketpulse_alerts" }

  override suspend fun doWork(): Result {
    val prefs = Prefs(applicationContext)
    val symbols = prefs.getSymbols()
    val cryptoIds = symbols.filter { it.type=="CRYPTO" && it.coingeckoId.isNotBlank() }.map { it.coingeckoId }
    val cryptoPrices = ApiClients.fetchCryptoPrices(cryptoIds)

    val sb = StringBuilder()
    for (s in symbols) {
      when (s.type) {
        "CRYPTO" -> cryptoPrices[s.coingeckoId]?.let { sb.append("${s.ticker}: $").append(String.format("%.4f", it)).append("\n") }
        "STOCK"  -> ApiClients.fetchStockPrice(applicationContext, s.ticker)?.let {
          sb.append("${s.ticker}: $").append(String.format("%.2f", it)).append("\n")
        }
      }
    }
    if (sb.isNotEmpty()) notify("Price Update", sb.toString().trim())
    return Result.success()
  }

  private fun notify(title: String, text: String) {
    val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val n = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_stat_name)
      .setContentTitle(title)
      .setContentText(text)
      .setStyle(NotificationCompat.BigTextStyle().bigText(text))
      .setAutoCancel(true)
      .build()
    nm.notify(System.currentTimeMillis().toInt(), n)
  }
}
