package com.nevoit.cresto

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.nevoit.cresto.feature.guide.GuideActivity
import com.nevoit.cresto.feature.main.MainScreen
import com.nevoit.cresto.feature.screenextract.ScreenExtractEvents
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.feature.widget.TodayTodoWidget
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.GlasenseTheme
import com.nevoit.cresto.util.NotificationPermissionCompat
import com.nevoit.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.nevoit.glasense.theme.LocalGlasenseContentColor
import kotlinx.coroutines.launch

private const val REQUEST_POST_NOTIFICATIONS = 1001

/**
 * The main activity of the application.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This makes the app display behind the system bars.

        requestNotificationPermissionIfNeeded()
        handleScreenExtractErrorIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

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

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            TodayTodoWidget.updateAll(applicationContext)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (!NotificationPermissionCompat.shouldRequestPostNotificationsPermission()) {
            return
        }

        if (NotificationPermissionCompat.hasPostNotificationsPermission(this)) {
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(NotificationPermissionCompat.POST_NOTIFICATIONS),
            REQUEST_POST_NOTIFICATIONS
        )
    }
}
