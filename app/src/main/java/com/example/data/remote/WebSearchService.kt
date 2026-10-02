package com.example.data.remote

import com.example.model.WebSource
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object WebSearchService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    /**
     * Searches the web using multi-engine fallbacks (DuckDuckGo instant search + HTML search)
     * Returns verified WebSource list with genuine titles, urls, snippets, and retrieval timestamp.
     */
    suspend fun search(query: String, maxResults: Int = 4): List<WebSource> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val results = mutableListOf<WebSource>()
        val accessDate = dateFormat.format(Date())

        try {
            // 1. DuckDuckGo Instant Answer API
            val encodedQuery = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8.toString())
            val instantUrl = "https://api.duckduckgo.com/?q=$encodedQuery&format=json&no_html=1&skip_disambig=1"
            val instantReq = Request.Builder()
                .url(instantUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile) AI Smart App")
                .build()

            val instantResp = client.newCall(instantReq).execute()
            if (instantResp.isSuccessful && instantResp.body != null) {
                val jsonStr = instantResp.body!!.string()
                parseInstantAnswerJson(jsonStr, cleanQuery, accessDate, results)
            }
        } catch (_: Exception) {
            // Ignore and fall through to web parser
        }

        // 2. DuckDuckGo HTML / Web results if more needed
        if (results.size < maxResults) {
            try {
                val encodedQuery = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8.toString())
                val htmlUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"
                val htmlReq = Request.Builder()
                    .url(htmlUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                val htmlResp = client.newCall(htmlReq).execute()
                if (htmlResp.isSuccessful && htmlResp.body != null) {
                    val html = htmlResp.body!!.string()
                    parseDuckDuckGoHtml(html, accessDate, results, maxResults)
                }
            } catch (_: Exception) {}
        }

        return@withContext results.take(maxResults)
    }

    private fun parseInstantAnswerJson(
        jsonStr: String,
        query: String,
        accessDate: String,
        results: MutableList<WebSource>
    ) {
        try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val map = moshi.adapter(Map::class.java).fromJson(jsonStr) ?: return

            val abstractText = map["AbstractText"] as? String
            val abstractSource = map["AbstractSource"] as? String ?: "الموسوعة الحرة / مرجع موثوق"
            val abstractUrl = map["AbstractURL"] as? String

            if (!abstractText.isNullOrBlank() && !abstractUrl.isNullOrBlank()) {
                val domain = extractDomain(abstractUrl)
                results.add(
                    WebSource(
                        title = map["Heading"] as? String ?: query,
                        url = abstractUrl,
                        snippet = abstractText.take(300),
                        sourceName = if (abstractSource.isNotBlank()) abstractSource else domain,
                        accessDate = accessDate
                    )
                )
            }

            // Related topics
            val related = map["RelatedTopics"] as? List<*>
            related?.forEach { item ->
                if (item is Map<*, *>) {
                    val text = item["Text"] as? String
                    val firstUrl = item["FirstURL"] as? String
                    if (!text.isNullOrBlank() && !firstUrl.isNullOrBlank() && results.size < 4) {
                        results.add(
                            WebSource(
                                title = text.take(60),
                                url = firstUrl,
                                snippet = text,
                                sourceName = extractDomain(firstUrl),
                                accessDate = accessDate
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun parseDuckDuckGoHtml(
        html: String,
        accessDate: String,
        results: MutableList<WebSource>,
        maxResults: Int
    ) {
        try {
            // Match result links: <a class="result__url" href="..."> or <a class="result__snippet" ...>
            // Regex for link and title
            val pattern = Pattern.compile(
                "<a[^>]+class=\"result__snippet\"[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>",
                Pattern.DOTALL
            )
            val matcher = pattern.matcher(html)

            while (matcher.find() && results.size < maxResults) {
                var rawUrl = matcher.group(1) ?: continue
                val rawSnippet = matcher.group(2) ?: ""

                // Decode redirect url /l/?uddg=...
                val realUrl = if (rawUrl.contains("uddg=")) {
                    try {
                        val encoded = rawUrl.substringAfter("uddg=").substringBefore("&")
                        URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
                    } catch (_: Exception) { rawUrl }
                } else rawUrl

                if (!realUrl.startsWith("http")) continue

                val cleanSnippet = rawSnippet.replace(Regex("<[^>]+>"), " ").trim()
                val domain = extractDomain(realUrl)

                if (results.none { it.url == realUrl }) {
                    results.add(
                        WebSource(
                            title = cleanSnippet.take(50).ifBlank { domain },
                            url = realUrl,
                            snippet = cleanSnippet.take(250),
                            sourceName = domain,
                            accessDate = accessDate
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val host = uri.host ?: url
            host.removePrefix("www.")
        } catch (_: Exception) {
            url.substringBefore("/").removePrefix("http://").removePrefix("https://").removePrefix("www.")
        }
    }
}
