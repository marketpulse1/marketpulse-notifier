package com.xaotec.marketpulse.net

import android.content.Context
import com.xaotec.marketpulse.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object ApiClients {
  private val client = OkHttpClient()

  suspend fun fetchCryptoPrices(ids: List<String>): Map<String, Double> = withContext(Dispatchers.IO) {
    if (ids.isEmpty()) return@withContext emptyMap()
    val joined = URLEncoder.encode(ids.joinToString(","), "UTF-8")
    val url = "https://api.coingecko.com/api/v3/simple/price?ids=$joined&vs_currencies=usd"
    client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
      if (!resp.isSuccessful) return@withContext emptyMap()
      val body = resp.body?.string() ?: return@withContext emptyMap()
      val json = JSONObject(body); val out = mutableMapOf<String,Double>()
      for (id in ids) if (json.has(id)) json.getJSONObject(id).optDouble("usd").let { if (!it.isNaN()) out[id]=it }
      out
    }
  }

  suspend fun fetchStockPrice(ctx: Context, symbol: String): Double? = withContext(Dispatchers.IO) {
    val key = try { ctx.getString(R.string.alpha_vantage_key) } catch (_: Exception) { "demo" }
    val url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=$symbol&apikey=$key"
    client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
      if (!resp.isSuccessful) return@withContext null
      val body = resp.body?.string() ?: return@withContext null
      val quote = JSONObject(body).optJSONObject("Global Quote") ?: return@withContext null
      quote.optString("05. price", "").toDoubleOrNull()
    }
  }
}
