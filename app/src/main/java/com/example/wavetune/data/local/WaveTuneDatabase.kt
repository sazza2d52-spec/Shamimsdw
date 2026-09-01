package com.example.wavetune.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.wavetune.data.model.FavoriteEntity
import com.example.wavetune.data.model.HistoryEntity
import com.example.wavetune.data.model.LyricsEntity
import com.example.wavetune.data.model.Playlist
import com.example.wavetune.data.model.PlaylistSongCrossRef
import com.example.wavetune.data.model.VaultItemEntity

@Database(
    entities = [
        Playlist::class,
        PlaylistSongCrossRef::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        LyricsEntity::class,
        VaultItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WaveTuneDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun historyDao(): HistoryDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: WaveTuneDatabase? = null

        fun getDatabase(context: Context): WaveTuneDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WaveTuneDatabase::class.java,
                    "wavetune_music_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
