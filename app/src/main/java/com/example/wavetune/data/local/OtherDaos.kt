package com.example.wavetune.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.wavetune.data.model.FavoriteEntity
import com.example.wavetune.data.model.HistoryEntity
import com.example.wavetune.data.model.LyricsEntity
import com.example.wavetune.data.model.VaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE type = 'SONG'")
    fun getFavoriteSongs(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE type = 'ALBUM'")
    fun getFavoriteAlbums(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE type = 'ARTIST'")
    fun getFavoriteArtists(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE `key` = :key)")
    fun isFavorite(key: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE `key` = :key")
    suspend fun deleteFavorite(key: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: HistoryEntity)

    @Query("SELECT songId, count(*) as count FROM playback_history GROUP BY songId ORDER BY count DESC LIMIT :limit")
    suspend fun getMostPlayedSongIds(limit: Int = 50): List<SongPlayCount>

    @Query("SELECT COUNT(*) FROM playback_history")
    fun getTotalPlaysCount(): Flow<Int>

    @Query("SELECT SUM(listenedSeconds) FROM playback_history")
    fun getTotalListeningTime(): Flow<Long?>

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
}

data class SongPlayCount(
    val songId: Long,
    val count: Int
)

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId LIMIT 1")
    fun getLyricsForSong(songId: Long): Flow<LyricsEntity?>

    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId LIMIT 1")
    suspend fun getLyricsForSongOnce(songId: Long): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLyrics(entity: LyricsEntity)

    @Query("DELETE FROM lyrics_cache WHERE songId = :songId")
    suspend fun deleteLyrics(songId: Long)
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items")
    fun getAllVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT itemId FROM vault_items WHERE itemType = 'SONG'")
    fun getVaultSongIds(): Flow<List<Long>>

    @Query("SELECT itemId FROM vault_items WHERE itemType = 'PLAYLIST'")
    fun getVaultPlaylistIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToVault(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE itemKey = :key")
    suspend fun removeFromVault(key: String)
}
