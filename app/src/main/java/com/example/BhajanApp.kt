package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.BhajanRepository
import com.example.playback.PlaybackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BhajanApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var bhajanRepository: BhajanRepository
        private set

    lateinit var playbackManager: PlaybackManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        preferencesRepository = UserPreferencesRepository(this)
        bhajanRepository = BhajanRepository(this, database)
        playbackManager = PlaybackManager(this, preferencesRepository)

        applicationScope.launch {
            bhajanRepository.initializeIfEmpty()
        }
    }

    companion object {
        lateinit var instance: BhajanApp
            private set
    }
}
