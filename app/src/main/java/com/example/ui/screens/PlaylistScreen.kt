package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.PlaylistEntity
import com.example.data.model.BhajanItem
import com.example.ui.components.BhajanThumbnailImage
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.LargeAccessibleButton
import com.example.ui.viewmodel.BhajanViewModel

@Composable
fun PlaylistScreen(
    viewModel: BhajanViewModel,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val playlistSongs by viewModel.selectedPlaylistSongs.collectAsState()
    val allBhajans by viewModel.allBhajans.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<PlaylistEntity?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showAddSongsDialog by remember { mutableStateOf(false) }

    // Intercept back navigation if inside playlist detail
    if (selectedPlaylist != null) {
        BackHandler {
            viewModel.selectedPlaylist.value = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("playlist_screen")
    ) {
        if (selectedPlaylist == null) {
            // Main Playlists Overview
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Big "Create New Playlist" Button
                LargeAccessibleButton(
                    text = stringResource(R.string.new_playlist),
                    icon = Icons.Default.Add,
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    testTag = "btn_create_playlist"
                )

                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty_playlist),
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                onOpen = { viewModel.selectedPlaylist.value = playlist },
                                onRename = { playlistToRename = playlist },
                                onDelete = { playlistToDelete = playlist }
                            )
                        }
                    }
                }
            }
        } else {
            // Detailed Playlist Song View
            val currentPl = selectedPlaylist!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header with Back Arrow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.selectedPlaylist.value = null },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("btn_back_to_playlists")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = currentPl.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Action buttons: Play All & Add Bhajans
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (playlistSongs.isNotEmpty()) {
                                viewModel.playAll(playlistSongs)
                            }
                        },
                        enabled = playlistSongs.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.btn_play_all), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { showAddSongsDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.add_to_playlist), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (playlistSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty_playlist),
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = playlistSongs,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            val title = com.example.util.SindhiTransliterator.resolveTitle(song, appLanguage)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.playBhajan(song, playlistSongs) }
                                    .testTag("playlist_song_${song.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    BhajanThumbnailImage(
                                        thumbnailPath = song.thumbnail,
                                        contentDescription = song.title,
                                        modifier = Modifier.size(60.dp, 80.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (song.artist.isNotBlank()) {
                                            Text(
                                                text = song.artist,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Up / Down Reorder Buttons
                                    Column {
                                        IconButton(
                                            onClick = { viewModel.movePlaylistSongUp(currentPl.id, index, playlistSongs) },
                                            enabled = index > 0,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Up",
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.movePlaylistSongDown(currentPl.id, index, playlistSongs) },
                                            enabled = index < playlistSongs.lastIndex,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Down",
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    // Remove Song Button
                                    IconButton(
                                        onClick = { viewModel.removeSongFromPlaylist(currentPl.id, song.id) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("btn_remove_song_${song.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = stringResource(R.string.remove_from_playlist),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(text = stringResource(R.string.create_playlist_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text(stringResource(R.string.playlist_name_hint), fontSize = 18.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_playlist_name")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName)
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Text(text = stringResource(R.string.save), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCreateDialog = false },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Text(text = stringResource(R.string.cancel), fontSize = 18.sp)
                }
            }
        )
    }

    // Rename Playlist Dialog
    playlistToRename?.let { pl ->
        var renameText by remember { mutableStateOf(pl.name) }
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            title = {
                Text(text = stringResource(R.string.rename_playlist), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_rename_playlist")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renamePlaylist(pl.id, renameText)
                            playlistToRename = null
                        }
                    },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Text(text = stringResource(R.string.save), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { playlistToRename = null },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Text(text = stringResource(R.string.cancel), fontSize = 18.sp)
                }
            }
        )
    }

    // Confirm Delete Playlist Dialog (MANDATORY per requirements)
    playlistToDelete?.let { pl ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_msg, pl.name),
            onConfirm = {
                viewModel.deletePlaylist(pl.id)
                playlistToDelete = null
            },
            onDismiss = { playlistToDelete = null }
        )
    }

    // Add Bhajans to Playlist Dialog
    if (showAddSongsDialog && selectedPlaylist != null) {
        val currentPlaylistId = selectedPlaylist!!.id
        AlertDialog(
            onDismissRequest = { showAddSongsDialog = false },
            title = {
                Text(text = stringResource(R.string.add_to_playlist), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allBhajans) { item ->
                        val alreadyInPlaylist = playlistSongs.any { it.id == item.id }
                        val title = com.example.util.SindhiTransliterator.resolveTitle(item, appLanguage)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (alreadyInPlaylist) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(8.dp)
                        ) {
                            BhajanThumbnailImage(
                                thumbnailPath = item.thumbnail,
                                contentDescription = item.title,
                                modifier = Modifier.size(45.dp, 60.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (alreadyInPlaylist) {
                                Text(
                                    text = "✓",
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            } else {
                                IconButton(
                                    onClick = { viewModel.addSongToPlaylist(currentPlaylistId, item.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAddSongsDialog = false },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Text(text = stringResource(R.string.ok), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: PlaylistEntity,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen)
            .testTag("playlist_card_${playlist.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tap to open and play",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Rename button
            IconButton(
                onClick = onRename,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("rename_playlist_${playlist.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.rename_playlist),
                    modifier = Modifier.size(26.dp)
                )
            }

            // Delete button (opens confirmation dialog!)
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("delete_playlist_${playlist.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_playlist),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
