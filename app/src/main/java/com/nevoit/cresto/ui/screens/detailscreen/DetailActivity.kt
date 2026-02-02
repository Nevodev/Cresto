package com.nevoit.cresto.ui.screens.detailscreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowCompat
import com.nevoit.cresto.CrestoApplication
import com.nevoit.cresto.data.todo.TodoViewModel
import com.nevoit.cresto.data.todo.TodoViewModelFactory
import com.nevoit.cresto.toolkit.overscroll.OffsetOverscrollFactory
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.GlasenseTheme

class DetailActivity : ComponentActivity() {

    private val todoViewModel: TodoViewModel by viewModels {
        TodoViewModelFactory((application as CrestoApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val todoId = intent.getIntExtra("todo_id", -1)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            GlasenseTheme {
                val animationScope = rememberCoroutineScope()
                val overscrollFactory = remember {
                    OffsetOverscrollFactory(
                        orientation = Orientation.Vertical,
                        animationScope = animationScope,
                    )
                }
                CompositionLocalProvider(
                    LocalOverscrollFactory provides overscrollFactory,
                    LocalContentColor provides contentColorFor(AppColors.pageBackground),
                ) {
                    DetailScreen(todoId = todoId, viewModel = todoViewModel)
                }
            }
        }
        window.setBackgroundDrawable(null)
    }
}