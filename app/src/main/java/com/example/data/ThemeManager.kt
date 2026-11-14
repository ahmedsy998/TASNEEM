package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.InterfaceBackgroundStyle
import com.example.ui.theme.TasneemOrange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tasneem_appearance_prefs")

class ThemeManager(private val context: Context) {

    companion object {
        private const val SYNC_PREFS_NAME = "tasneem_appearance_sync_prefs"
        private const val SYNC_KEY_THEME_MODE = "theme_mode"
        private const val SYNC_KEY_BG_STYLE = "interface_bg_style"
        private const val SYNC_KEY_COLOR_THEME = "app_color_theme"

        private val KEY_THEME_MODE = stringPreferencesKey(SYNC_KEY_THEME_MODE)
        private val KEY_BG_STYLE = stringPreferencesKey(SYNC_KEY_BG_STYLE)
        private val KEY_COLOR_THEME = stringPreferencesKey(SYNC_KEY_COLOR_THEME)
    }

    private val syncPrefs = context.getSharedPreferences(SYNC_PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeModeFlow = MutableStateFlow(getInitialThemeMode())
    private val _backgroundStyleFlow = MutableStateFlow(getInitialBackgroundStyle())
    private val _colorThemeFlow = MutableStateFlow(getInitialColorTheme())

    init {
        TasneemOrange = _colorThemeFlow.value.primaryAccent
    }

    val themeModeFlow: Flow<AppThemeMode> = _themeModeFlow.asStateFlow()

    val backgroundStyleFlow: Flow<InterfaceBackgroundStyle> = _backgroundStyleFlow.asStateFlow()

    val colorThemeFlow: Flow<AppColorTheme> = _colorThemeFlow.asStateFlow()

    fun getInitialThemeMode(): AppThemeMode {
        val id = syncPrefs.getString(SYNC_KEY_THEME_MODE, AppThemeMode.DARK.id) ?: AppThemeMode.DARK.id
        return AppThemeMode.fromId(id)
    }

    fun getInitialBackgroundStyle(): InterfaceBackgroundStyle {
        val id = syncPrefs.getString(SYNC_KEY_BG_STYLE, InterfaceBackgroundStyle.DEEP_CHARCOAL.id)
            ?: InterfaceBackgroundStyle.DEEP_CHARCOAL.id
        return InterfaceBackgroundStyle.fromId(id)
    }

    fun getInitialColorTheme(): AppColorTheme {
        val id = syncPrefs.getString(SYNC_KEY_COLOR_THEME, AppColorTheme.TASNEEM_ORANGE_SILVER.id)
            ?: AppColorTheme.TASNEEM_ORANGE_SILVER.id
        return AppColorTheme.fromId(id)
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        syncPrefs.edit().putString(SYNC_KEY_THEME_MODE, mode.id).commit()
        _themeModeFlow.value = mode
        try {
            context.dataStore.edit { prefs ->
                prefs[KEY_THEME_MODE] = mode.id
            }
        } catch (_: Exception) {
        }
    }

    suspend fun setBackgroundStyle(style: InterfaceBackgroundStyle) {
        syncPrefs.edit().putString(SYNC_KEY_BG_STYLE, style.id).commit()
        _backgroundStyleFlow.value = style
        try {
            context.dataStore.edit { prefs ->
                prefs[KEY_BG_STYLE] = style.id
            }
        } catch (_: Exception) {
        }
    }

    suspend fun setColorTheme(theme: AppColorTheme) {
        saveColorThemeSync(theme)
        try {
            context.dataStore.edit { prefs ->
                prefs[KEY_COLOR_THEME] = theme.id
            }
        } catch (_: Exception) {
        }
    }

    fun saveColorThemeSync(theme: AppColorTheme) {
        syncPrefs.edit().putString(SYNC_KEY_COLOR_THEME, theme.id).commit()
        TasneemOrange = theme.primaryAccent
        _colorThemeFlow.value = theme
    }
}
