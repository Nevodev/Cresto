package com.nevoit.cresto.feature.guide

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import com.nevoit.cresto.MainActivity
import com.nevoit.cresto.feature.settings.util.SettingsManager
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.GlasenseTheme
import com.nevoit.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.nevoit.glasense.theme.LocalGlasenseContentColor

class GuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            GlasenseTheme {
                val overscrollFactory = rememberOffsetOverscrollFactory()

                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalGlasenseContentColor provides AppColors.content,
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
