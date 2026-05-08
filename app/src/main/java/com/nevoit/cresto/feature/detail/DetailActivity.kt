package com.nevoit.cresto.feature.detail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import com.nevoit.cresto.data.todo.EXTRA_TODO_ID
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.theme.AppColors
import com.nevoit.cresto.theme.GlasenseTheme
import com.nevoit.glasense.overscroll.rememberOffsetOverscrollFactory
import org.koin.androidx.viewmodel.ext.android.viewModel

class DetailActivity : ComponentActivity() {

    private val todoViewModel: TodoViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val todoId = intent.getIntExtra(EXTRA_TODO_ID, -1)

        setContent {
            GlasenseTheme {
                val overscrollFactory = rememberOffsetOverscrollFactory(
                    orientation = Orientation.Vertical
                )
                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalContentColor provides AppColors.content
                ) {
                    DetailScreen(todoId = todoId, viewModel = todoViewModel)
                }
            }
        }
        window.setBackgroundDrawable(null)
    }
}