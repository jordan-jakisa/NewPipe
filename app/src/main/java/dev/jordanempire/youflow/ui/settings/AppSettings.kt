package dev.jordanempire.youflow.ui.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** YouFlow's own UI preferences, stored beside the classic ones in the default SharedPreferences. */
class AppSettings private constructor(app: Application) {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(app)

    init {
        // One time: move installs that saved the old default (wallpaper colours) to the YouFlow palette.
        if (!prefs.contains(KEY_PALETTE_VERSION)) {
            prefs.edit().putBoolean(KEY_DYNAMIC, false).putInt(KEY_PALETTE_VERSION, 1).apply()
        }
    }

    private val _theme = MutableStateFlow(prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM)
    val theme: StateFlow<String> = _theme.asStateFlow()
    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, false))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()
    private val _autoplay = MutableStateFlow(prefs.getBoolean(KEY_AUTOPLAY, true))
    val autoplay: StateFlow<Boolean> = _autoplay.asStateFlow()

    fun setTheme(value: String) {
        prefs.edit().putString(KEY_THEME, value).apply()
        _theme.value = value
    }

    fun setDynamicColor(value: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC, value).apply()
        _dynamicColor.value = value
    }

    fun setAutoplay(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOPLAY, value).apply()
        _autoplay.value = value
    }

    companion object {
        const val KEY_THEME = "youflow_theme"
        const val KEY_DYNAMIC = "youflow_dynamic_color"
        const val KEY_AUTOPLAY = "youflow_autoplay"
        private const val KEY_PALETTE_VERSION = "youflow_palette_version"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_BLACK = "black"

        @Volatile
        private var instance: AppSettings? = null

        fun get(app: Application): AppSettings = instance ?: synchronized(this) { instance ?: AppSettings(app).also { instance = it } }
    }
}
