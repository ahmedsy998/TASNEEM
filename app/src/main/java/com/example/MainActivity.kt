package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppearanceBottomSheet
import com.example.ui.components.ReciterSelectorBottomSheet
import com.example.ui.components.SurahSelectorBottomSheet
import com.example.ui.components.TasneemBackgroundContainer
import com.example.ui.components.TasneemTopBar
import com.example.ui.screens.TasneemHomeScreen
import com.example.ui.screens.TasneemStudioScreen
import com.example.ui.theme.TasneemTheme
import com.example.viewmodel.TasneemScreen
import com.example.viewmodel.TasneemViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TasneemApp()
        }
    }
}

@Composable
fun TasneemApp(
    viewModel: TasneemViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val backgroundStyle by viewModel.backgroundStyle.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackSyncState by viewModel.playbackSyncState.collectAsStateWithLifecycle()
    val previewReciterId by viewModel.previewReciterId.collectAsStateWithLifecycle()

    TasneemTheme(
        themeMode = themeMode,
        backgroundStyle = backgroundStyle
    ) {
        TasneemBackgroundContainer(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Single Clean Top Bar: Tasneem Logo, Title ("تسنيم"), and Appearance Button ("المظهر")
                TasneemTopBar(
                    onAppearanceClick = { viewModel.setAppearanceModalVisible(true) },
                    onLogoClick = {
                        if (uiState.currentScreen != TasneemScreen.HOME) {
                            viewModel.navigateToHome()
                        }
                    }
                )

                // Screen Content (No bottom navigation bar, no login, no clutter)
                when (uiState.currentScreen) {
                    TasneemScreen.HOME -> {
                        TasneemHomeScreen(
                            uiState = uiState,
                            onStartCreateVideo = viewModel::navigateToStudio,
                            onQuickOpenSurahSelector = { viewModel.setSurahSelectorVisible(true) },
                            onQuickOpenReciterSelector = { viewModel.setReciterSelectorVisible(true) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    TasneemScreen.STUDIO, TasneemScreen.FULLSCREEN_PREVIEW -> {
                        TasneemStudioScreen(
                            uiState = uiState,
                            playbackSyncState = playbackSyncState,
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Modals & BottomSheets
            if (uiState.showAppearanceModal) {
                AppearanceBottomSheet(
                    currentThemeMode = themeMode,
                    currentBackgroundStyle = backgroundStyle,
                    onSelectThemeMode = viewModel::updateThemeMode,
                    onSelectBackgroundStyle = viewModel::updateBackgroundStyle,
                    onDismiss = { viewModel.setAppearanceModalVisible(false) }
                )
            }

            if (uiState.showSurahSelectorModal) {
                SurahSelectorBottomSheet(
                    allSurahs = viewModel.quranRepository.allSurahs,
                    selectedSurah = uiState.selectedSurah,
                    onSelectSurah = viewModel::selectSurah,
                    onDismiss = { viewModel.setSurahSelectorVisible(false) }
                )
            }

            if (uiState.showReciterSelectorModal) {
                ReciterSelectorBottomSheet(
                    allReciters = viewModel.reciterRepository.allReciters,
                    selectedReciter = uiState.selectedReciter,
                    previewingReciterId = previewReciterId,
                    onSelectReciter = viewModel::selectReciter,
                    onPreviewReciter = viewModel::toggleReciterSamplePreview,
                    onDismiss = { viewModel.setReciterSelectorVisible(false) }
                )
            }
        }
    }
}
