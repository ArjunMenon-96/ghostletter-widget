package com.ghostletter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
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
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import com.ghostletter.app.BuildConfig

class GhostWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH = "com.ghostletter.app.REFRESH_WIDGET"
        private val isUpdating = AtomicBoolean(false)

        val ROW_IDS   = intArrayOf(R.id.gl_row_0, R.id.gl_row_1, R.id.gl_row_2, R.id.gl_row_3, R.id.gl_row_4,
                                   R.id.gl_row_5, R.id.gl_row_6, R.id.gl_row_7, R.id.gl_row_8, R.id.gl_row_9)
        val SRC_IDS   = intArrayOf(R.id.gl_src_0, R.id.gl_src_1, R.id.gl_src_2, R.id.gl_src_3, R.id.gl_src_4,
                                   R.id.gl_src_5, R.id.gl_src_6, R.id.gl_src_7, R.id.gl_src_8, R.id.gl_src_9)
        val TITLE_IDS = intArrayOf(R.id.gl_title_0, R.id.gl_title_1, R.id.gl_title_2, R.id.gl_title_3, R.id.gl_title_4,
                                   R.id.gl_title_5, R.id.gl_title_6, R.id.gl_title_7, R.id.gl_title_8, R.id.gl_title_9)
        val THUMB_IDS = intArrayOf(R.id.gl_thumb_0, R.id.gl_thumb_1, R.id.gl_thumb_2, R.id.gl_thumb_3, R.id.gl_thumb_4,
                                   R.id.gl_thumb_5, R.id.gl_thumb_6, R.id.gl_thumb_7, R.id.gl_thumb_8, R.id.gl_thumb_9)
        val DIV_IDS   = intArrayOf(R.id.gl_div_0, R.id.gl_div_1, R.id.gl_div_2, R.id.gl_div_3, R.id.gl_div_4,
                                   R.id.gl_div_5, R.id.gl_div_6, R.id.gl_div_7, R.id.gl_div_8, R.id.gl_div_9)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, GhostWidgetProvider::class.java))
            if (ids.isNotEmpty()) onUpdate(context, mgr, ids)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (!isUpdating.compareAndSet(false, true)) return

        for (id in appWidgetIds) showLoading(context, appWidgetManager, id)

        Thread {
            val pool = Executors.newCachedThreadPool()
            try {
                if (!isNetworkAvailable(context)) {
                    for (id in appWidgetIds) showError(context, appWidgetManager, id, offline = true)
                    return@Thread
                }

                // Fetch prices and stories concurrently
                val pricesFuture  = pool.submit<List<CoinPrice>> { tryFetchPrices() }
                val storiesFuture = pool.submit<List<Story>>     { fetchStories() }

                val prices  = pricesFuture.get(12, TimeUnit.SECONDS)
                val stories = storiesFuture.get(12, TimeUnit.SECONDS)

                for (id in appWidgetIds) {
                    val rows = visibleRows(appWidgetManager, id)
                    // Only fetch thumbnails for rows that are actually visible, in parallel
                    val thumbFutures: List<Future<Bitmap?>> = stories.take(rows).map { s ->
                        pool.submit<Bitmap?> { loadThumb(s.imageUrl) }
                    }
                    val thumbs = thumbFutures.map { f ->
                        try { f.get(5, TimeUnit.SECONDS) } catch (_: Exception) { null }
                    }
                    updateWidget(context, appWidgetManager, id, stories, prices, rows, thumbs)
                }
            } catch (e: Exception) {
                for (id in appWidgetIds) showError(context, appWidgetManager, id, offline = false)
            } finally {
                isUpdating.set(false)
                pool.shutdownNow()
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

    // ── Visible row calculation ─────────────────────────────────────────────

    private fun visibleRows(mgr: AppWidgetManager, id: Int): Int {
        val opts = mgr.getAppWidgetOptions(id)
        val maxH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 240)
        return ((maxH - 56) / 44).coerceIn(1, 10)
    }

    // ── Loading state ───────────────────────────────────────────────────────

    private fun showLoading(ctx: Context, mgr: AppWidgetManager, id: Int) {
        val views = RemoteViews(ctx.packageName, R.layout.ghost_widget)
        for (i in 0 until 10) {
            val vis = if (i < 5) View.VISIBLE else View.GONE
            views.setViewVisibility(ROW_IDS[i], vis)
            views.setViewVisibility(DIV_IDS[i], if (i < 4) View.VISIBLE else View.GONE)
            views.setTextViewText(SRC_IDS[i], "")
            views.setTextViewText(TITLE_IDS[i], "")
            views.setViewVisibility(THUMB_IDS[i], View.GONE)
        }
        views.setTextViewText(SRC_IDS[0], "LOADING")
        views.setTextViewText(TITLE_IDS[0], "Fetching latest headlines…")
        views.setTextViewText(R.id.gl_price, "")
        views.setTextViewText(R.id.gl_footer, "ghostletter.online  v${BuildConfig.VERSION_NAME}  ·  loading…")
        mgr.updateAppWidget(id, views)
    }

    // ── Error state ─────────────────────────────────────────────────────────

    private fun showError(ctx: Context, mgr: AppWidgetManager, id: Int, offline: Boolean) {
        val views = RemoteViews(ctx.packageName, R.layout.ghost_widget)

        for (i in 0 until 10) {
            views.setViewVisibility(ROW_IDS[i], if (i < 5) View.VISIBLE else View.GONE)
            views.setViewVisibility(DIV_IDS[i], if (i < 4) View.VISIBLE else View.GONE)
            views.setTextViewText(SRC_IDS[i], "")
            views.setTextViewText(TITLE_IDS[i], "")
            views.setViewVisibility(THUMB_IDS[i], View.GONE)
        }

        views.setTextViewText(SRC_IDS[0], if (offline) "NO INTERNET" else "FEED UNAVAILABLE")
        views.setTextViewText(TITLE_IDS[0], if (offline) "Connect to Wi-Fi or mobile data, then tap ↻ to retry." else "Couldn't load headlines. Tap ↻ to retry.")
        views.setTextViewText(R.id.gl_price, if (offline) "● OFFLINE" else "● ERROR")
        views.setTextViewText(R.id.gl_footer, "ghostletter.online  v${BuildConfig.VERSION_NAME}  ·  tap ↻ to refresh")

        val refreshIntent = Intent(ctx, GhostWidgetProvider::class.java).apply { action = ACTION_REFRESH }
        val refreshPi = PendingIntent.getBroadcast(ctx, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.gl_refresh, refreshPi)
        views.setOnClickPendingIntent(ROW_IDS[0], refreshPi)

        mgr.updateAppWidget(id, views)
    }

    // ── Price fetching ──────────────────────────────────────────────────────

    data class CoinPrice(val symbol: String, val price: Double, val change24h: Double)

    private fun tryFetchPrices(): List<CoinPrice> = try { fetchPrices() } catch (_: Exception) { emptyList() }

    private fun fetchPrices(): List<CoinPrice> {
        val url = URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=usd&include_24hr_change=true")
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
            if (i > 0) sb.append("   ")
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

    // ── News fetching ───────────────────────────────────────────────────────

    private fun fetchStories(): List<Story> {
        val url  = URL("https://ghostletter.online/api/news")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout    = 10_000
        conn.setRequestProperty("User-Agent", "GhostLetter-Widget/1.0")
        return try { parseStories(conn.inputStream.bufferedReader().readText()) }
        finally    { conn.disconnect() }
    }

    private fun parseStories(json: String): List<Story> {
        val arr: JSONArray = try { JSONArray(json) } catch (_: Exception) {
            JSONObject(json).optJSONArray("items") ?: JSONArray()
        }
        return (0 until minOf(arr.length(), 10)).map { i ->
            val obj = arr.getJSONObject(i)
            Story(
                title    = obj.optString("title", "").trim(),
                link     = obj.optString("link", obj.optString("url", "")),
                source   = obj.optString("source", obj.optString("feedTitle", "")).uppercase(),
                category = obj.optString("category", "").uppercase(),
                pubDate  = obj.optString("pubDate", obj.optString("publishedAt", "")),
                imageUrl = obj.optString("imageUrl", obj.optString("image", obj.optString("thumbnail", ""))).takeIf { it.isNotEmpty() },
            )
        }
    }

    // ── Thumbnail loading ───────────────────────────────────────────────────

    private fun loadThumb(urlStr: String?): Bitmap? {
        if (urlStr.isNullOrEmpty()) return null
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout    = 5_000
            conn.doInput = true
            conn.connect()
            val raw = BitmapFactory.decodeStream(conn.inputStream) ?: return null
            conn.disconnect()
            val maxPx = 128
            if (raw.width <= maxPx) raw
            else Bitmap.createScaledBitmap(raw, maxPx, (raw.height * maxPx.toFloat() / raw.width).toInt(), true)
        } catch (_: Exception) { null }
    }

    // ── Widget rendering ────────────────────────────────────────────────────

    private fun updateWidget(
        ctx: Context,
        mgr: AppWidgetManager,
        id: Int,
        stories: List<Story>,
        prices: List<CoinPrice>,
        rows: Int,
        thumbs: List<Bitmap?>
    ) {
        val views = RemoteViews(ctx.packageName, R.layout.ghost_widget)

        // Price header
        if (prices.isNotEmpty()) {
            views.setTextViewText(R.id.gl_price, buildPriceSpan(prices))
        } else {
            views.setTextViewText(R.id.gl_price, "● LIVE")
        }

        // Refresh button
        val refreshIntent = Intent(ctx, GhostWidgetProvider::class.java).apply { action = ACTION_REFRESH }
        val refreshPi = PendingIntent.getBroadcast(ctx, 200, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.gl_refresh, refreshPi)

        // Header tap → open website
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ghostletter.online"))
        val webPi = PendingIntent.getActivity(ctx, 0, webIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.gl_header, webPi)

        // Story rows + dividers (divider sits BETWEEN rows, so last visible row has no divider)
        for (i in 0 until 10) {
            val visible = i < rows
            views.setViewVisibility(ROW_IDS[i], if (visible) View.VISIBLE else View.GONE)
            // Divider visible only if both this row AND next row are visible
            views.setViewVisibility(DIV_IDS[i], if (visible && i + 1 < rows) View.VISIBLE else View.GONE)

            if (visible && i < stories.size) {
                val s = stories[i]

                // Merged source line: SOURCE  ·  CATEGORY  ·  7m
                val timeStr = timeAgo(s.pubDate)
                val srcText = buildString {
                    append(s.source)
                    if (s.category.isNotEmpty()) append("  ·  ${s.category}")
                    if (timeStr.isNotEmpty()) append("  ·  $timeStr")
                }
                views.setTextViewText(SRC_IDS[i], srcText)
                views.setTextViewText(TITLE_IDS[i], s.title)

                // Thumbnail
                val bmp = thumbs.getOrNull(i)
                if (bmp != null) {
                    views.setImageViewBitmap(THUMB_IDS[i], bmp)
                    views.setViewVisibility(THUMB_IDS[i], View.VISIBLE)
                } else {
                    views.setViewVisibility(THUMB_IDS[i], View.GONE)
                }

                // Row tap → open article
                if (s.link.isNotEmpty()) {
                    val pi = PendingIntent.getActivity(
                        ctx, 100 + i,
                        Intent(Intent.ACTION_VIEW, Uri.parse(s.link)),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(ROW_IDS[i], pi)
                }
            } else if (visible) {
                views.setTextViewText(SRC_IDS[i], "")
                views.setTextViewText(TITLE_IDS[i], "")
                views.setViewVisibility(THUMB_IDS[i], View.GONE)
            }
        }

        views.setTextViewText(R.id.gl_footer, "ghostletter.online  v${BuildConfig.VERSION_NAME}  ·  updated ${formatNow()}")
        mgr.updateAppWidget(id, views)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    data class Story(
        val title: String,
        val link: String,
        val source: String,
        val category: String,
        val pubDate: String,
        val imageUrl: String?,
    )

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
                    date = SimpleDateFormat(fmt, Locale.US).also { it.timeZone = TimeZone.getTimeZone("UTC") }.parse(pubDate)
                    if (date != null) break
                } catch (_: Exception) {}
            }
            val mins = ((System.currentTimeMillis() - (date ?: return "").time) / 60_000).toInt()
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
}
