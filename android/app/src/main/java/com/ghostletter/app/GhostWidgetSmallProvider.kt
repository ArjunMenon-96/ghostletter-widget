package com.ghostletter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
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
import java.util.concurrent.atomic.AtomicBoolean

class GhostWidgetSmallProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.ghostletter.app.REFRESH_WIDGET_SMALL"
        private val isUpdating = AtomicBoolean(false)

        val ROW_IDS   = intArrayOf(R.id.gs_row_0, R.id.gs_row_1, R.id.gs_row_2)
        val NUM_IDS   = intArrayOf(R.id.gs_num_0, R.id.gs_num_1, R.id.gs_num_2)
        val TITLE_IDS = intArrayOf(R.id.gs_title_0, R.id.gs_title_1, R.id.gs_title_2)
        val TIME_IDS  = intArrayOf(R.id.gs_time_0, R.id.gs_time_1, R.id.gs_time_2)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, GhostWidgetSmallProvider::class.java))
            if (ids.isNotEmpty()) onUpdate(context, mgr, ids)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (!isUpdating.compareAndSet(false, true)) return

        for (id in appWidgetIds) showLoading(context, appWidgetManager, id)

        Thread {
            try {
                if (!isNetworkAvailable(context)) {
                    for (id in appWidgetIds) showError(context, appWidgetManager, id, offline = true)
                    return@Thread
                }
                val prices  = tryFetchPrices()
                val stories = fetchStories()
                for (id in appWidgetIds) updateWidget(context, appWidgetManager, id, stories, prices)
            } catch (e: Exception) {
                for (id in appWidgetIds) showError(context, appWidgetManager, id, offline = false)
            } finally {
                isUpdating.set(false)
            }
        }.start()
    }

    // ── Network check ───────────────────────────────────────────────────────

    private fun isNetworkAvailable(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    // ── Loading state ───────────────────────────────────────────────────────

    private fun showLoading(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(ctx.packageName, R.layout.ghost_widget_small)
        for (i in 0 until 3) {
            views.setTextViewText(NUM_IDS[i],   "")
            views.setTextViewText(TITLE_IDS[i], "")
            views.setTextViewText(TIME_IDS[i],  "")
        }
        views.setTextViewText(NUM_IDS[0],   "·")
        views.setTextViewText(TITLE_IDS[0], "Loading…")
        views.setTextViewText(R.id.gs_price, "")
        mgr.updateAppWidget(id, views)
    }

    // ── Error state ─────────────────────────────────────────────────────────

    private fun showError(ctx: Context, mgr: AppWidgetManager, id: Int, offline: Boolean) {
        val views = RemoteViews(ctx.packageName, R.layout.ghost_widget_small)

        // Clear all rows
        for (i in 0 until 3) {
            views.setTextViewText(NUM_IDS[i],   "")
            views.setTextViewText(TITLE_IDS[i], "")
            views.setTextViewText(TIME_IDS[i],  "")
        }

        // Row 0: error message
        views.setTextViewText(NUM_IDS[0],   "!")
        views.setTextViewText(TITLE_IDS[0], if (offline) "No internet · tap ↻" else "Feed unavailable · tap ↻")
        views.setTextViewText(R.id.gs_price, if (offline) "● OFFLINE" else "● ERROR")

        // Refresh PendingIntent on both the button and row 0
        val refreshIntent = Intent(ctx, GhostWidgetSmallProvider::class.java).apply { action = ACTION_REFRESH }
        val refreshPi = PendingIntent.getBroadcast(
            ctx, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.gs_refresh, refreshPi)
        views.setOnClickPendingIntent(ROW_IDS[0], refreshPi)

        mgr.updateAppWidget(id, views)
    }

    // ── Prices ──────────────────────────────────────────────────────────────

    data class CoinPrice(val symbol: String, val price: Double, val change24h: Double)

    private fun tryFetchPrices(): List<CoinPrice> = try { fetchPrices() } catch (_: Exception) { emptyList() }

    private fun fetchPrices(): List<CoinPrice> {
        val url  = URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=usd&include_24hr_change=true")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout    = 8_000
        conn.setRequestProperty("User-Agent", "GhostLetter-Widget/1.0")
        return try {
            val obj = JSONObject(conn.inputStream.bufferedReader().readText())
            listOf(
                CoinPrice("BTC", obj.getJSONObject("bitcoin").getDouble("usd"),  obj.getJSONObject("bitcoin").getDouble("usd_24h_change")),
                CoinPrice("ETH", obj.getJSONObject("ethereum").getDouble("usd"), obj.getJSONObject("ethereum").getDouble("usd_24h_change")),
            )
        } finally { conn.disconnect() }
    }

    private fun formatPrice(price: Double): String = when {
        price >= 1_000 -> "$${DecimalFormat("#,##0").format(price.toLong())}"
        price >= 1     -> "$${DecimalFormat("0.00").format(price)}"
        else           -> "$${DecimalFormat("0.0000").format(price)}"
    }

    private fun buildPriceSpan(prices: List<CoinPrice>): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        prices.forEachIndexed { i, coin ->
            if (i > 0) sb.append("  ")
            sb.append("${coin.symbol} ${formatPrice(coin.price)} ")
            val arrow = if (coin.change24h >= 0) "▲" else "▼"
            val pct   = String.format("%.1f%%", Math.abs(coin.change24h))
            val start = sb.length
            sb.append("$arrow$pct")
            val color = if (coin.change24h >= 0) Color.parseColor("#52c41a") else Color.parseColor("#ff4d4f")
            sb.setSpan(ForegroundColorSpan(color), start, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }

    // ── News ─────────────────────────────────────────────────────────────────

    private fun fetchStories(): List<GhostWidgetProvider.Story> {
        val url  = URL("https://ghostletter.online/api/news")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout    = 10_000
        conn.setRequestProperty("User-Agent", "GhostLetter-Widget/1.0")
        return try {
            val json = conn.inputStream.bufferedReader().readText()
            val arr: JSONArray = try { JSONArray(json) } catch (_: Exception) {
                JSONObject(json).optJSONArray("items") ?: JSONArray()
            }
            (0 until minOf(arr.length(), 3)).map { i ->
                val obj = arr.getJSONObject(i)
                GhostWidgetProvider.Story(
                    title    = obj.optString("title", "").trim(),
                    link     = obj.optString("link", obj.optString("url", "")),
                    source   = obj.optString("source", "").uppercase(),
                    category = obj.optString("category", "").uppercase(),
                    pubDate  = obj.optString("pubDate", obj.optString("publishedAt", "")),
                )
            }
        } finally { conn.disconnect() }
    }

    // ── Render ───────────────────────────────────────────────────────────────

    private fun updateWidget(ctx: Context, mgr: AppWidgetManager, id: Int, stories: List<GhostWidgetProvider.Story>, prices: List<CoinPrice>) {
        val views = RemoteViews(ctx.packageName, R.layout.ghost_widget_small)

        // Prices
        if (prices.isNotEmpty()) {
            views.setTextViewText(R.id.gs_price, buildPriceSpan(prices))
        } else {
            views.setTextViewText(R.id.gs_price, "● LIVE")
        }

        // Refresh button
        val refreshIntent = Intent(ctx, GhostWidgetSmallProvider::class.java).apply { action = ACTION_REFRESH }
        val refreshPi = PendingIntent.getBroadcast(ctx, 300, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.gs_refresh, refreshPi)

        // Header tap → open website
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ghostletter.online"))
        val webPi = PendingIntent.getActivity(ctx, 1, webIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.gs_header, webPi)

        for (i in 0 until 3) {
            if (i < stories.size) {
                val s = stories[i]
                views.setTextViewText(NUM_IDS[i],   "${i + 1}")
                views.setTextViewText(TITLE_IDS[i], s.title)
                views.setTextViewText(TIME_IDS[i],  timeAgo(s.pubDate))
                if (s.link.isNotEmpty()) {
                    val pi = PendingIntent.getActivity(
                        ctx, 400 + i,
                        Intent(Intent.ACTION_VIEW, Uri.parse(s.link)),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(ROW_IDS[i], pi)
                }
            } else {
                views.setTextViewText(NUM_IDS[i],   "")
                views.setTextViewText(TITLE_IDS[i], "")
                views.setTextViewText(TIME_IDS[i],  "")
            }
        }

        mgr.updateAppWidget(id, views)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun timeAgo(pubDate: String): String {
        if (pubDate.isEmpty()) return ""
        return try {
            val formats = listOf("EEE, dd MMM yyyy HH:mm:ss z", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ssXXX")
            var date: Date? = null
            for (fmt in formats) {
                try {
                    date = SimpleDateFormat(fmt, Locale.US).also { it.timeZone = TimeZone.getTimeZone("UTC") }.parse(pubDate)
                    if (date != null) break
                } catch (_: Exception) {}
            }
            val mins = ((System.currentTimeMillis() - (date ?: return "").time) / 60_000).toInt()
            when { mins < 1 -> "now"; mins < 60 -> "${mins}m"; mins < 1440 -> "${mins/60}h"; else -> "${mins/1440}d" }
        } catch (_: Exception) { "" }
    }
}
