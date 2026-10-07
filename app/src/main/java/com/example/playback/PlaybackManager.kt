package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.data.model.BhajanItem
import com.example.data.preferences.UserPreferencesRepository
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val currentBhajan: BhajanItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: Int = 0, // 0 = OFF, 1 = ONE, 2 = ALL
    val isShuffle: Boolean = false,
    val sleepTimerRemainingSeconds: Int = 0 // 0 = off
)

@OptIn(UnstableApi::class)
class PlaybackManager(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var currentPlaylist: List<BhajanItem> = emptyList()
    private var currentIndex: Int = -1

    private var positionTrackerJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        initController()
        loadSavedPreferences()
    }

    private fun loadSavedPreferences() {
        _uiState.value = _uiState.value.copy(
            playbackSpeed = preferencesRepository.playbackSpeed.value,
            repeatMode = preferencesRepository.repeatMode.value,
            isShuffle = preferencesRepository.isShuffle.value
        )
    }

    private fun initController() {
        val sessionToken = SessionToken(context, ComponentName(context, BhajanPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future

        future.addListener({
            try {
                val mediaController = future.get()
                controller = mediaController
                mediaController.playbackParameters = PlaybackParameters(_uiState.value.playbackSpeed)
                applyRepeatModeToPlayer(_uiState.value.repeatMode)

                mediaController.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                        if (isPlaying) {
                            startPositionTracker()
                        } else {
                            stopPositionTracker()
                            saveCurrentPosition()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val isBuffering = playbackState == Player.STATE_BUFFERING
                        _uiState.value = _uiState.value.copy(
                            isBuffering = isBuffering,
                            durationMs = mediaController.duration.coerceAtLeast(0L)
                        )

                        if (playbackState == Player.STATE_ENDED) {
                            handleTrackEnded()
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateCurrentItemFromPlayer()
                    }
                })

                updateCurrentItemFromPlayer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun updateCurrentItemFromPlayer() {
        val mediaController = controller ?: return
        val currentMediaItem = mediaController.currentMediaItem
        val duration = mediaController.duration.coerceAtLeast(0L)
        val pos = mediaController.currentPosition.coerceAtLeast(0L)

        val id = currentMediaItem?.mediaId
        val bhajan = currentPlaylist.find { it.id == id }

        _uiState.value = _uiState.value.copy(
            currentBhajan = bhajan ?: _uiState.value.currentBhajan,
            isPlaying = mediaController.isPlaying,
            durationMs = duration,
            currentPositionMs = pos
        )
    }

    fun playBhajanList(list: List<BhajanItem>, startIndex: Int = 0, initialPositionMs: Long = 0L) {
        if (list.isEmpty()) return
        currentPlaylist = list
        currentIndex = startIndex.coerceIn(0, list.lastIndex)
        val target = list[currentIndex]

        playInternal(target, initialPositionMs)
    }

    private fun playInternal(bhajan: BhajanItem, startPositionMs: Long = 0L) {
        val mediaController = controller ?: return

        val artworkUri = when {
            bhajan.thumbnail.startsWith("images/") -> Uri.parse("file:///android_asset/${bhajan.thumbnail}")
            bhajan.thumbnail.startsWith("content://") || bhajan.thumbnail.startsWith("file://") -> Uri.parse(bhajan.thumbnail)
            bhajan.thumbnail.isNotBlank() -> Uri.fromFile(java.io.File(bhajan.thumbnail))
            else -> null
        }

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(bhajan.title)
            .setArtist(bhajan.artist)
            .apply {
                if (artworkUri != null) {
                    setArtworkUri(artworkUri)
                }
            }
            .build()

        val audioUri = when {
            bhajan.audioFile.startsWith("audio/") -> Uri.parse("asset:///${bhajan.audioFile}")
            bhajan.audioFile.startsWith("content://") || bhajan.audioFile.startsWith("file://") -> Uri.parse(bhajan.audioFile)
            else -> Uri.fromFile(java.io.File(bhajan.audioFile))
        }
        val mediaItem = MediaItem.Builder()
            .setMediaId(bhajan.id)
            .setUri(audioUri)
            .setMediaMetadata(mediaMetadata)
            .build()

        mediaController.setMediaItem(mediaItem)
        if (startPositionMs > 0) {
            mediaController.seekTo(startPositionMs)
        }
        mediaController.prepare()
        mediaController.play()

        _uiState.value = _uiState.value.copy(
            currentBhajan = bhajan,
            isPlaying = true,
            currentPositionMs = startPositionMs
        )

        preferencesRepository.saveLastPlayback(bhajan.id, startPositionMs)
    }

    fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) {
            mediaController.pause()
        } else {
            if (mediaController.mediaItemCount == 0 && _uiState.value.currentBhajan != null) {
                val current = _uiState.value.currentBhajan!!
                playInternal(current, _uiState.value.currentPositionMs)
            } else {
                mediaController.play()
            }
        }
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun seekTo(positionMs: Long) {
        val mediaController = controller ?: return
        mediaController.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
        saveCurrentPosition()
    }

    fun seekRelative(offsetSeconds: Int) {
        val mediaController = controller ?: return
        val currentPos = mediaController.currentPosition
        val duration = mediaController.duration
        val targetPos = if (duration > 0) {
            (currentPos + offsetSeconds * 1000L).coerceIn(0L, duration)
        } else {
            (currentPos + offsetSeconds * 1000L).coerceAtLeast(0L)
        }
        mediaController.seekTo(targetPos)
        _uiState.value = _uiState.value.copy(currentPositionMs = targetPos)
    }

    fun restartFromBeginning() {
        val mediaController = controller ?: return
        mediaController.seekTo(0L)
        mediaController.play()
        _uiState.value = _uiState.value.copy(
            currentPositionMs = 0L,
            isPlaying = true
        )
    }

    fun next() {
        if (currentPlaylist.isEmpty()) return
        if (_uiState.value.isShuffle) {
            currentIndex = (currentPlaylist.indices).random()
        } else {
            currentIndex = (currentIndex + 1) % currentPlaylist.size
        }
        playInternal(currentPlaylist[currentIndex], 0L)
    }

    fun previous() {
        if (currentPlaylist.isEmpty()) return
        val mediaController = controller
        if (mediaController != null && mediaController.currentPosition > 3000L) {
            // If already played more than 3s, restart current bhajan
            mediaController.seekTo(0L)
            return
        }

        if (_uiState.value.isShuffle) {
            currentIndex = (currentPlaylist.indices).random()
        } else {
            currentIndex = if (currentIndex - 1 < 0) currentPlaylist.lastIndex else currentIndex - 1
        }
        playInternal(currentPlaylist[currentIndex], 0L)
    }

    fun setSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
        preferencesRepository.setPlaybackSpeed(speed)
        controller?.playbackParameters = PlaybackParameters(speed)
    }

    fun setRepeatMode(mode: Int) {
        // 0 = OFF, 1 = ONE, 2 = ALL
        _uiState.value = _uiState.value.copy(repeatMode = mode)
        preferencesRepository.setRepeatMode(mode)
        applyRepeatModeToPlayer(mode)
    }

    private fun applyRepeatModeToPlayer(mode: Int) {
        val mediaController = controller ?: return
        when (mode) {
            1 -> mediaController.repeatMode = Player.REPEAT_MODE_ONE
            2 -> mediaController.repeatMode = Player.REPEAT_MODE_ALL
            else -> mediaController.repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_uiState.value.isShuffle
        _uiState.value = _uiState.value.copy(isShuffle = newShuffle)
        preferencesRepository.setShuffle(newShuffle)
    }

    private fun handleTrackEnded() {
        val repeatMode = _uiState.value.repeatMode
        if (repeatMode == 1) {
            // Repeat one
            controller?.seekTo(0L)
            controller?.play()
            return
        }

        val autoplayNext = preferencesRepository.autoplayNext.value
        if (autoplayNext) {
            if (repeatMode == 2 || currentIndex < currentPlaylist.lastIndex) {
                next()
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.value = _uiState.value.copy(sleepTimerRemainingSeconds = 0)
            return
        }

        val totalSeconds = minutes * 60
        _uiState.value = _uiState.value.copy(sleepTimerRemainingSeconds = totalSeconds)

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (isActive && remaining > 0) {
                delay(1000)
                remaining--
                _uiState.value = _uiState.value.copy(sleepTimerRemainingSeconds = remaining)
            }
            if (remaining <= 0) {
                controller?.pause()
                _uiState.value = _uiState.value.copy(sleepTimerRemainingSeconds = 0)
            }
        }
    }

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = scope.launch {
            while (isActive) {
                val mediaController = controller
                if (mediaController != null && mediaController.isPlaying) {
                    val pos = mediaController.currentPosition.coerceAtLeast(0L)
                    val dur = mediaController.duration.coerceAtLeast(0L)
                    _uiState.value = _uiState.value.copy(
                        currentPositionMs = pos,
                        durationMs = dur
                    )
                }
                delay(300)
            }
        }
    }

    private fun stopPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = null
    }

    private fun saveCurrentPosition() {
        val bhajan = _uiState.value.currentBhajan ?: return
        val pos = _uiState.value.currentPositionMs
        preferencesRepository.saveLastPlayback(bhajan.id, pos)
    }

    fun release() {
        saveCurrentPosition()
        stopPositionTracker()
        sleepTimerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }
}
