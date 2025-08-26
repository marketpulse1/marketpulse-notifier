package com.xaotec.marketpulse.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Symbol(val ticker: String, val type: String, val coingeckoId: String = "")

class Prefs(ctx: Context) {
  private val sp = ctx.getSharedPreferences("marketpulse_prefs", Context.MODE_PRIVATE)

  fun getSymbols(): List<Symbol> {
    val raw = sp.getString("symbols", defaultSymbols())
    val arr = JSONArray(raw)
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(Symbol(o.getString("ticker"), o.getString("type"), o.optString("coingeckoId","")))
      }
    }
  }

  private fun defaultSymbols(): String {
    val arr = JSONArray()
    arr.put(JSONObject(mapOf("ticker" to "NVDA","type" to "STOCK")))
    arr.put(JSONObject(mapOf("ticker" to "BBAI","type" to "STOCK")))
    arr.put(JSONObject(mapOf("ticker" to "DASH","type" to "STOCK")))
    arr.put(JSONObject(mapOf("ticker" to "BTC","type" to "CRYPTO","coingeckoId" to "bitcoin")))
    arr.put(JSONObject(mapOf("ticker" to "SOL","type" to "CRYPTO","coingeckoId" to "solana")))
    arr.put(JSONObject(mapOf("ticker" to "ADA","type" to "CRYPTO","coingeckoId" to "cardano")))
    return arr.toString()
  }

  fun addSymbol(s: Symbol) {
    val arr = JSONArray(sp.getString("symbols", defaultSymbols()))
    var exists = false
    for (i in 0 until arr.length()) {
      if (arr.getJSONObject(i).getString("ticker").equals(s.ticker, true)) { exists = true; break }
    }
    if (!exists) {
      arr.put(JSONObject(mapOf("ticker" to s.ticker, "type" to s.type, "coingeckoId" to s.coingeckoId)))
      sp.edit().putString("symbols", arr.toString()).apply()
    }
  }

  fun removeSymbol(t: String) {
    val arr = JSONArray(sp.getString("symbols", defaultSymbols()))
    val out = JSONArray()
    for (i in 0 until arr.length()) {
      val o = arr.getJSONObject(i)
      if (!o.getString("ticker").equals(t, true)) out.put(o)
    }
    sp.edit().putString("symbols", out.toString()).apply()
  }
}
