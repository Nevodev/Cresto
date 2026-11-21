package com.nevoit.cresto.navigation

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize

internal fun Modifier.navigationCallerReturnInput(
    onGestureActiveChanged: (Boolean) -> Unit,
): Modifier = then(NavigationCallerReturnInputElement(onGestureActiveChanged))

private data class NavigationCallerReturnInputElement(
    val onGestureActiveChanged: (Boolean) -> Unit,
) : ModifierNodeElement<NavigationCallerReturnInputNode>() {
    override fun create() = NavigationCallerReturnInputNode(onGestureActiveChanged)

    override fun update(node: NavigationCallerReturnInputNode) {
        node.onGestureActiveChanged = onGestureActiveChanged
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "navigationCallerReturnInput"
    }
}

private class NavigationCallerReturnInputNode(
    var onGestureActiveChanged: (Boolean) -> Unit,
) : Modifier.Node(), PointerInputModifierNode {
    private var gestureActive = false

    // Sharing must be on the page itself, which is the sibling of the underlay.
    override fun sharePointerInputWithSiblings(): Boolean = true

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass == PointerEventPass.Initial && !gestureActive && pointerEvent.changes.any { it.pressed }) {
            gestureActive = true
            onGestureActiveChanged(true)
        }
        if (pass == PointerEventPass.Final && pointerEvent.changes.none { it.pressed }) {
            // The original card handles click, long press and drag before the exit can release its hit path.
            onCancelPointerInput()
        }
    }

    override fun onCancelPointerInput() {
        if (gestureActive) {
            gestureActive = false
            onGestureActiveChanged(false)
        }
    }

    override fun onDetach() {
        onCancelPointerInput()
    }
}
