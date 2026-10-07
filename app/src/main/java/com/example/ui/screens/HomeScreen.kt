package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.BhajanItem
import com.example.ui.components.BhajanThumbnailImage
import com.example.ui.components.LargeAccessibleButton
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HeartRed
import com.example.ui.theme.SaffronPrimary
import com.example.ui.viewmodel.BhajanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BhajanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bhajans by viewModel.filteredBhajans.collectAsState()
    val allBhajans by viewModel.allBhajans.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val isFavOnly by viewModel.isFavoriteFilter.collectAsState()
    val isReorderMode by viewModel.isReorderMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    var showAddBhajanDialog by remember { mutableStateOf(false) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.searchQuery.value = spoken
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen")
    ) {
        // Top Header: App Title & Instant Language Shift Toggle (Hinglish <-> سنڌي)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Direct 1-tap Language Switch
            Surface(
                onClick = {
                    val nextLang = if (appLanguage == "sindhi") "hinglish" else "sindhi"
                    viewModel.setLanguage(nextLang)
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .testTag("btn_language_shift_toggle")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (appLanguage == "sindhi") "سنڌي ➔ Hinglish" else "Hinglish ➔ سنڌي",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Top Search Bar with Microphone button
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .heightIn(min = 60.dp)
                .testTag("search_field"),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_hint),
                    fontSize = 18.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.searchQuery.value = "" },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("btn_clear_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, if (appLanguage == "sindhi") "ڀڄن جو نالو ڳالهايو…" else "Speak bhajan name…")
                            }
                            try {
                                speechRecognizerLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Voice search not available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_voice_search_mic")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Search Mic",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Filter & Action Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !isFavOnly,
                onClick = { viewModel.isFavoriteFilter.value = false },
                label = {
                    Text(
                        text = stringResource(R.string.chip_all),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("chip_all")
            )

            FilterChip(
                selected = isFavOnly,
                onClick = { viewModel.isFavoriteFilter.value = true },
                label = {
                    Text(
                        text = stringResource(R.string.chip_favorites),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = HeartRed,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("chip_favorites")
            )

            Spacer(modifier = Modifier.weight(1f))

            // Reorder Toggle Button
            FilledTonalButton(
                onClick = { viewModel.isReorderMode.value = !isReorderMode },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (isReorderMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_reorder_toggle")
            ) {
                Icon(
                    imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.SwapVert,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isReorderMode) stringResource(R.string.btn_done_reorder) else stringResource(R.string.btn_reorder),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Play All & Shuffle Buttons Header
        if (!isReorderMode && bhajans.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.playAll(bhajans, shuffle = false) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("btn_play_all"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_play_all),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.playAll(bhajans, shuffle = true) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("btn_shuffle_all"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_shuffle),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Big Add Bhajan Button
            Button(
                onClick = { showAddBhajanDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .heightIn(min = 52.dp)
                    .testTag("btn_add_bhajan"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_add_bhajan),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2-Column Grid of Bhajan Cards
        if (bhajans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = if (isFavOnly) {
                            stringResource(R.string.empty_favorites)
                        } else {
                            "No bhajans in library yet.\nTap below to add your first MP3!"
                        },
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isFavOnly) {
                        Button(
                            onClick = { showAddBhajanDialog = true },
                            modifier = Modifier
                                .heightIn(min = 54.dp)
                                .testTag("btn_add_first_bhajan"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.btn_add_bhajan), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("bhajan_grid")
            ) {
                itemsIndexed(
                    items = bhajans,
                    key = { _, item -> item.id }
                ) { index, bhajan ->
                    val isCurrent = playbackState.currentBhajan?.id == bhajan.id
                    val isPlaying = isCurrent && playbackState.isPlaying

                    BhajanCard(
                        bhajan = bhajan,
                        isCurrentPlaying = isPlaying,
                        isCurrentSelected = isCurrent,
                        isReorderMode = isReorderMode,
                        appLanguage = appLanguage,
                        onCardClick = {
                            viewModel.playBhajan(bhajan, bhajans)
                        },
                        onFavoriteClick = {
                            viewModel.toggleFavorite(bhajan)
                        },
                        onMoveUp = {
                            viewModel.moveBhajanUp(index, bhajans)
                        },
                        onMoveDown = {
                            viewModel.moveBhajanDown(index, bhajans)
                        },
                        canMoveUp = index > 0,
                        canMoveDown = index < bhajans.lastIndex
                    )
                }
            }
        }
    }

    if (showAddBhajanDialog) {
        AddBhajanDialog(
            onDismiss = { showAddBhajanDialog = false },
            onSave = { title, titleSindhi, artist, audioUri, thumbUri ->
                viewModel.addCustomBhajan(context, title, titleSindhi, artist, audioUri, thumbUri) { success ->
                    if (success) {
                        Toast.makeText(context, "Bhajan added! भजन शामिल हो गया!", Toast.LENGTH_SHORT).show()
                        showAddBhajanDialog = false
                    } else {
                        Toast.makeText(context, "Could not save audio", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun BhajanCard(
    bhajan: BhajanItem,
    isCurrentPlaying: Boolean,
    isCurrentSelected: Boolean,
    isReorderMode: Boolean,
    appLanguage: String,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    modifier: Modifier = Modifier
) {
    val displayTitle = com.example.util.SindhiTransliterator.resolveTitle(bhajan, appLanguage)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onCardClick)
            .testTag("bhajan_card_${bhajan.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isCurrentPlaying) 3.dp else 1.5.dp,
            color = if (isCurrentPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentPlaying) 6.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Thumbnail in strict 3:4 portrait ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                BhajanThumbnailImage(
                    thumbnailPath = bhajan.thumbnail,
                    contentDescription = bhajan.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Playing Indicator Badge
                if (isCurrentPlaying) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.now_playing),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Big Favorite Button in Top-Right (min 48dp target)
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(48.dp)
                        .clickable(onClick = onFavoriteClick)
                        .testTag("fav_btn_${bhajan.id}"),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (bhajan.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (bhajan.isFavorite) HeartRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Bhajan Title (min 18-20sp bold for grandmother)
            Text(
                text = displayTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            if (bhajan.artist.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bhajan.artist,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Big Up/Down Arrow Reorder Buttons
            AnimatedVisibility(visible = isReorderMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = if (canMoveUp) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("move_up_${bhajan.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = stringResource(R.string.move_up),
                            modifier = Modifier.size(28.dp),
                            tint = if (canMoveUp) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = if (canMoveDown) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("move_down_${bhajan.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = stringResource(R.string.move_down),
                            modifier = Modifier.size(28.dp),
                            tint = if (canMoveDown) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddBhajanDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, titleSindhi: String, artist: String, audioUri: Uri, thumbUri: Uri?) -> Unit
) {
    val context = LocalContext.current
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var audioFileName by remember { mutableStateOf("") }
    var thumbnailUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var titleSindhi by remember { mutableStateOf("") }
    var isSindhiManuallyEdited by remember { mutableStateOf(false) }
    var artist by remember { mutableStateOf("") }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            audioUri = uri
            val name = getFileName(context, uri)
            audioFileName = name
            if (title.isBlank()) {
                val cleanName = name.substringBeforeLast(".")
                title = cleanName
                titleSindhi = com.example.util.SindhiTransliterator.toSindhiArabic(cleanName)
                isSindhiManuallyEdited = false
            }
        }
    }

    val imageFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            thumbnailUri = uri
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            thumbnailUri = uri
        }
    }

    fun pickImageSafely() {
        try {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e: Exception) {
            try {
                imageFallbackLauncher.launch("image/*")
            } catch (ex: Exception) {
                Toast.makeText(context, "Could not open image picker", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_bhajan_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Audio Picker Button
                Button(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .testTag("btn_pick_audio"),
                    shape = RoundedCornerShape(14.dp),
                    colors = if (audioUri != null) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    }
                ) {
                    Icon(
                        imageVector = if (audioUri != null) Icons.Default.CheckCircle else Icons.Default.Audiotrack,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (audioUri != null) stringResource(R.string.audio_selected_fmt, audioFileName) else stringResource(R.string.select_audio_file),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Thumbnail Photo Picker with 3:4 Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(75.dp, 100.dp) // 3:4 ratio preview
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { pickImageSafely() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumbnailUri != null) {
                            AsyncImage(
                                model = thumbnailUri,
                                contentDescription = "Thumbnail Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "3:4 Photo",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { pickImageSafely() },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("btn_pick_photo"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (thumbnailUri != null) stringResource(R.string.thumbnail_selected) else stringResource(R.string.select_thumbnail),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Title Input (English / Hinglish)
                OutlinedTextField(
                    value = title,
                    onValueChange = { newTitle ->
                        title = newTitle
                        if (!isSindhiManuallyEdited) {
                            titleSindhi = com.example.util.SindhiTransliterator.toSindhiArabic(newTitle)
                        }
                    },
                    label = { Text(stringResource(R.string.bhajan_name_label), fontSize = 16.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bhajan_title")
                )

                // Sindhi Title Input with Auto-Convert Button
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سنڌي نالو (Sindhi)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FilledTonalButton(
                            onClick = {
                                titleSindhi = com.example.util.SindhiTransliterator.toSindhiArabic(title)
                                isSindhiManuallyEdited = false
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Text(
                                text = "✨ Convert سنڌي",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = titleSindhi,
                        onValueChange = {
                            titleSindhi = it
                            isSindhiManuallyEdited = true
                        },
                        placeholder = { Text("مثلاً: هري رام هري ڪرشن", fontSize = 16.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_bhajan_title_sindhi")
                    )
                }

                // Artist Input
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(stringResource(R.string.artist_label), fontSize = 16.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bhajan_artist")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (audioUri == null) {
                        Toast.makeText(context, context.getString(R.string.please_select_audio), Toast.LENGTH_SHORT).show()
                    } else if (title.isBlank()) {
                        Toast.makeText(context, "Please enter a title", Toast.LENGTH_SHORT).show()
                    } else {
                        val finalSindhi = if (titleSindhi.isNotBlank()) titleSindhi.trim() else com.example.util.SindhiTransliterator.toSindhiArabic(title.trim())
                        onSave(title.trim(), finalSindhi, artist.trim(), audioUri!!, thumbnailUri)
                    }
                },
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .testTag("btn_save_bhajan")
            ) {
                Text(text = stringResource(R.string.save), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 52.dp)
            ) {
                Text(text = stringResource(R.string.cancel), fontSize = 18.sp)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

private fun getFileName(context: Context, uri: Uri): String {
    var result = ""
    if (uri.scheme == "content") {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) result = it.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    if (result.isBlank()) {
        result = uri.path?.substringAfterLast('/') ?: "bhajan.mp3"
    }
    return result
}
