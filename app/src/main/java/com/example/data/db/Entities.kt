package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bhajans")
data class BhajanEntity(
    @PrimaryKey val id: String,
    val title: String,
    val titleSindhi: String,
    val artist: String,
    val audioFile: String,
    val thumbnail: String,
    val category: String,
    val customOrder: Int = 0,
    val isFavorite: Boolean = false,
    val lastPositionMs: Long = 0L
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_bhajans",
    primaryKeys = ["playlistId", "bhajanId"]
)
data class PlaylistBhajanCrossRef(
    val playlistId: Long,
    val bhajanId: String,
    val orderIndex: Int = 0
)
