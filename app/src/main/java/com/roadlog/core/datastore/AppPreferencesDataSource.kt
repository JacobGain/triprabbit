package com.roadlog.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.roadlog.core.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("roadlog_settings")

@Singleton
class AppPreferencesDataSource @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.dataStore
    val settings: Flow<AppSettings> = store.data.map { values ->
        AppSettings(
            selectedVehicleId = values[SELECTED_VEHICLE],
            firstLaunchComplete = values[FIRST_LAUNCH] ?: false,
            themeMode = values[THEME_MODE].enumOr(com.roadlog.core.model.ThemeMode.SYSTEM),
            accentTheme = values[ACCENT_THEME].enumOr(com.roadlog.core.model.AccentTheme.DEFAULT),
            useDynamicColor = values[DYNAMIC_COLOR] ?: true,
            useAmoledBlack = values[AMOLED] ?: false,
            displayDensity = values[DENSITY].enumOr(com.roadlog.core.model.DisplayDensity.COMFORTABLE),
            showVehicleDetails = values[SHOW_DETAILS] ?: true,
            confirmReadingDeletion = values[CONFIRM_READING_DELETE] ?: true,
        )
    }

    suspend fun setSelectedVehicle(id: Long?) { store.edit { values ->
        if (id == null) values.remove(SELECTED_VEHICLE) else values[SELECTED_VEHICLE] = id
    } }

    suspend fun setFirstLaunchComplete(complete: Boolean) { store.edit { it[FIRST_LAUNCH] = complete } }
    suspend fun updateAppearance(
        themeMode: com.roadlog.core.model.ThemeMode? = null,
        accentTheme: com.roadlog.core.model.AccentTheme? = null,
        dynamicColor: Boolean? = null,
        amoled: Boolean? = null,
        density: com.roadlog.core.model.DisplayDensity? = null,
    ) { store.edit { values ->
        themeMode?.let { values[THEME_MODE] = it.name }
        accentTheme?.let { values[ACCENT_THEME] = it.name }
        dynamicColor?.let { values[DYNAMIC_COLOR] = it }
        amoled?.let { values[AMOLED] = it }
        density?.let { values[DENSITY] = it.name }
    } }
    suspend fun setConfirmReadingDeletion(enabled: Boolean) { store.edit { it[CONFIRM_READING_DELETE] = enabled } }

    private companion object {
        val SELECTED_VEHICLE = longPreferencesKey("selected_vehicle_id")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch_complete")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AMOLED = booleanPreferencesKey("amoled_black")
        val DENSITY = stringPreferencesKey("display_density")
        val SHOW_DETAILS = booleanPreferencesKey("show_vehicle_details")
        val CONFIRM_READING_DELETE = booleanPreferencesKey("confirm_reading_deletion")
    }
}

private inline fun <reified T : Enum<T>> String?.enumOr(default: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
