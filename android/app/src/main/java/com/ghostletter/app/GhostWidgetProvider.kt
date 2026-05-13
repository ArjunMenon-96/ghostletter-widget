package com.ghostletter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class GhostWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.ghostletter.app.REFRESH_WIDGET"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, GhostWidgetProvider::class.java))
            if (ids.isNotEmpty()) onUpdate(context, mgr, ids)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) showLoading(context, appWidgetManager, id)
        Thread {
            val prices = tryFetchPrices()
            try {
                val stories = fetchStories()
                for (id in appWidgetIds) updateWidget(context, appWidgetManager, id, stories, prices)
            } catch (e: Exception) {
                for (id in appWidgetIds) showError(context, appWidgetManager, id)
            }
        }.start()
    }

    // ── Loading / error states ──────────────────────────────────────────────

    private fun showLoading(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(ctx.packageName, R.layout.ghost_widget)
        views.setTextViewText(R.id.gl_title_0, "Loading headlines...")
        views.setTextViewText(R.id.gl_title_1, "")
        views.setTextViewText(R.id.gl_title_2, "")
        views.setTextViewText(R.id.gl_title_3, "")
        views.setTextViewText(R.id.gl_title_4, "")
        mgr.updateAppWidget(id, views)
    }

    private fun showError(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(ctx.packageName, R.layout.ghost_widget)
        views.setTextViewText(R.id.gl_title_0, "Tap to refresh")
        views.setTextViewText(R.id.gl_num_0, "!")
        val intent = Intent(ctx, GhostWidgetProvider::class.java).apply { action = ACTION_REFRESH }
        val pi = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.gl_row_0, pi)
        mgr.updateAppWidget(id, views)
    }

    // ── Price fetching ──────────────────────────────────────────────────────

    data class CoinPrice(val symbol: String, val price: Double, val change24h: Double)

    private fun tryFetchPrices(): List<CoinPrice> = try { fetchPrices() } catch (_: Exception) { emptyList() }

    private fun fetchPrices(): List<CoinPrice> {
        val url = URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=usd&include_24hr_change=true")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        conn.setRequestProperty("User-Agent", "GhostLetter-Widget/1.0")
        return try {
            val obj = JSONObject(conn.inputStream.bufferedReader().readText())
            listOf(
                CoinPrice("BTC", obj.getJSONObject("bitcoin").getDouble("usd"), obj.getJSONObject("bitcoin").getDouble("usd_24h_change")),
                CoinPrice("ETH", obj.getJSONObject("ethereum").getDouble("usd"), obj.getJSONObject("ethereum").getDouble("usd_24h_change")),
            )
        } finally { conn.disconnect() }
    }

    private fun formatPrice(price: Double): String =
        when {
            price >= 1_000 -> "$${DecimalFormat("#,##0").format(price.toLong())}"
            price >= 1     -> "$${DecimalFormat("0.00").format(price)}"
            else           -> "$${DecimalFormat("0.0000").format(price)}"
        }

    private fun buildPriceSpan(prices: List<CoinPrice>): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        prices.forEachIndexed { i, coin ->
            if (i > 0) sb.append("   ")
            sb.append("${coin.symbol} ${formatPrice(coin.price)} ")
            val arrow = if (coin.change24h >= 0) "▲" else "▼"
            val pct   = String.format("%.1f", Math.abs(coin.change24h)) + "%"
            val start = sb.length
            sb.append("$arrow$pct")
            val color = if (coin.change24h >= 0) Color.parseColor("#52c41a") else Color.parseColor("#ff4d4f")
            sb.setSpan(ForegroundColorSpan(color), start, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }

    // ── News fetching ───────────────────────────────────────────────────────

    private fun fetchStories(): List<Story> {
        val url = URL("https://ghostletter.online/api/news")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "GhostLetter-Widget/1.0")
        return try {
            parseStories(conn.inputStream.bufferedReader().readText())
        } finally { conn.disconnect() }
    }

    private fun parseStories(json: String): List<Story> {
        val stories = mutableListOf<Story>()
        val arr: JSONArray = try { JSONArray(json) } catch (_: Exception) {
            JSONObject(json).optJSONArray("items") ?: JSONArray()
        }
        for (i in 0 until minOf(arr.length(), 5)) {
            val obj = arr.getJSONObject(i)
            stories.add(Story(
                title    = obj.optString("title", "").trim(),
                link     = obj.optString("link", obj.optString("url", "")),
                source   = obj.optString("source", obj.optString("feedTitle", "")).uppercase(),
                category = obj.optString("category", "").uppercase(),
                pubDate  = obj.optString("pubDate", obj.optString("publishedAt", "")),
            ))
        }
        return stories
    }

    // ── Widget rendering ────────────────────────────────────────────────────

    private fun updateWidget(ctx: Context, mgr: AppWidgetManager, id: Int, stories: List<Story>, prices: List<CoinPrice>) {
        val views = RemoteViews(ctx.packageName, R.layout.ghost_widget)

        // Price in header
        if (prices.isNotEmpty()) {
            views.setTextViewText(R.id.gl_price, buildPriceSpan(prices))
        } else {
            views.setTextViewText(R.id.gl_price, "● LIVE")
        }

        // Refresh button
        val refreshIntent = Intent(ctx, GhostWidgetProvider::class.java).apply { action = ACTION_REFRESH }
        val refreshPi = PendingIntent.getBroadcast(ctx, 200, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.gl_refresh, refreshPi)

        // Logo taps → open app
        ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let { launchApp ->
            val pi = PendingIntent.getActivity(ctx, 0, launchApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.gl_header, pi)
        }

        // Story rows
        val rowIds   = intArrayOf(R.id.gl_row_0, R.id.gl_row_1, R.id.gl_row_2, R.id.gl_row_3, R.id.gl_row_4)
        val numIds   = intArrayOf(R.id.gl_num_0, R.id.gl_num_1, R.id.gl_num_2, R.id.gl_num_3, R.id.gl_num_4)
        val srcIds   = intArrayOf(R.id.gl_src_0, R.id.gl_src_1, R.id.gl_src_2, R.id.gl_src_3, R.id.gl_src_4)
        val timeIds  = intArrayOf(R.id.gl_time_0, R.id.gl_time_1, R.id.gl_time_2, R.id.gl_time_3, R.id.gl_time_4)
        val titleIds = intArrayOf(R.id.gl_title_0, R.id.gl_title_1, R.id.gl_title_2, R.id.gl_title_3, R.id.gl_title_4)

        for (i in 0 until 5) {
            if (i < stories.size) {
                val s = stories[i]
                views.setTextViewText(numIds[i], "${i + 1}")
                views.setTextViewText(srcIds[i],
                    if (s.category.isNotEmpty()) "${s.source.take(10)}  ·  ${s.category.take(8)}"
                    else s.source.take(14))
                views.setTextViewText(timeIds[i], timeAgo(s.pubDate))
                views.setTextViewText(titleIds[i], s.title)
                if (s.link.isNotEmpty()) {
                    val pi = PendingIntent.getActivity(ctx, 100 + i,
                        Intent(Intent.ACTION_VIEW, Uri.parse(s.link)),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(rowIds[i], pi)
                }
            } else {
                views.setTextViewText(numIds[i], "")
                views.setTextViewText(srcIds[i], "")
                views.setTextViewText(timeIds[i], "")
                views.setTextViewText(titleIds[i], "")
            }
        }

        views.setTextViewText(R.id.gl_footer, "ghostletter.online · updated ${formatNow()}")
        mgr.updateAppWidget(id, views)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun timeAgo(pubDate: String): String {
        if (pubDate.isEmpty()) return ""
        return try {
            val formats = listOf(
                "EEE, dd MMM yyyy HH:mm:ss z",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ssXXX"
            )
            var date: Date? = null
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.US).also { it.timeZone = TimeZone.getTimeZone("UTC") }
                    date = sdf.parse(pubDate)
                    if (date != null) break
                } catch (_: Exception) {}
            }
            val d = date ?: return ""
            val mins = ((System.currentTimeMillis() - d.time) / 60_000).toInt()
            when {
                mins < 1    -> "now"
                mins < 60   -> "${mins}m"
                mins < 1440 -> "${mins / 60}h"
                else        -> "${mins / 1440}d"
            }
        } catch (_: Exception) { "" }
    }

    private fun formatNow(): String =
        SimpleDateFormat("HH:mm", Locale.US).also { it.timeZone = TimeZone.getTimeZone("UTC") }.format(Date()) + " UTC"

    data class Story(val title: String, val link: String, val source: String, val category: String, val pubDate: String)
}
