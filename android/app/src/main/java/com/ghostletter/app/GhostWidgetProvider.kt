package com.ghostletter.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class GhostWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            showLoading(context, appWidgetManager, id)
        }
        Thread {
            try {
                val stories = fetchStories()
                for (id in appWidgetIds) {
                    updateWidget(context, appWidgetManager, id, stories)
                }
            } catch (e: Exception) {
                for (id in appWidgetIds) {
                    showError(context, appWidgetManager, id)
                }
            }
        }.start()
    }

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
        val intent = Intent(ctx, GhostWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val pi = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.gl_row_0, pi)
        mgr.updateAppWidget(id, views)
    }

    private fun fetchStories(): List<Story> {
        val url = URL("https://ghostletter111.vercel.app/api/news")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "GhostLetter-Widget/1.0")
        return try {
            val json = conn.inputStream.bufferedReader().readText()
            parseStories(json)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseStories(json: String): List<Story> {
        val stories = mutableListOf<Story>()
        // API returns array directly or {items:[...]}
        val arr: JSONArray = try {
            JSONArray(json)
        } catch (e: Exception) {
            JSONObject(json).optJSONArray("items") ?: JSONArray()
        }
        for (i in 0 until minOf(arr.length(), 5)) {
            val obj = arr.getJSONObject(i)
            val title = obj.optString("title", "").trim()
            val link = obj.optString("link", obj.optString("url", ""))
            val source = obj.optString("source", obj.optString("feedTitle", "")).uppercase()
            val category = obj.optString("category", "").uppercase()
            val pubDate = obj.optString("pubDate", obj.optString("publishedAt", ""))
            stories.add(Story(title, link, source, category, pubDate))
        }
        return stories
    }

    private fun updateWidget(ctx: Context, mgr: AppWidgetManager, id: Int, stories: List<Story>) {
        val views = RemoteViews(ctx.packageName, R.layout.ghost_widget)

        // App launch intent on header
        val launchApp = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        if (launchApp != null) {
            val pi = PendingIntent.getActivity(ctx, 0, launchApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.gl_header, pi)
        }

        val rowIds = intArrayOf(R.id.gl_row_0, R.id.gl_row_1, R.id.gl_row_2, R.id.gl_row_3, R.id.gl_row_4)
        val numIds = intArrayOf(R.id.gl_num_0, R.id.gl_num_1, R.id.gl_num_2, R.id.gl_num_3, R.id.gl_num_4)
        val srcIds = intArrayOf(R.id.gl_src_0, R.id.gl_src_1, R.id.gl_src_2, R.id.gl_src_3, R.id.gl_src_4)
        val timeIds = intArrayOf(R.id.gl_time_0, R.id.gl_time_1, R.id.gl_time_2, R.id.gl_time_3, R.id.gl_time_4)
        val titleIds = intArrayOf(R.id.gl_title_0, R.id.gl_title_1, R.id.gl_title_2, R.id.gl_title_3, R.id.gl_title_4)

        for (i in 0 until 5) {
            if (i < stories.size) {
                val s = stories[i]
                views.setTextViewText(numIds[i], "${i + 1}")
                views.setTextViewText(srcIds[i], if (s.category.isNotEmpty()) "${s.source.take(10)}  ·  ${s.category.take(8)}" else s.source.take(14))
                views.setTextViewText(timeIds[i], timeAgo(s.pubDate))
                views.setTextViewText(titleIds[i], s.title)

                if (s.link.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(s.link))
                    val pi = PendingIntent.getActivity(
                        ctx, 100 + i, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(rowIds[i], pi)
                }
            } else {
                views.setTextViewText(numIds[i], "")
                views.setTextViewText(srcIds[i], "")
                views.setTextViewText(timeIds[i], "")
                views.setTextViewText(titleIds[i], "")
            }
        }

        // Footer timestamp
        views.setTextViewText(R.id.gl_footer, "ghostletter111.vercel.app · updated ${formatNow()}")

        mgr.updateAppWidget(id, views)
    }

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
                    val sdf = SimpleDateFormat(fmt, Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    date = sdf.parse(pubDate)
                    if (date != null) break
                } catch (_: Exception) {}
            }
            val d = date ?: return ""
            val diffMs = System.currentTimeMillis() - d.time
            val mins = (diffMs / 60_000).toInt()
            when {
                mins < 1 -> "now"
                mins < 60 -> "${mins}m"
                mins < 1440 -> "${mins / 60}h"
                else -> "${mins / 1440}d"
            }
        } catch (_: Exception) { "" }
    }

    private fun formatNow(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date()) + " UTC"
    }

    data class Story(
        val title: String,
        val link: String,
        val source: String,
        val category: String,
        val pubDate: String
    )
}
