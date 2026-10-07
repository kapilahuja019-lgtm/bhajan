package com.example.data.model

data class BhajanItem(
    val id: String,
    val title: String,
    val titleSindhi: String = "",
    val artist: String = "",
    val audioFile: String,
    val thumbnail: String,
    val category: String = "",
    val customOrder: Int = 0,
    val isFavorite: Boolean = false,
    val durationMs: Long = 0L,
    val lastPositionMs: Long = 0L
)
