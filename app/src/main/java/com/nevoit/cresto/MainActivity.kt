package com.nevoit.cresto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.nevoit.cresto.feature.guide.GuideActivity
import com.nevoit.cresto.feature.main.MainScreen
import com.nevoit.cresto.feature.screenextract.ScreenExtractEvents
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.GlasenseTheme
import com.nevoit.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.nevoit.glasense.theme.LocalGlasenseContentColor

/**
 * The main activity of the application.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This makes the app display behind the system bars.

        requestNotificationPermissionIfNeeded()
        handleScreenExtractErrorIntent(intent)

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
                val overscrollFactory = rememberOffsetOverscrollFactory()

                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalGlasenseContentColor provides AppColors.content //provide content color
                ) {
                    MainScreen()
                }
            }
        }
        window.setBackgroundDrawable(null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleScreenExtractErrorIntent(intent)
    }

    private fun handleScreenExtractErrorIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(ScreenExtractEvents.EXTRA_SHOW_ERROR_DIALOG, false) != true) {
            return
        }
        val message = intent.getStringExtra(ScreenExtractEvents.EXTRA_ERROR_MESSAGE)
            ?: getString(R.string.extract_screen_failed)
        ScreenExtractEvents.emitError(message)
        intent.removeExtra(ScreenExtractEvents.EXTRA_SHOW_ERROR_DIALOG)
        intent.removeExtra(ScreenExtractEvents.EXTRA_ERROR_MESSAGE)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }
}
