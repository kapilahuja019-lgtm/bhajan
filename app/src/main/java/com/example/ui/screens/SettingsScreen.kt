package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BhajanItem
import com.example.ui.components.BhajanThumbnailImage
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.viewmodel.BhajanViewModel

@Composable
fun SettingsScreen(
    viewModel: BhajanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsState()
    val currentLanguage by viewModel.appLanguage.collectAsState()
    val autoplayNext by viewModel.autoplayNext.collectAsState()
    val allBhajans by viewModel.allBhajans.collectAsState()

    var showCustomSongsHelpDialog by remember { mutableStateOf(false) }
    var showManageBhajansDialog by remember { mutableStateOf(false) }
    var bhajanToDelete by remember { mutableStateOf<BhajanItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        // Section: Appearance & Sound
        Text(
            text = stringResource(R.string.section_appearance),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // 1. Language Chooser (Hinglish <-> Sindhi Devanagari)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.language_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isHinglish = currentLanguage == "hinglish"
                    val isSindhi = currentLanguage == "sindhi"

                    ChoiceButton(
                        text = stringResource(R.string.lang_hinglish),
                        isSelected = isHinglish,
                        onClick = { viewModel.setLanguage("hinglish") },
                        modifier = Modifier.weight(1f),
                        testTag = "lang_btn_hinglish"
                    )

                    ChoiceButton(
                        text = stringResource(R.string.lang_sindhi),
                        isSelected = isSindhi,
                        onClick = { viewModel.setLanguage("sindhi") },
                        modifier = Modifier.weight(1f),
                        testTag = "lang_btn_sindhi"
                    )
                }
            }
        }

        // 2. Theme Chooser (System / Light / Dark)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BrightnessMedium,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.theme_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChoiceButton(
                        text = "System",
                        isSelected = currentTheme == "SYSTEM",
                        onClick = { viewModel.setTheme("SYSTEM") },
                        modifier = Modifier.weight(1f),
                        testTag = "theme_btn_system"
                    )
                    ChoiceButton(
                        text = "Light ☀️",
                        isSelected = currentTheme == "LIGHT",
                        onClick = { viewModel.setTheme("LIGHT") },
                        modifier = Modifier.weight(1f),
                        testTag = "theme_btn_light"
                    )
                    ChoiceButton(
                        text = "Dark 🌙",
                        isSelected = currentTheme == "DARK",
                        onClick = { viewModel.setTheme("DARK") },
                        modifier = Modifier.weight(1f),
                        testTag = "theme_btn_dark"
                    )
                }
            }
        }

        // 3. Autoplay Next Toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.autoplay_title),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.autoplay_desc),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoplayNext,
                    onCheckedChange = { viewModel.setAutoplayNext(it) },
                    modifier = Modifier.testTag("switch_autoplay")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: Help & Storage
        Text(
            text = stringResource(R.string.section_help),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // 4. Background Play & Battery Settings Help Item
        SettingsClickableCard(
            title = stringResource(R.string.battery_help_title),
            subtitle = stringResource(R.string.battery_help_desc),
            icon = Icons.Default.BatteryChargingFull,
            onClick = {
                openBatterySettings(context)
            },
            testTag = "card_battery_help"
        )

        // 5. How to Add Custom Bhajans Guide
        SettingsClickableCard(
            title = stringResource(R.string.custom_songs_help_title),
            subtitle = stringResource(R.string.custom_songs_help_desc),
            icon = Icons.Default.HelpOutline,
            onClick = { showCustomSongsHelpDialog = true },
            testTag = "card_how_to_add_songs"
        )

        // 6. Safely Delete Bhajans from Settings
        SettingsClickableCard(
            title = stringResource(R.string.manage_bhajans_title),
            subtitle = stringResource(R.string.manage_bhajans_desc),
            icon = Icons.Default.DeleteOutline,
            onClick = { showManageBhajansDialog = true },
            testTag = "card_delete_bhajans_settings"
        )

        Spacer(modifier = Modifier.height(120.dp))
    }

    // Manage & Delete Bhajans Dialog
    if (showManageBhajansDialog) {
        AlertDialog(
            onDismissRequest = { showManageBhajansDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.manage_bhajans_dialog_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                if (allBhajans.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_bhajans_to_delete),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allBhajans, key = { it.id }) { item ->
                            val title = com.example.util.SindhiTransliterator.resolveTitle(item, currentLanguage)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.artist.isNotBlank()) {
                                        Text(
                                            text = item.artist,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { bhajanToDelete = item },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("btn_delete_bhajan_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showManageBhajansDialog = false },
                    modifier = Modifier.heightIn(min = 50.dp)
                ) {
                    Text(text = stringResource(R.string.close), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Confirmation dialog before deleting bhajan
    bhajanToDelete?.let { item ->
        val title = com.example.util.SindhiTransliterator.resolveTitle(item, currentLanguage)
        ConfirmDeleteDialog(
            title = stringResource(R.string.confirm_delete_bhajan_title),
            message = stringResource(R.string.confirm_delete_bhajan_msg, title),
            onConfirm = {
                viewModel.deleteBhajan(item)
                bhajanToDelete = null
            },
            onDismiss = { bhajanToDelete = null }
        )
    }

    // Help Dialog
    if (showCustomSongsHelpDialog) {
        AlertDialog(
            onDismissRequest = { showCustomSongsHelpDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.custom_songs_help_dialog_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.custom_songs_help_dialog_text),
                    fontSize = 18.sp,
                    lineHeight = 26.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCustomSongsHelpDialog = false },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Text(text = stringResource(R.string.ok), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ChoiceButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 54.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsClickableCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun openBatterySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }
}
