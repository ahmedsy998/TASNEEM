package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.InterfaceBackgroundStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tasneem_appearance_prefs")

class ThemeManager(private val context: Context) {

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_BG_STYLE = stringPreferencesKey("interface_bg_style")
    }

    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        AppThemeMode.fromId(prefs[KEY_THEME_MODE] ?: AppThemeMode.DARK.id)
    }

    val backgroundStyleFlow: Flow<InterfaceBackgroundStyle> = context.dataStore.data.map { prefs ->
        InterfaceBackgroundStyle.fromId(prefs[KEY_BG_STYLE] ?: InterfaceBackgroundStyle.DEEP_CHARCOAL.id)
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.id
        }
    }

    suspend fun setBackgroundStyle(style: InterfaceBackgroundStyle) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BG_STYLE] = style.id
        }
    }
}
