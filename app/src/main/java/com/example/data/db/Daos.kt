package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BhajanDao {
    @Query("SELECT * FROM bhajans ORDER BY customOrder ASC")
    fun getAllBhajans(): Flow<List<BhajanEntity>>

    @Query("SELECT * FROM bhajans WHERE isFavorite = 1 ORDER BY customOrder ASC")
    fun getFavoriteBhajans(): Flow<List<BhajanEntity>>

    @Query("SELECT * FROM bhajans WHERE id = :id LIMIT 1")
    suspend fun getBhajanById(id: String): BhajanEntity?

    @Query("SELECT COUNT(*) FROM bhajans")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBhajan(bhajan: BhajanEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(bhajans: List<BhajanEntity>)

    @Update
    suspend fun update(bhajan: BhajanEntity)

    @Query("UPDATE bhajans SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE bhajans SET customOrder = :newOrder WHERE id = :id")
    suspend fun updateOrder(id: String, newOrder: Int)

    @Query("UPDATE bhajans SET lastPositionMs = :positionMs WHERE id = :id")
    suspend fun updateLastPosition(id: String, positionMs: Long)

    @Query("DELETE FROM bhajans WHERE id = :id")
    suspend fun deleteBhajan(id: String)

    @Query("DELETE FROM bhajans WHERE id LIKE 'bhajan_%'")
    suspend fun clearDemoBhajans()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName WHERE id = :id")
    suspend fun renamePlaylist(id: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_bhajans WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongToPlaylist(crossRef: PlaylistBhajanCrossRef)

    @Query("DELETE FROM playlist_bhajans WHERE playlistId = :playlistId AND bhajanId = :bhajanId")
    suspend fun removeSongFromPlaylist(playlistId: Long, bhajanId: String)

    @Query("DELETE FROM playlist_bhajans WHERE bhajanId = :bhajanId")
    suspend fun removeBhajanFromAllPlaylists(bhajanId: String)

    @Query("""
        SELECT b.* FROM bhajans b
        INNER JOIN playlist_bhajans pb ON b.id = pb.bhajanId
        WHERE pb.playlistId = :playlistId
        ORDER BY pb.orderIndex ASC
    """)
    fun getSongsForPlaylist(playlistId: Long): Flow<List<BhajanEntity>>

    @Query("SELECT COUNT(*) FROM playlist_bhajans WHERE playlistId = :playlistId")
    fun getSongCountForPlaylist(playlistId: Long): Flow<Int>

    @Query("UPDATE playlist_bhajans SET orderIndex = :newIndex WHERE playlistId = :playlistId AND bhajanId = :bhajanId")
    suspend fun updateSongOrderInPlaylist(playlistId: Long, bhajanId: String, newIndex: Int)
}
