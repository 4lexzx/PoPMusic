package com.popmusic.app.data.podcasts

import android.content.Context
import android.util.Log
import android.util.Xml
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Catálogo de podcasts real: iTunes Search API (búsqueda/categorías) y
 * feeds RSS oficiales (episodios mp3). Sin dependencias nuevas: JSON con
 * org.json y XML con XmlPullParser nativo.
 *
 * Todas las llamadas son bloqueantes: ejecutar en Dispatchers.IO.
 */
class PodcastRepository(context: Context) {

    private val prefs = context.getSharedPreferences("podcast_state", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Cachés en memoria de la sesión.
    @Volatile private var topCache: List<Podcast>? = null
    private val feedCache = LinkedHashMap<String, List<PodcastEpisode>>(16, 0.75f, true)

    // ————————————————— Catálogo —————————————————

    /** Categorías curadas para los chips. */
    fun categories(): List<String> = PODCAST_CATEGORIES

    /** Busca podcasts por término (iTunes Search API). */
    fun search(term: String): List<Podcast> {
        val clean = term.trim()
        if (clean.isEmpty()) return emptyList()
        val url = "https://itunes.apple.com/search?term=" +
            URLEncoder.encode(clean, "UTF-8") +
            "&media=podcast&entity=podcast&limit=24"
        val body = get(url) ?: return emptyList()
        return runCatching {
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return emptyList()
            buildList {
                for (i in 0 until results.length()) {
                    val obj = results.optJSONObject(i) ?: continue
                    val feedUrl = obj.optString("feedUrl")
                    if (feedUrl.isBlank()) continue
                    add(
                        Podcast(
                            id = obj.optString("collectionId")
                                .ifBlank { obj.optString("feedUrl") },
                            title = obj.optString("collectionName"),
                            author = obj.optString("artistName"),
                            artworkUrl = obj.optString("artworkUrl600")
                                .ifBlank { obj.optString("artworkUrl100") },
                            feedUrl = feedUrl,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /** Top de podcasts (RSS oficial de iTunes) + lookup de feeds. */
    fun top(): List<Podcast> {
        topCache?.let { return it }
        val body = get("https://itunes.apple.com/us/rss/toppodcasts/limit=24/json")
            ?: return emptyList()
        val ids = runCatching {
            val json = JSONObject(body)
            val entries = json.getJSONObject("feed").optJSONArray("entry")
                ?: return emptyList()
            buildList {
                for (i in 0 until entries.length()) {
                    val entry = entries.optJSONObject(i) ?: continue
                    val id = entry.optJSONObject("id")
                        ?.optJSONObject("attributes")
                        ?.optString("im:id")
                    if (!id.isNullOrBlank()) add(id)
                }
            }
        }.getOrDefault(emptyList())
        if (ids.isEmpty()) return emptyList()

        // lookup admite varios ids separados por coma (por tandas de 10).
        val podcasts = mutableListOf<Podcast>()
        ids.chunked(10).forEach { chunk ->
            val lookup = get(
                "https://itunes.apple.com/lookup?id=${chunk.joinToString(",")}&entity=podcast",
            ) ?: return@forEach
            runCatching {
                val json = JSONObject(lookup)
                val results = json.optJSONArray("results") ?: return@forEach
                for (i in 0 until results.length()) {
                    val obj = results.optJSONObject(i) ?: continue
                    val feedUrl = obj.optString("feedUrl")
                    if (feedUrl.isBlank()) continue
                    podcasts += Podcast(
                        id = obj.optString("collectionId").ifBlank { feedUrl },
                        title = obj.optString("collectionName"),
                        author = obj.optString("artistName"),
                        artworkUrl = obj.optString("artworkUrl600")
                            .ifBlank { obj.optString("artworkUrl100") },
                        feedUrl = feedUrl,
                    )
                }
            }
        }
        return podcasts.filter { it.title.isNotBlank() }
            .also { topCache = it }
    }

    /** Episodes de un feed RSS (con caché LRU en memoria). */
    fun episodes(feedUrl: String, podcast: Podcast? = null): List<PodcastEpisode> {
        feedCache[feedUrl]?.let { return it }
        val body = get(feedUrl) ?: return emptyList()
        val parsed = runCatching { parseFeed(body, podcast) }.getOrElse {
            Log.w(TAG, "Feed no parseable: $feedUrl", it)
            emptyList()
        }
        if (parsed.isNotEmpty()) {
            if (feedCache.size >= 24) feedCache.remove(feedCache.keys.first())
            feedCache[feedUrl] = parsed
        }
        return parsed
    }

    // ————————————————— Suscripciones —————————————————

    fun subscriptions(): List<Podcast> = runCatching {
        val raw = prefs.getString(KEY_SUBS, null) ?: return emptyList()
        val json = JSONObject(raw)
        json.keys().asSequence().mapNotNull { key ->
            val obj = json.optJSONObject(key) ?: return@mapNotNull null
            Podcast(
                id = key,
                title = obj.optString("title"),
                author = obj.optString("author"),
                artworkUrl = obj.optString("artwork").ifBlank { null },
                feedUrl = obj.optString("feed"),
            )
        }.filter { it.feedUrl.isNotBlank() }.toList()
    }.getOrDefault(emptyList())

    fun isSubscribed(id: String): Boolean =
        prefs.getString(KEY_SUBS, null)
            ?.let { runCatching { JSONObject(it).has(id) }.getOrDefault(false) } == true

    /** Devuelve true si quedó suscrito tras el toque. */
    fun toggleSubscribe(podcast: Podcast): Boolean {
        val json = runCatching {
            JSONObject(prefs.getString(KEY_SUBS, null) ?: "{}")
        }.getOrDefault(JSONObject())
        val nowSubscribed = if (json.has(podcast.id)) {
            json.remove(podcast.id)
            false
        } else {
            json.put(
                podcast.id,
                JSONObject().apply {
                    put("title", podcast.title)
                    put("author", podcast.author)
                    put("artwork", podcast.artworkUrl.orEmpty())
                    put("feed", podcast.feedUrl)
                },
            )
            true
        }
        prefs.edit().putString(KEY_SUBS, json.toString()).apply()
        return nowSubscribed
    }

    // ————————————————— Recientes ("continuar escuchando") —————————————————

    fun recent(): List<PodcastEpisode> = runCatching {
        val raw = prefs.getString(KEY_RECENT, null) ?: return emptyList()
        val json = JSONObject(raw)
        json.keys().asSequence().mapNotNull { key ->
            val obj = json.optJSONObject(key) ?: return@mapNotNull null
            val audio = obj.optString("audio")
            if (audio.isBlank()) return@mapNotNull null
            PodcastEpisode(
                id = key,
                title = obj.optString("title"),
                description = "",
                audioUrl = audio,
                durationSeconds = obj.optLong("duration"),
                publishedAt = obj.optLong("published"),
                podcastTitle = obj.optString("podcast"),
                artworkUrl = obj.optString("artwork").ifBlank { null },
                podcastFeedUrl = obj.optString("feed"),
            )
        }.sortedByDescending { it.publishedAt }.toList()
    }.getOrDefault(emptyList())

    fun markPlayed(episode: PodcastEpisode) {
        val json = runCatching {
            JSONObject(prefs.getString(KEY_RECENT, null) ?: "{}")
        }.getOrDefault(JSONObject())
        json.put(
            episode.id,
            JSONObject().apply {
                put("title", episode.title)
                put("audio", episode.audioUrl)
                put("duration", episode.durationSeconds)
                put("published", episode.publishedAt)
                put("podcast", episode.podcastTitle)
                put("artwork", episode.artworkUrl.orEmpty())
                put("feed", episode.podcastFeedUrl)
            },
        )
        // Mantén solo los 12 más recientes.
        while (json.length() > 12) {
            val oldest = json.keys().asSequence()
                .minByOrNull { json.optJSONObject(it)?.optLong("published") ?: 0L }
                ?: break
            json.remove(oldest)
        }
        prefs.edit().putString(KEY_RECENT, json.toString()).apply()
    }

    // ————————————————— Interno —————————————————

    private fun get(url: String): String? = runCatching {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.string()
        }
    }.getOrNull()

    private fun parseFeed(xml: String, podcast: Podcast?): List<PodcastEpisode> {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        // Sin namespaces: los prefijos (itunes:) quedan en `name`.
        var event = parser.eventType

        var channelTitle = podcast?.title.orEmpty()
        var channelArtwork = podcast?.artworkUrl
        var inImage = false
        var inItem = false
        var inDescription = false

        var title = ""
        var guid = ""
        var audio = ""
        var pubDate = ""
        var duration = ""
        var description = ""
        val episodes = mutableListOf<PodcastEpisode>()

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    when {
                        name == "image" && !inItem -> inImage = true
                        name == "url" && inImage && channelArtwork == null ->
                            channelArtwork = parser.nextText().trim()

                        name == "item" -> {
                            inItem = true
                            title = ""; guid = ""; audio = ""
                            pubDate = ""; duration = ""; description = ""
                        }

                        inItem && name == "title" -> title = parser.nextText().trim()
                        inItem && name == "guid" -> guid = parser.nextText().trim()
                        inItem && name == "enclosure" ->
                            audio = parser.getAttributeValue(null, "url")
                                ?: parser.getAttributeValue("", "url").orEmpty()

                        inItem && (name == "pubDate") -> pubDate = parser.nextText().trim()
                        inItem && name == "itunes:duration" ->
                            duration = parser.nextText().trim()

                        inItem && (name == "description" || name == "itunes:summary" ||
                            name == "content:encoded") -> {
                            inDescription = true
                            description = parser.nextText()
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    when {
                        name == "image" -> inImage = false
                        name == "description" || name == "itunes:summary" ||
                            name == "content:encoded" -> inDescription = false

                        name == "item" -> {
                            inItem = false
                            val finalAudio = audio
                            if (finalAudio.isNotBlank()) {
                                episodes += PodcastEpisode(
                                    id = guid.ifBlank { finalAudio },
                                    title = title.ifBlank { "Episodio" },
                                    description = stripHtml(description).take(240),
                                    audioUrl = finalAudio,
                                    durationSeconds = parseDuration(duration),
                                    publishedAt = parseDate(pubDate),
                                    podcastTitle = channelTitle,
                                    artworkUrl = channelArtwork,
                                    podcastFeedUrl = podcast?.feedUrl.orEmpty(),
                                )
                            }
                            if (episodes.size >= 30) return episodes
                        }
                    }
                }
            }
            if (inDescription && event == XmlPullParser.TEXT) {
                // nextText() ya consumió el contenido; se ignora aquí.
            }
            event = parser.next()
        }
        return episodes
    }

    private fun stripHtml(raw: String): String = raw
        .replace(Regex("<[^>]*>"), " ")
        .replace(Regex("&nbsp;"), " ")
        .replace(Regex("&amp;"), "&")
        .replace(Regex("&#39;|&apos;"), "'")
        .replace(Regex("&quot;"), "\"")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun parseDuration(raw: String): Long {
        if (raw.isBlank()) return 0L
        val parts = raw.split(":").map { it.toLongOrNull() ?: 0L }
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            else -> parts.firstOrNull() ?: 0L
        }
    }

    private fun parseDate(raw: String): Long {
        if (raw.isBlank()) return 0L
        return runCatching {
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
                .parse(raw)?.time ?: 0L
        }.getOrDefault(0L)
    }

    private companion object {
        const val TAG = "PodcastRepo"
        const val KEY_SUBS = "subscriptions"
        const val KEY_RECENT = "recent"
    }
}
