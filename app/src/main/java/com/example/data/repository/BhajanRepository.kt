package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.BhajanEntity
import com.example.data.db.PlaylistBhajanCrossRef
import com.example.data.db.PlaylistEntity
import com.example.data.model.BhajanItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray

class BhajanRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val bhajanDao = database.bhajanDao()
    private val playlistDao = database.playlistDao()

    suspend fun initializeIfEmpty() = withContext(Dispatchers.IO) {
        bhajanDao.clearDemoBhajans()
        val count = bhajanDao.getCount()
        if (count == 0) {
            val loaded = loadBhajansFromAssets()
            if (loaded.isNotEmpty()) {
                bhajanDao.insertAll(loaded)
            }
        }
    }

    suspend fun deleteBhajan(id: String) = withContext(Dispatchers.IO) {
        val bhajan = bhajanDao.getBhajanById(id)
        if (bhajan != null) {
            if (bhajan.audioFile.startsWith(context.filesDir.absolutePath)) {
                try { java.io.File(bhajan.audioFile).delete() } catch (_: Exception) {}
            }
            if (bhajan.thumbnail.startsWith(context.filesDir.absolutePath)) {
                try { java.io.File(bhajan.thumbnail).delete() } catch (_: Exception) {}
            }
        }
        bhajanDao.deleteBhajan(id)
        playlistDao.removeBhajanFromAllPlaylists(id)
    }

    private fun loadBhajansFromAssets(): List<BhajanEntity> {
        val list = mutableListOf<BhajanEntity>()
        try {
            val jsonString = context.assets.open("bhajans.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "bhajan_${i + 1}")
                val title = obj.optString("title", "Bhajan ${i + 1}")
                val titleSindhi = obj.optString("titleSindhi", title)
                val artist = obj.optString("artist", "")
                val audioFile = obj.optString("audioFile", "")
                val thumbnail = obj.optString("thumbnail", "")
                val category = obj.optString("category", "Devotional")
                list.add(
                    BhajanEntity(
                        id = id,
                        title = title,
                        titleSindhi = titleSindhi,
                        artist = artist,
                        audioFile = audioFile,
                        thumbnail = thumbnail,
                        category = category,
                        customOrder = i,
                        isFavorite = false,
                        lastPositionMs = 0L
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getAllBhajans(): Flow<List<BhajanItem>> {
        return bhajanDao.getAllBhajans().map { entities ->
            entities.map { it.toItem() }
        }
    }

    fun getFavoriteBhajans(): Flow<List<BhajanItem>> {
        return bhajanDao.getFavoriteBhajans().map { entities ->
            entities.map { it.toItem() }
        }
    }

    suspend fun getBhajanById(id: String): BhajanItem? = withContext(Dispatchers.IO) {
        bhajanDao.getBhajanById(id)?.toItem()
    }

    suspend fun addCustomBhajan(
        title: String,
        titleSindhi: String,
        artist: String,
        audioFile: String,
        thumbnail: String,
        category: String = "My Bhajans"
    ) = withContext(Dispatchers.IO) {
        val count = bhajanDao.getCount()
        val newEntity = BhajanEntity(
            id = "custom_${System.currentTimeMillis()}",
            title = title,
            titleSindhi = if (titleSindhi.isNotBlank()) titleSindhi else title,
            artist = if (artist.isNotBlank()) artist else "Personal Recording",
            audioFile = audioFile,
            thumbnail = thumbnail,
            category = category,
            customOrder = count,
            isFavorite = false,
            lastPositionMs = 0L
        )
        bhajanDao.insertBhajan(newEntity)
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        bhajanDao.updateFavorite(id, isFavorite)
    }

    suspend fun moveBhajanOrder(fromIndex: Int, toIndex: Int, currentList: List<BhajanItem>) = withContext(Dispatchers.IO) {
        if (fromIndex in currentList.indices && toIndex in currentList.indices && fromIndex != toIndex) {
            val mutable = currentList.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            mutable.forEachIndexed { index, bhajanItem ->
                bhajanDao.updateOrder(bhajanItem.id, index)
            }
        }
    }

    suspend fun moveBhajanUp(index: Int, currentList: List<BhajanItem>) {
        if (index > 0) {
            moveBhajanOrder(index, index - 1, currentList)
        }
    }

    suspend fun moveBhajanDown(index: Int, currentList: List<BhajanItem>) {
        if (index < currentList.lastIndex) {
            moveBhajanOrder(index, index + 1, currentList)
        }
    }

    suspend fun updateLastPosition(id: String, positionMs: Long) = withContext(Dispatchers.IO) {
        bhajanDao.updateLastPosition(id, positionMs)
    }

    // Playlists
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(id: Long, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.renamePlaylist(id, newName)
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.clearPlaylistSongs(id)
        playlistDao.deletePlaylist(id)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<BhajanItem>> {
        return playlistDao.getSongsForPlaylist(playlistId).map { entities ->
            entities.map { it.toItem() }
        }
    }

    suspend fun addSongToPlaylist(playlistId: Long, bhajanId: String, orderIndex: Int) = withContext(Dispatchers.IO) {
        playlistDao.insertSongToPlaylist(
            PlaylistBhajanCrossRef(
                playlistId = playlistId,
                bhajanId = bhajanId,
                orderIndex = orderIndex
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, bhajanId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, bhajanId)
    }

    suspend fun reorderPlaylistSongs(playlistId: Long, fromIndex: Int, toIndex: Int, songs: List<BhajanItem>) = withContext(Dispatchers.IO) {
        if (fromIndex in songs.indices && toIndex in songs.indices && fromIndex != toIndex) {
            val mutable = songs.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            mutable.forEachIndexed { index, song ->
                playlistDao.updateSongOrderInPlaylist(playlistId, song.id, index)
            }
        }
    }

    private fun BhajanEntity.toItem() = BhajanItem(
        id = id,
        title = title,
        titleSindhi = titleSindhi,
        artist = artist,
        audioFile = audioFile,
        thumbnail = thumbnail,
        category = category,
        customOrder = customOrder,
        isFavorite = isFavorite,
        lastPositionMs = lastPositionMs
    )
}
