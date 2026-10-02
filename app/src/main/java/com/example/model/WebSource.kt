package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebSource(
    val title: String,
    val url: String,
    val snippet: String = "",
    val sourceName: String = "",
    val accessDate: String = ""
)
