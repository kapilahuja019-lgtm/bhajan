package com.example.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.components.BhajanThumbnailImage
import com.example.ui.components.formatTime
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HeartRed
import com.example.ui.viewmodel.BhajanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayer(
    viewModel: BhajanViewModel,
    onDismiss: () -> Unit
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val bhajan = playbackState.currentBhajan ?: run {
        onDismiss()
        return
    }

    val displayTitle = com.example.util.SindhiTransliterator.resolveTitle(bhajan, appLanguage)

    var showSleepTimerDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("full_screen_player"),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar with Close Arrow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("btn_close_player")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.close),
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = stringResource(R.string.now_playing),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Favorite Button in Top Bar
                    IconButton(
                        onClick = { viewModel.toggleFavorite(bhajan) },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("player_favorite_toggle")
                    ) {
                        Icon(
                            imageVector = if (bhajan.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (bhajan.isFavorite) HeartRed else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // ⭐ Restart from Beginning Button in Top Bar
                    IconButton(
                        onClick = { viewModel.restartFromBeginning() },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("player_restart_from_start")
                    ) {
                        Text("⭐", fontSize = 28.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Big Thumbnail in strict 3:4 portrait ratio
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                ) {
                    BhajanThumbnailImage(
                        thumbnailPath = bhajan.thumbnail,
                        contentDescription = bhajan.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title and Artist
                Text(
                    text = displayTitle,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )

                if (bhajan.artist.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = bhajan.artist,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Seek Bar Slider & Timestamps
                Column(modifier = Modifier.fillMaxWidth()) {
                    var isUserSeeking by remember { mutableStateOf(false) }
                    var seekSliderValue by remember { mutableFloatStateOf(0f) }

                    val currentPos = if (isUserSeeking) {
                        seekSliderValue.toLong()
                    } else {
                        playbackState.currentPositionMs
                    }
                    val totalDuration = playbackState.durationMs.coerceAtLeast(1L)

                    Slider(
                        value = (currentPos.toFloat()).coerceIn(0f, totalDuration.toFloat()),
                        onValueChange = {
                            isUserSeeking = true
                            seekSliderValue = it
                        },
                        onValueChangeFinished = {
                            isUserSeeking = false
                            viewModel.seekTo(seekSliderValue.toLong())
                        },
                        valueRange = 0f..totalDuration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_seek_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPos),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTime(playbackState.durationMs),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Playback Controls (+/-10s, Previous, Huge Play/Pause, Next)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = { viewModel.seekRelative(-10) },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("btn_rewind_10")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = stringResource(R.string.rewind_10),
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Previous Track
                    IconButton(
                        onClick = { viewModel.previous() },
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("btn_previous")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = stringResource(R.string.previous),
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // HUGE Play/Pause Button (80dp x 80dp for elderly touch ease)
                    Surface(
                        onClick = { viewModel.togglePlayPause() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(80.dp)
                            .testTag("player_play_pause")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    // Next Track
                    IconButton(
                        onClick = { viewModel.next() },
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("btn_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = stringResource(R.string.next),
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { viewModel.seekRelative(10) },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("btn_forward_10")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = stringResource(R.string.forward_10),
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // ⭐ Big Start Over from Beginning Button
                OutlinedButton(
                    onClick = { viewModel.restartFromBeginning() },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(top = 10.dp)
                        .heightIn(min = 52.dp)
                        .testTag("btn_big_star_restart"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "⭐ " + stringResource(R.string.restart_bhajan),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Secondary Controls Row (Loop, Shuffle, Sleep Timer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Loop Mode Chip
                    val loopText = when (playbackState.repeatMode) {
                        1 -> stringResource(R.string.loop_one)
                        2 -> stringResource(R.string.loop_all)
                        else -> stringResource(R.string.loop_off)
                    }
                    val loopIcon = when (playbackState.repeatMode) {
                        1 -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    FilterChip(
                        selected = playbackState.repeatMode != 0,
                        onClick = {
                            val nextMode = (playbackState.repeatMode + 1) % 3
                            viewModel.setRepeatMode(nextMode)
                        },
                        label = { Text(loopText, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(imageVector = loopIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("chip_repeat_mode")
                    )

                    // Shuffle Chip
                    FilterChip(
                        selected = playbackState.isShuffle,
                        onClick = { viewModel.toggleShuffle() },
                        label = { Text(stringResource(R.string.btn_shuffle), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("chip_shuffle_mode")
                    )

                    // Sleep Timer Chip
                    val timerMinutesRemaining = playbackState.sleepTimerRemainingSeconds / 60
                    val timerLabel = if (playbackState.sleepTimerRemainingSeconds > 0) {
                        "${timerMinutesRemaining}m"
                    } else {
                        stringResource(R.string.sleep_timer)
                    }
                    FilterChip(
                        selected = playbackState.sleepTimerRemainingSeconds > 0,
                        onClick = { showSleepTimerDialog = true },
                        label = { Text(timerLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("chip_sleep_timer")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Playback Speed Chips (0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.speed),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        speeds.forEach { speed ->
                            val isSelected = (playbackState.playbackSpeed == speed)
                            Surface(
                                onClick = { viewModel.setSpeed(speed) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("speed_chip_$speed"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${speed}x",
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Sleep Timer Selection Dialog
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.sleep_timer),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val timerOptions = listOf(
                        0 to stringResource(R.string.timer_off),
                        15 to stringResource(R.string.timer_15m),
                        30 to stringResource(R.string.timer_30m),
                        60 to stringResource(R.string.timer_60m)
                    )

                    timerOptions.forEach { (mins, label) ->
                        Button(
                            onClick = {
                                viewModel.setSleepTimer(mins)
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .testTag("timer_option_$mins"),
                            shape = RoundedCornerShape(14.dp),
                            colors = if (mins == 0) {
                                ButtonDefaults.outlinedButtonColors()
                            } else {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            }
                        ) {
                            Text(text = label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { showSleepTimerDialog = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(text = stringResource(R.string.cancel), fontSize = 18.sp)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
