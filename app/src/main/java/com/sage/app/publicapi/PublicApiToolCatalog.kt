package com.sage.app.publicapi

import com.sage.app.needle.NeedleJsonSchemaObject
import com.sage.app.needle.NeedleJsonSchemaProperty
import com.sage.app.needle.NeedleToolSchema

/** Safe local tools exposed to Needle for API discovery. */
object PublicApiToolCatalog {
    fun schemas(): List<NeedleToolSchema> = listOf(
        NeedleToolSchema(
            name = "search_public_apis",
            description = "Find public APIs by name, description, or category. This only searches the catalogue; it does not execute an API.",
            parameters = NeedleJsonSchemaObject(
                properties = mapOf(
                    "query" to NeedleJsonSchemaProperty("string", "Name or description to search for"),
                    "category" to NeedleJsonSchemaProperty("string", "Optional API category"),
                ),
                required = listOf("query")
            )
        ),
        NeedleToolSchema(
            name = "open_public_api_docs",
            description = "Open the documentation link for a selected public API. Never execute an arbitrary URL from user input.",
            parameters = NeedleJsonSchemaObject(
                properties = mapOf(
                    "api_name" to NeedleJsonSchemaProperty("string", "Exact catalogue API name")
                ),
                required = listOf("api_name")
            )
        )
    )
}
