package com.sage.app.publicapi

import com.sage.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class PublicApiRepository(private val http: OkHttpClient = OkHttpClient()) {
    suspend fun search(query: String, category: String?): Result<List<PublicApiEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            require(BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) { "Supabase is not configured in this build." }
            val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/public_apis".toHttpUrl().newBuilder()
                .addQueryParameter("select", "name,description,auth,https,cors,link,category")
                .addQueryParameter("order", "name.asc")
                .addQueryParameter("limit", "50")
            if (category.isNullOrBlank()) {
                if (query.isNotBlank()) url.addQueryParameter("name", "ilike.*${escape(query)}*")
            } else {
                url.addQueryParameter("category", "eq.${escape(category)}")
                if (query.isNotBlank()) url.addQueryParameter("name", "ilike.*${escape(query)}*")
            }
            val request = Request.Builder().url(url.build())
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .get().build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Catalogue request failed: HTTP ${response.code}")
                parse(response.body?.string().orEmpty())
            }
        }
    }

    suspend fun categories(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/public_apis".toHttpUrl().newBuilder()
                .addQueryParameter("select", "category").addQueryParameter("category", "not.is.null").addQueryParameter("limit", "2000").build()
            val request = Request.Builder().url(url).addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY).addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}").get().build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Category request failed: HTTP ${response.code}")
                JSONArray(response.body?.string().orEmpty()).let { arr ->
                    (0 until arr.length()).mapNotNull { arr.getJSONObject(it).optString("category").takeIf(String::isNotBlank) }.distinct().sorted()
                }
            }
        }
    }

    private fun parse(body: String): List<PublicApiEntry> = JSONArray(body).let { arr ->
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            PublicApiEntry(
                api = o.optString("name"), description = o.optString("description"), auth = o.optString("auth").takeIf { it.isNotBlank() },
                https = o.optBoolean("https"), cors = o.optString("cors").takeIf { it.isNotBlank() }, link = o.optString("link"), category = o.optString("category").takeIf { it.isNotBlank() }
            )
        }
    }
    private fun escape(value: String) = value.replace("*", "").replace("%", "").replace("_", "").replace(",", " ")
}
