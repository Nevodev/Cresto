package com.nevoit.cresto

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
import com.nevoit.cresto.ui.screens.guidescreen.GuideActivity
import com.nevoit.cresto.ui.screens.main.MainScreen
import com.nevoit.cresto.ui.screens.settings.util.SettingsManager
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.GlasenseTheme
import com.nevoit.cresto.ui.theme.glasense.isAppInDarkTheme
import com.nevoit.glasense.overscroll.rememberOffsetOverscrollFactory
import com.nevoit.glasense.theme.LocalGlasenseIsDarkTheme

/**
 * The main activity of the application.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This makes the app display behind the system bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val isFirstRun = SettingsManager.isFirstRun

        if (isFirstRun) {
            startActivity(Intent(this, GuideActivity::class.java))
            finish()
            return
        }

        setContent {
            GlasenseTheme {
                val overscrollFactory = rememberOffsetOverscrollFactory(Orientation.Vertical)

                // Provide the custom overscroll factory to the composable tree.
                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalContentColor provides AppColors.content,
                    LocalGlasenseIsDarkTheme provides isAppInDarkTheme()
                ) {
                    // Display the main screen of the application.
                    MainScreen()
                }
            }
        }
        window.setBackgroundDrawable(null)
    }
}