package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.ReplayClip
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GameModeScreen
import com.example.ui.screens.ReplayLibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerDialog
import android.view.KeyEvent
import android.widget.Toast
import com.example.ui.theme.AppBackground
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueLight
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextTitle
import com.example.ui.viewmodel.ReplayViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ReplayViewModel by viewModels()
    private var lastVolumeDownTimestamp = 0L

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestRequiredPermissions()

        setContent {
            MyApplicationTheme {
                GameReplayApp(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.settings.value.floatingButtonEnabled && android.provider.Settings.canDrawOverlays(this)) {
            com.example.service.FloatingOverlayService.start(this)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val currentSettings = viewModel.settings.value
        if (currentSettings.volumeKeyShortcutEnabled && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val now = System.currentTimeMillis()
            if (now - lastVolumeDownTimestamp < 650) {
                // Double tap Volume Down detected!
                viewModel.triggerSaveReplay()
                Toast.makeText(this, "请稍等", Toast.LENGTH_SHORT).show()
                lastVolumeDownTimestamp = 0L
                return true
            }
            lastVolumeDownTimestamp = now
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

enum class NavigationTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    HOME("首页", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    LIBRARY("视频库", Icons.Filled.Movie, Icons.Outlined.Movie),
    GAMEMODE("游戏模式", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    SETTINGS("设置", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun GameReplayApp(viewModel: ReplayViewModel) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var activePreviewClip by remember { mutableStateOf<ReplayClip?>(null) }
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity

    androidx.activity.compose.BackHandler {
        if (currentTab != NavigationTab.HOME) {
            currentTab = NavigationTab.HOME
        } else {
            activity?.moveTaskToBack(true)
        }
    }

    val dashboardState by viewModel.dashboardState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val clips by viewModel.clips.collectAsState()
    val totalStorageUsed by viewModel.totalStorageUsed.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                // In Game Mode, automatically hide the bottom navigation bar to maximize gaming immersion
                if (!settings.isGameModeActive && activePreviewClip == null) {
                    androidx.compose.material3.NavigationBar(
                        containerColor = PureWhite,
                        contentColor = PrimaryBlue,
                        tonalElevation = 0.dp,
                        windowInsets = androidx.compose.material3.NavigationBarDefaults.windowInsets,
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawLine(
                                    color = BorderSubtle,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                    ) {
                        NavigationTab.entries.forEach { tab ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryBlue,
                                    selectedTextColor = PrimaryBlue,
                                    indicatorColor = PrimaryBlueLight,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
                    .statusBarsPadding()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                Crossfade(
                    targetState = currentTab,
                    label = "screen_crossfade"
                ) { tab ->
                    when (tab) {
                        NavigationTab.HOME -> {
                            DashboardScreen(
                                state = dashboardState,
                                settings = settings,
                                recentClips = clips,
                                onStartReplay = { resultCode, resultData ->
                                    viewModel.startReplayEngine(resultCode, resultData)
                                },
                                onStopReplay = {
                                    viewModel.stopReplayEngine()
                                },
                                onTriggerSave = { title ->
                                    viewModel.triggerSaveReplay(title)
                                },
                                onToggleFloatingOverlay = { enabled ->
                                    viewModel.toggleFloatingOverlay(enabled)
                                },
                                onToggleGameMode = { enabled ->
                                    viewModel.updateSettings(settings.copy(isGameModeActive = enabled))
                                },
                                onSelectGame = { game ->
                                    viewModel.updateSettings(settings.copy(currentGameTitle = game))
                                },
                                onLaunchGame = { game ->
                                    viewModel.launchGameAndSelect(game)
                                },
                                onNavigateToLibrary = { currentTab = NavigationTab.LIBRARY },
                                onNavigateToGameMode = { currentTab = NavigationTab.GAMEMODE },
                                onNavigateToSettings = { currentTab = NavigationTab.SETTINGS },
                                onUpdateSettings = { viewModel.updateSettings(it) }
                            )
                        }
                        NavigationTab.LIBRARY -> {
                            ReplayLibraryScreen(
                                clips = clips,
                                totalStorageUsed = totalStorageUsed,
                                onPlayClip = { clip ->
                                    activePreviewClip = clip
                                },
                                onDeleteClip = { clip ->
                                    viewModel.deleteClip(clip)
                                },
                                onUpdateTitle = { id, newTitle ->
                                    viewModel.updateClipTitle(id, newTitle)
                                },
                                onToggleFavorite = { clip ->
                                    viewModel.toggleFavorite(clip)
                                }
                            )
                        }
                        NavigationTab.GAMEMODE -> {
                            GameModeScreen(
                                settings = settings,
                                detectedGames = dashboardState.detectedGames,
                                onToggleGameMode = { enabled ->
                                    viewModel.updateSettings(settings.copy(isGameModeActive = enabled))
                                },
                                onSelectGame = { game ->
                                    viewModel.updateSettings(settings.copy(currentGameTitle = game))
                                },
                                onLaunchGame = { game ->
                                    viewModel.launchGameAndSelect(game)
                                },
                                onUpdateSettings = { newSettings ->
                                    viewModel.updateSettings(newSettings)
                                }
                            )
                        }
                        NavigationTab.SETTINGS -> {
                            SettingsScreen(
                                settings = settings,
                                totalStorageUsed = totalStorageUsed,
                                onUpdateSettings = { newSettings ->
                                    viewModel.updateSettings(newSettings)
                                },
                                onClearBufferCache = {
                                    viewModel.clearBufferCache()
                                }
                            )
                        }
                    }
                }
            }
        }

        // In-app Full-Window Video Preview & Fullscreen Cinema Player
        activePreviewClip?.let { clip ->
            VideoPlayerDialog(
                clip = clip,
                onDismiss = { activePreviewClip = null }
            )
        }
    }
}
