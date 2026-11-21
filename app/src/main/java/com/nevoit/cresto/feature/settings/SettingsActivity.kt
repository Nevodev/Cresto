package com.nevoit.cresto.feature.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.nevoit.cresto.feature.widget.TodayTodoWidget
import com.nevoit.cresto.navigation.AppDestination
import com.nevoit.cresto.navigation.AppNavigationHost
import com.nevoit.cresto.theme.GlasenseTheme
import com.nevoit.glasense.core.utility.clearBackground
import kotlinx.coroutines.launch

enum class SettingsDestination(val value: String) {
    SETTINGS("settings"),
    APPEARANCE("appearance"),
    AI("ai"),
    DATA_STORAGE("data_storage"),
    GENERAL("general"),
    ABOUT("about"),
    CREDITS("credits");

    companion object {
        fun fromValue(value: String?): SettingsDestination {
            return entries.firstOrNull { it.value == value } ?: SETTINGS
        }
    }
}

class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_SETTINGS_DESTINATION = "settings_destination"

        fun createIntent(context: Context, destination: SettingsDestination): Intent {
            return Intent(context, SettingsActivity::class.java)
                .putExtra(EXTRA_SETTINGS_DESTINATION, destination.value)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        val destination = SettingsDestination.fromValue(
            intent.getStringExtra(EXTRA_SETTINGS_DESTINATION)
        )

        setContent {
            GlasenseTheme {
                AppNavigationHost(root = AppDestination.Settings(destination))
            }
        }
        clearBackground()
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            TodayTodoWidget.updateAll(applicationContext)
        }
    }
}
