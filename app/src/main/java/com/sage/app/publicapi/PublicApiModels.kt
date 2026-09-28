package com.sage.app.publicapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PublicApiEntry(
    @Json(name = "API") val api: String,
    @Json(name = "Description") val description: String,
    @Json(name = "Auth") val auth: String? = null,
    @Json(name = "HTTPS") val https: Boolean = false,
    @Json(name = "Cors") val cors: String? = null,
    @Json(name = "Link") val link: String,
    @Json(name = "Category") val category: String? = null,
)

@JsonClass(generateAdapter = true)
data class PublicApiEntriesResponse(
    @Json(name = "count") val count: Int = 0,
    @Json(name = "entries") val entries: List<PublicApiEntry> = emptyList(),
)
