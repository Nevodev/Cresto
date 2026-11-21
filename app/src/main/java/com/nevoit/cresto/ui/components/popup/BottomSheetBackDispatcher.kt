package com.nevoit.cresto.ui.components.popup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

@Composable
internal fun BottomSheetBackScope(
    onBack: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val parentOwner = checkNotNull(
        LocalNavigationEventDispatcherOwner.current
            ?: LocalOnBackPressedDispatcherOwner.current as? NavigationEventDispatcherOwner
    )
    val parent = parentOwner.navigationEventDispatcher
    val owner = remember(parent) { BottomSheetBackDispatcher() }
    DisposableEffect(parent, owner) {
        owner.attach(parent)
        onDispose { owner.dispose() }
    }
    LifecycleStartEffect(owner, enabled) {
        owner.isEnabled = enabled
        onStopOrDispose { owner.isEnabled = false }
    }
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
        BackHandler(enabled = enabled, onBack = onBack)
        content()
    }
}

internal class BottomSheetBackDispatcher : NavigationEventDispatcherOwner {
    override val navigationEventDispatcher = NavigationEventDispatcher()
    private val input = object : NavigationEventInput() {
        fun back() = dispatchOnBackCompleted()
    }
    private val overlay = object : NavigationEventHandler<NavigationEventInfo>(
        initialInfo = NavigationEventInfo.None,
        isBackEnabled = false,
    ) {
        override fun onBackCompleted() = input.back()
    }

    init {
        navigationEventDispatcher.addInput(input)
    }

    var isEnabled: Boolean
        get() = overlay.isBackEnabled
        set(value) {
            overlay.isBackEnabled = value
        }

    fun attach(parent: NavigationEventDispatcher) {
        // NavigationHost subcomposes during layout and can register after a restored sheet.
        // Overlay priority protects the sheet; its own dispatcher preserves nested page ordering.
        parent.addHandler(overlay, NavigationEventDispatcher.PRIORITY_OVERLAY)
    }

    fun dispose() {
        overlay.remove()
        navigationEventDispatcher.dispose()
    }
}
