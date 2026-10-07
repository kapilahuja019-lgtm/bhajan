package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("bhajan_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME, "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString(KEY_LANG, "hinglish") ?: "hinglish")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _autoplayNext = MutableStateFlow(prefs.getBoolean(KEY_AUTOPLAY, true))
    val autoplayNext: StateFlow<Boolean> = _autoplayNext.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(prefs.getFloat(KEY_SPEED, 1.0f))
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(prefs.getInt(KEY_REPEAT, 0)) // 0: OFF, 1: ONE, 2: ALL
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(prefs.getBoolean(KEY_SHUFFLE, false))
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _lastBhajanId = MutableStateFlow(prefs.getString(KEY_LAST_BHAJAN_ID, null))
    val lastBhajanId: StateFlow<String?> = _lastBhajanId.asStateFlow()

    private val _lastPositionMs = MutableStateFlow(prefs.getLong(KEY_LAST_POS_MS, 0L))
    val lastPositionMs: StateFlow<Long> = _lastPositionMs.asStateFlow()

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME, mode).apply()
        _themeMode.value = mode
    }

    fun setAppLanguage(lang: String) {
        prefs.edit().putString(KEY_LANG, lang).apply()
        _appLanguage.value = lang
    }

    fun setAutoplayNext(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOPLAY, enabled).apply()
        _autoplayNext.value = enabled
    }

    fun setPlaybackSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_SPEED, speed).apply()
        _playbackSpeed.value = speed
    }

    fun setRepeatMode(mode: Int) {
        prefs.edit().putInt(KEY_REPEAT, mode).apply()
        _repeatMode.value = mode
    }

    fun setShuffle(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE, enabled).apply()
        _isShuffle.value = enabled
    }

    fun saveLastPlayback(bhajanId: String, positionMs: Long) {
        prefs.edit()
            .putString(KEY_LAST_BHAJAN_ID, bhajanId)
            .putLong(KEY_LAST_POS_MS, positionMs)
            .apply()
        _lastBhajanId.value = bhajanId
        _lastPositionMs.value = positionMs
    }

    companion object {
        private const val KEY_THEME = "pref_theme"
        private const val KEY_LANG = "pref_lang"
        private const val KEY_AUTOPLAY = "pref_autoplay"
        private const val KEY_SPEED = "pref_speed"
        private const val KEY_REPEAT = "pref_repeat"
        private const val KEY_SHUFFLE = "pref_shuffle"
        private const val KEY_LAST_BHAJAN_ID = "pref_last_bhajan_id"
        private const val KEY_LAST_POS_MS = "pref_last_pos_ms"
    }
}
