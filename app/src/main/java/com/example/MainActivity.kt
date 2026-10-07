package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.player.FullScreenPlayer
import com.example.ui.player.MiniPlayer
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlaylistScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BhajanSangrahTheme
import com.example.ui.viewmodel.BhajanViewModel
import com.example.ui.viewmodel.BhajanViewModelFactory
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: BhajanViewModel by viewModels {
        BhajanViewModelFactory(application as BhajanApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val appLanguage by viewModel.appLanguage.collectAsState()

            // Dynamic instant language switching without restart
            val currentContext = LocalContext.current
            val localizedConfiguration = remember(appLanguage, currentContext) {
                val locale = if (appLanguage == "sindhi") Locale("sd") else Locale("en")
                Configuration(currentContext.resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }

            val localizedContext = remember(appLanguage, currentContext, localizedConfiguration) {
                val confContext = currentContext.createConfigurationContext(localizedConfiguration)
                object : ContextWrapper(currentContext) {
                    override fun getResources(): Resources = confContext.resources
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalActivityResultRegistryOwner provides this
            ) {
                BhajanSangrahTheme(themePreference = themeMode) {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}

enum class BottomTab {
    BHAJANS,
    PLAYLISTS,
    SETTINGS
}

@Composable
fun MainAppScaffold(viewModel: BhajanViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(BottomTab.BHAJANS) }
    val playbackState by viewModel.playbackState.collectAsState()
    val isFullScreenVisible by viewModel.isFullScreenPlayerVisible.collectAsState()

    // Polite notification permission request for Android 13+
    var showNotificationPermissionDialog by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            } else false
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        showNotificationPermissionDialog = false
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Mini Player directly above Bottom Navigation
                AnimatedVisibility(
                    visible = playbackState.currentBhajan != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    MiniPlayer(viewModel = viewModel)
                }

                // Strict 3-Tab Bottom Navigation Bar (min 64dp touch area)
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .testTag("bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == BottomTab.BHAJANS,
                        onClick = { selectedTab = BottomTab.BHAJANS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = stringResource(R.string.tab_bhajans),
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.tab_bhajans),
                                fontSize = 16.sp,
                                fontWeight = if (selectedTab == BottomTab.BHAJANS) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("nav_tab_bhajans")
                    )

                    NavigationBarItem(
                        selected = selectedTab == BottomTab.PLAYLISTS,
                        onClick = { selectedTab = BottomTab.PLAYLISTS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = stringResource(R.string.tab_playlists),
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.tab_playlists),
                                fontSize = 16.sp,
                                fontWeight = if (selectedTab == BottomTab.PLAYLISTS) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("nav_tab_playlists")
                    )

                    NavigationBarItem(
                        selected = selectedTab == BottomTab.SETTINGS,
                        onClick = { selectedTab = BottomTab.SETTINGS },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.tab_settings),
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.tab_settings),
                                fontSize = 16.sp,
                                fontWeight = if (selectedTab == BottomTab.SETTINGS) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("nav_tab_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                BottomTab.BHAJANS -> HomeScreen(viewModel = viewModel)
                BottomTab.PLAYLISTS -> PlaylistScreen(viewModel = viewModel)
                BottomTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    // Full-Screen Player Dialog
    if (isFullScreenVisible && playbackState.currentBhajan != null) {
        FullScreenPlayer(
            viewModel = viewModel,
            onDismiss = { viewModel.closeFullScreenPlayer() }
        )
    }

    // Polite Notification Permission Dialog for Android 13+
    if (showNotificationPermissionDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.permission_notification_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.permission_notification_msg),
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Text(
                        text = stringResource(R.string.permission_allow),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showNotificationPermissionDialog = false },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Text(
                        text = stringResource(R.string.permission_later),
                        fontSize = 18.sp
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
