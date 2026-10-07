package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BhajanApp
import com.example.data.db.PlaylistEntity
import com.example.data.model.BhajanItem
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.BhajanRepository
import com.example.playback.PlaybackManager
import com.example.playback.PlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BhajanViewModel(
    private val repository: BhajanRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val isFavoriteFilter = MutableStateFlow(false)
    val isReorderMode = MutableStateFlow(false)

    val playbackState: StateFlow<PlaybackUiState> = playbackManager.uiState

    val themeMode: StateFlow<String> = preferencesRepository.themeMode
    val appLanguage: StateFlow<String> = preferencesRepository.appLanguage
    val autoplayNext: StateFlow<Boolean> = preferencesRepository.autoplayNext

    val allBhajans: StateFlow<List<BhajanItem>> = repository.getAllBhajans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteBhajans: StateFlow<List<BhajanItem>> = repository.getFavoriteBhajans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredBhajans: StateFlow<List<BhajanItem>> = combine(
        allBhajans,
        searchQuery,
        isFavoriteFilter
    ) { bhajans, query, favOnly ->
        var list = if (favOnly) bhajans.filter { it.isFavorite } else bhajans
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.titleSindhi.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)

    val selectedPlaylistSongs: StateFlow<List<BhajanItem>> = selectedPlaylist
        .flatMapLatest { playlist ->
            if (playlist != null) repository.getSongsForPlaylist(playlist.id) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isFullScreenPlayerVisible = MutableStateFlow(false)
    val isFullScreenPlayerVisible: StateFlow<Boolean> = _isFullScreenPlayerVisible.asStateFlow()

    init {
        restoreLastPlaybackState()
    }

    private fun restoreLastPlaybackState() {
        viewModelScope.launch {
            val lastId = preferencesRepository.lastBhajanId.value
            val lastPos = preferencesRepository.lastPositionMs.value
            if (!lastId.isNullOrEmpty() && playbackState.value.currentBhajan == null) {
                val item = repository.getBhajanById(lastId)
                if (item != null) {
                    // Preload state in idle/paused mode
                    // Next tap on play will immediately start here
                }
            }
        }
    }

    fun openFullScreenPlayer() {
        _isFullScreenPlayerVisible.value = true
    }

    fun closeFullScreenPlayer() {
        _isFullScreenPlayerVisible.value = false
    }

    fun playBhajan(bhajan: BhajanItem, playlist: List<BhajanItem>) {
        val index = playlist.indexOfFirst { it.id == bhajan.id }.let { if (it == -1) 0 else it }
        playbackManager.playBhajanList(playlist, index, 0L)
        _isFullScreenPlayerVisible.value = true
    }

    fun playAll(playlist: List<BhajanItem>, shuffle: Boolean = false) {
        if (playlist.isEmpty()) return
        if (shuffle) {
            playbackManager.toggleShuffle()
            val randomIndex = playlist.indices.random()
            playbackManager.playBhajanList(playlist, randomIndex, 0L)
        } else {
            playbackManager.playBhajanList(playlist, 0, 0L)
        }
        _isFullScreenPlayerVisible.value = true
    }

    fun deleteBhajan(bhajan: BhajanItem) {
        viewModelScope.launch {
            if (playbackState.value.currentBhajan?.id == bhajan.id) {
                playbackManager.pause()
            }
            repository.deleteBhajan(bhajan.id)
        }
    }

    fun toggleFavorite(bhajan: BhajanItem) {
        viewModelScope.launch {
            repository.toggleFavorite(bhajan.id, !bhajan.isFavorite)
        }
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun restartFromBeginning() = playbackManager.restartFromBeginning()
    fun next() = playbackManager.next()
    fun previous() = playbackManager.previous()
    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)
    fun seekRelative(seconds: Int) = playbackManager.seekRelative(seconds)
    fun setSpeed(speed: Float) = playbackManager.setSpeed(speed)
    fun setRepeatMode(mode: Int) = playbackManager.setRepeatMode(mode)
    fun toggleShuffle() = playbackManager.toggleShuffle()
    fun setSleepTimer(minutes: Int) = playbackManager.setSleepTimer(minutes)

    fun moveBhajanUp(index: Int, currentList: List<BhajanItem>) {
        viewModelScope.launch {
            repository.moveBhajanUp(index, currentList)
        }
    }

    fun moveBhajanDown(index: Int, currentList: List<BhajanItem>) {
        viewModelScope.launch {
            repository.moveBhajanDown(index, currentList)
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name.trim())
        }
    }

    fun renamePlaylist(id: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renamePlaylist(id, newName.trim())
            if (selectedPlaylist.value?.id == id) {
                selectedPlaylist.value = selectedPlaylist.value?.copy(name = newName.trim())
            }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
            if (selectedPlaylist.value?.id == id) {
                selectedPlaylist.value = null
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, bhajanId: String) {
        viewModelScope.launch {
            val count = selectedPlaylistSongs.value.size
            repository.addSongToPlaylist(playlistId, bhajanId, count)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, bhajanId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, bhajanId)
        }
    }

    fun movePlaylistSongUp(playlistId: Long, index: Int, songs: List<BhajanItem>) {
        if (index > 0) {
            viewModelScope.launch {
                repository.reorderPlaylistSongs(playlistId, index, index - 1, songs)
            }
        }
    }

    fun movePlaylistSongDown(playlistId: Long, index: Int, songs: List<BhajanItem>) {
        if (index < songs.lastIndex) {
            viewModelScope.launch {
                repository.reorderPlaylistSongs(playlistId, index, index + 1, songs)
            }
        }
    }

    fun addCustomBhajan(
        context: android.content.Context,
        title: String,
        titleSindhi: String,
        artist: String,
        audioUri: android.net.Uri,
        thumbnailUri: android.net.Uri?,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val audioDir = java.io.File(context.filesDir, "user_audio").apply { mkdirs() }
                val imageDir = java.io.File(context.filesDir, "user_images").apply { mkdirs() }

                val audioFile = java.io.File(audioDir, "bhajan_${System.currentTimeMillis()}.mp3")
                context.contentResolver.openInputStream(audioUri)?.use { input ->
                    audioFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val savedThumbnailPath: String = if (thumbnailUri != null) {
                    val imgFile = java.io.File(imageDir, "thumb_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(thumbnailUri)?.use { input ->
                        imgFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    imgFile.absolutePath
                } else {
                    "images/om_jai_jagdish.png"
                }

                repository.addCustomBhajan(
                    title = title,
                    titleSindhi = titleSindhi,
                    artist = artist,
                    audioFile = audioFile.absolutePath,
                    thumbnail = savedThumbnailPath
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun setTheme(theme: String) = preferencesRepository.setThemeMode(theme)
    fun setLanguage(lang: String) = preferencesRepository.setAppLanguage(lang)
    fun setAutoplayNext(enabled: Boolean) = preferencesRepository.setAutoplayNext(enabled)
}

class BhajanViewModelFactory(
    private val app: BhajanApp
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BhajanViewModel(
            app.bhajanRepository,
            app.preferencesRepository,
            app.playbackManager
        ) as T
    }
}
