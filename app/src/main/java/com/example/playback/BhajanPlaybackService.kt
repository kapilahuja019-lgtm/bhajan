package com.example.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class BhajanPlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val restartButton by lazy {
        CommandButton.Builder()
            .setDisplayName("Start Over ⭐")
            .setIconResId(R.drawable.ic_star_restart)
            .setSessionCommand(SessionCommand(ACTION_RESTART_FROM_START, Bundle.EMPTY))
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Handles audio focus (pause on phone calls)
            .setHandleAudioBecomingNoisy(true) // Pauses playback when headphones unplugged or Bluetooth disconnects
            .setWakeMode(C.WAKE_MODE_LOCAL) // Keeps CPU awake for uninterrupted background playback
            .build()

        player = exoPlayer

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Custom notification provider with channel id
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .build()
        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .setCustomLayout(listOf(restartButton))
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bhajan Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows media controls for playing devotional bhajans"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // App was swiped away from Recents / task switcher.
        // As requested: audio keeps playing when the app is swiped away!
        // It should ONLY stop if player is paused or empty.
        val p = player
        if (p == null || !p.playWhenReady || p.playbackState == Player.STATE_ENDED || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        // Hardware button support (headset button, bluetooth controls)
        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent
        ): Boolean {
            val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            }

            if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                when (keyEvent.keyCode) {
                    KeyEvent.KEYCODE_HEADSETHOOK,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        val p = session.player
                        if (p.isPlaying) p.pause() else p.play()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        session.player.play()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        session.player.pause()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> {
                        session.player.seekToNextMediaItem()
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        session.player.seekToPreviousMediaItem()
                        return true
                    }
                }
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_RESTART_FROM_START, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SET_SPEED, Bundle.EMPTY))
                .add(SessionCommand(ACTION_SEEK_RELATIVE, Bundle.EMPTY))
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setCustomLayout(listOf(restartButton))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_RESTART_FROM_START -> {
                    session.player.seekTo(0L)
                    session.player.play()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_SET_SPEED -> {
                    val speed = args.getFloat("speed", 1.0f)
                    session.player.playbackParameters = PlaybackParameters(speed)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                ACTION_SEEK_RELATIVE -> {
                    val offsetMs = args.getLong("offsetMs", 0L)
                    val currentPos = session.player.currentPosition
                    val duration = session.player.duration
                    val targetPos = if (duration > 0) {
                        (currentPos + offsetMs).coerceIn(0L, duration)
                    } else {
                        (currentPos + offsetMs).coerceAtLeast(0L)
                    }
                    session.player.seekTo(targetPos)
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    companion object {
        const val CHANNEL_ID = "bhajan_playback_channel_v1"
        const val ACTION_RESTART_FROM_START = "com.example.bhajan.ACTION_RESTART_FROM_START"
        const val ACTION_SET_SPEED = "com.example.bhajan.ACTION_SET_SPEED"
        const val ACTION_SEEK_RELATIVE = "com.example.bhajan.ACTION_SEEK_RELATIVE"
    }
}
