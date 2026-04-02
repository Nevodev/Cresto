package com.nevoit.cresto.ui.screens.guidescreen

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
import com.nevoit.cresto.MainActivity
import com.nevoit.cresto.ui.screens.settings.util.SettingsManager
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.GlasenseTheme
import com.nevoit.glasense.overscroll.rememberOffsetOverscrollFactory

class GuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            GlasenseTheme {
                val overscrollFactory = rememberOffsetOverscrollFactory(Orientation.Vertical)

                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalContentColor provides AppColors.content,
                ) {
                    GuideScreen(onFinish = {
                        SettingsManager.isFirstRun = false

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)

                        finish()
                    })
                }
            }
        }
        window.setBackgroundDrawable(null)
    }
}