package com.nevoit.cresto.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.GlasenseTheme
import com.nevoit.glasense.overscroll.rememberOffsetOverscrollFactory

enum class SettingsDestination(val value: String) {
    APPEARANCE("appearance"),
    AI("ai"),
    DATA_STORAGE("data_storage"),
    GENERAL("general"),
    ABOUT("about");

    companion object {
        fun fromValue(value: String?): SettingsDestination {
            return entries.firstOrNull { it.value == value } ?: GENERAL
        }
    }
}

class SettingsActivity : ComponentActivity() {

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

        val destination = SettingsDestination.fromValue(
            intent.getStringExtra(EXTRA_SETTINGS_DESTINATION)
        )

        setContent {
            GlasenseTheme {
                val overscrollFactory = rememberOffsetOverscrollFactory(Orientation.Vertical)
                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalContentColor provides AppColors.content
                ) {
                    when (destination) {
                        SettingsDestination.APPEARANCE -> AppearanceScreen()
                        SettingsDestination.AI -> AIScreen()
                        SettingsDestination.DATA_STORAGE -> DataStorageScreen()
                        SettingsDestination.GENERAL -> GeneralScreen()
                        SettingsDestination.ABOUT -> AboutScreen()
                    }
                }
            }
        }
        window.setBackgroundDrawable(null)
    }
}

