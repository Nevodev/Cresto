package com.nevoit.cresto.ui.components.popup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy as LayerCompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.RoundedRectangularShape
import com.kyant.shapes.UnevenRoundedRectangle
import com.nevoit.cresto.navigation.clip.cornerMaskClip
import com.nevoit.glasense.theme.tokens.Springs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Creates the caller state a [PopupTransition.FromAnchor] popup needs.
 *
 * Attach it with [Modifier.popupCaller]: the caller records itself into a layer, stops drawing
 * while the popup owns it, and draws the tail of the close animation back in its own place.
 */
@Composable
fun rememberPopupCaller(
    shape: RoundedRectangularShape,
    containerColor: ColorProducer? = null,
): PopupCaller {
    val layer = rememberOffscreenGraphicsLayer()
    return remember(layer, shape, containerColor) { PopupCaller(layer, shape, containerColor) }
}

@Composable
private fun rememberOffscreenGraphicsLayer(): GraphicsLayer {
    val layer = rememberGraphicsLayer()
    return remember(layer) {
        layer.apply { compositingStrategy = LayerCompositingStrategy.Offscreen }
    }
}

class PopupCaller internal constructor(
    internal val layer: GraphicsLayer,
    internal val shape: RoundedRectangularShape,
    internal val containerColor: ColorProducer?,
) {
    internal val coordinates = mutableStateOf<LayoutCoordinates?>(null)
    internal var returning by mutableStateOf<PopupReturn?>(null)
    internal var active by mutableStateOf(false)

    /** Bumped by [open]; the popup re-expands from wherever it currently is. */
    internal var openCount by mutableStateOf(0)

    /**
     * Asks the popup that owns this caller to open, including while it is still returning.
     * Call it together with whatever makes the popup visible, before the popup is composed.
     */
    fun open() {
        openCount++
    }
}

internal class PopupReturn {
    var transform: Matrix? by mutableStateOf(null)
    var origin = Offset.Zero
    var draw: (DrawScope.() -> Unit)? by mutableStateOf(null)
}

fun Modifier.popupCaller(caller: PopupCaller): Modifier = this
    .onGloballyPositioned { caller.coordinates.value = it }
    .drawWithContent {
        drawContent()
        if (caller.active) {
            caller.returning?.takeIf { it.transform != null }?.draw?.invoke(this)
        } else {
            drawLayer(caller.layer)
        }
    }
    // Return frames invalidate only the replay above; live caller content invalidates this recording.
    .graphicsLayer()
    .drawWithContent {
        caller.layer.record { this@drawWithContent.drawContent() }
    }

/** Where a popup grows from. */
sealed interface PopupTransition {
    /** Slides in from below; overflow is how far the content hangs past the container bottom. */
    data class FromBottom(val overflow: Dp = 32.dp) : PopupTransition

    /**
     * Unfolds out of a recorded caller, then hands the tail of the close animation back to it.
     * See [PopupMode] for the two resting behaviours. [handoffDistance] is measured in caller
     * shorter sides, same as the bottom popup menu.
     */
    data class FromAnchor(
        val caller: PopupCaller,
        val mode: PopupMode = PopupMode.Modal,
        val shape: RoundedRectangularShape = RoundedRectangle(28.dp),
        val containerColor: ColorProducer? = null,
        val margin: Dp = 12.dp,
        val offset: DpOffset = DpOffset.Zero,
        val minWidth: Dp = PopupMenuDefaults.MinWidth,
        val maxWidth: Dp = PopupMenuDefaults.Width,
        /**
         * Menu mode docks to the bottom by default. True anchors it to the caller's nearest side
         * edge; [expandUpwards] selects whether their top or bottom edges align.
         */
        val anchoredToCaller: Boolean = false,
        /** Anchored panels align their bottom edge to the caller and expand above it. */
        val expandUpwards: Boolean = false,
        val handoffDistance: Float = 1f,
    ) : PopupTransition
}

/**
 * How an anchored popup behaves once open. Both modes share the reveal, the shadow and the return
 * handoff; only the resting size, placement and scrim differ.
 */
enum class PopupMode {
    /** Sits on the caller's edge, sized by its content, with the scrim behind it. */
    Modal,

    /** Clamped width and no scrim; placement follows [PopupTransition.FromAnchor.anchoredToCaller]. */
    Menu,
}

/**
 * Generic popup layer: scrim, enter/exit, outside tap and back. [PopupTransition.FromAnchor]
 * mirrors the bottom popup menu: one translation spring, one reveal spring and one content fade drive
 * the caller's recording into the panel, and the close returns through the caller's own layer.
 * [scrimAlpha] is ignored by [PopupMode.Menu], which never dims the page.
 */
@Composable
fun PopupLayer(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    transition: PopupTransition = PopupTransition.FromBottom(),
    scrimColor: Color = Color.Black,
    scrimAlpha: Float = .2f,
    onDismissRequest: (dismiss: () -> Unit) -> Unit = { dismiss -> dismiss() },
    content: @Composable BoxScope.(dismiss: () -> Unit) -> Unit,
) {
    when (transition) {
        is PopupTransition.FromBottom -> FromBottomLayer(
            onDismissed, modifier, transition, scrimColor, scrimAlpha, onDismissRequest, content,
        )

        is PopupTransition.FromAnchor -> FromAnchorLayer(
            onDismissed, modifier, transition, scrimColor, scrimAlpha, onDismissRequest, content,
        )
    }
}

@Composable
private fun FromBottomLayer(
    onDismissed: () -> Unit,
    modifier: Modifier,
    transition: PopupTransition.FromBottom,
    scrimColor: Color,
    scrimAlpha: Float,
    onDismissRequest: (dismiss: () -> Unit) -> Unit,
    content: @Composable BoxScope.(dismiss: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var ready by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }

    LaunchedEffect(ready) {
        if (ready && !dismissing) {
            visible = true
            offset.animateTo(1f, Springs.snappy(400))
        }
    }

    fun dismiss() {
        if (dismissing) return
        dismissing = true
        visible = false
        scope.launch {
            offset.animateTo(0f, tween(150, easing = CubicBezierEasing(.4f, 0f, 1f, 1f)))
            onDismissed()
        }
    }

    val scrim by animateFloatAsState(if (visible) scrimAlpha else 0f, tween(200))
    BottomSheetBackScope(onBack = { onDismissRequest(::dismiss) }) {
        Box(modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer()
                    .drawBehind { drawRect(scrimColor, alpha = scrim.coerceIn(0f, 1f)) }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onDismissRequest(::dismiss) },
                    )
            )
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .layout { measurable, constraints ->
                        val placeable =
                            measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(
                                x = 0,
                                y = constraints.maxHeight - placeable.height +
                                        transition.overflow.roundToPx(),
                            )
                            if (!ready) ready = true
                        }
                    }
                    .graphicsLayer { translationY = (1f - offset.value) * size.height }
            ) {
                content(::dismiss)
            }
        }
    }
}

@Composable
private fun FromAnchorLayer(
    onDismissed: () -> Unit,
    modifier: Modifier,
    transition: PopupTransition.FromAnchor,
    scrimColor: Color,
    scrimAlpha: Float,
    onDismissRequest: (dismiss: () -> Unit) -> Unit,
    content: @Composable BoxScope.(dismiss: () -> Unit) -> Unit,
) {
    val caller = transition.caller
    val menuMode = transition.mode == PopupMode.Menu
    val restingScrimAlpha = if (menuMode) 0f else scrimAlpha
    val translationProgress = remember { Animatable(0f) }
    val scaleProgress = remember { Animatable(0f) }
    val opacity = remember { Animatable(0f) }
    val panelLayer = rememberOffscreenGraphicsLayer()
    var panelSize by remember { mutableStateOf(IntSize.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var hostCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var callerBounds by remember { mutableStateOf(Rect.Zero) }
    var expanded by remember { mutableStateOf(true) }
    val returning = remember { PopupReturn() }
    val latestDismiss = rememberUpdatedState(onDismissRequest)
    val density = LocalDensity.current
    // Same stretched nine-patch mask as the menus.
    val layoutDirection = LocalLayoutDirection.current
    // Separate from caller.layer: scaling the caller's own recording would resize the caller.
    val callerLayer = key(caller.layer, density, layoutDirection) { rememberGraphicsLayer() }
    val shadow = remember(transition.shape, layoutDirection) {
        PopupMenuShadow(transition.shape, layoutDirection)
    }
    val offsetPx = with(density) {
        Offset(transition.offset.x.toPx(), transition.offset.y.toPx())
    }

    fun liveCallerCenter(): Offset? {
        val host = hostCoordinates?.takeIf { it.isAttached } ?: return null
        val node = caller.coordinates.value?.takeIf { it.isAttached } ?: return null
        return host.localBoundingBoxOf(node, clipBounds = false).center
    }

    fun panelTargetCenter(panel: Size): Offset {
        val target = popupPlacement(
            callerBounds = callerBounds,
            panelSize = panel,
            viewport = Size(viewportSize.width.toFloat(), viewportSize.height.toFloat()),
            margin = 0f,
            offset = offsetPx,
            mode = transition.mode,
            anchoredToCaller = transition.anchoredToCaller,
            expandUpwards = transition.expandUpwards,
            layoutDirection = layoutDirection,
        )
        return target + Offset(panel.width / 2f, panel.height / 2f)
    }

    val requestDismiss: () -> Unit = remember { { latestDismiss.value { expanded = false } } }
    val openOrigin = callerBounds.center
    // Freeze the travel origin while open; the close starts from wherever the caller is now.
    val returnOrigin = remember(expanded) {
        if (expanded) openOrigin else liveCallerCenter() ?: openOrigin
    }

    val drawReveal: DrawScope.() -> Unit = {
        drawPopupReveal(
            caller = caller,
            callerBounds = callerBounds,
            callerLayer = callerLayer,
            panelLayer = panelLayer,
            panelShape = transition.shape,
            scaleProgress = scaleProgress.value,
            opacity = opacity.value,
            containerColor = transition.containerColor?.invoke() ?: Color.Unspecified,
        )
    }

    SideEffect {
        caller.returning = returning
        caller.active = true
        returning.draw = {
            val transform = returning.transform
            if (transform != null && panelSize != IntSize.Zero) {
                val panel = Size(panelSize.width.toFloat(), panelSize.height.toFloat())
                val targetCenter = panelTargetCenter(panel)
                val position = returning.origin +
                        (targetCenter - returnOrigin) * translationProgress.value -
                        Offset(panel.width / 2f, panel.height / 2f)
                withTransform({
                    transform(transform)
                    translate(position.x, position.y)
                }) {
                    // Shadow sits outside the clip so the blur is not cut at the panel bounds.
                    if (callerBounds.width >= 1f) {
                        drawPopupShadow(
                            shadow = shadow,
                            bounds = popupRevealBounds(
                                callerBounds.size,
                                panel,
                                scaleProgress.value
                            ),
                            alpha = opacity.value,
                        )
                    }
                    // The caller's bounds differ from the panel; resolve masks and pivots at panel size.
                    inset(0f, 0f, size.width - panel.width, size.height - panel.height) {
                        drawReveal()
                    }
                }
            }
        }
    }
    DisposableEffect(caller) {
        onDispose {
            caller.active = false
            if (caller.returning === returning) caller.returning = null
        }
    }

    // Keyed on "has a size": content can change size while closing without restarting the exit.
    LaunchedEffect(expanded, panelSize != IntSize.Zero) {
        if (panelSize == IntSize.Zero) {
            if (!expanded) onDismissed()
            return@LaunchedEffect
        }
        coroutineScope {
            launch {
                translationProgress.animateTo(
                    targetValue = if (expanded) 1f else 0f,
                    animationSpec = Springs.smooth(
                        durationMillis = if (expanded) 300 else 450,
                        extraBounce = if (expanded) 0.1 else 0.15,
                        visibilityThreshold = 0.0001f,
                    ),
                )
            }
            launch {
                scaleProgress.animateTo(
                    targetValue = if (expanded) 1f else 0f,
                    animationSpec = Springs.smooth(
                        durationMillis = if (expanded) 400 else 250,
                        visibilityThreshold = 0.0001f,
                        extraBounce = if (expanded) 0.2 else 0.0
                    ),
                )
            }
            launch {
                opacity.animateTo(
                    targetValue = if (expanded) 1f else 0f,
                    animationSpec = if (expanded) {
                        Springs.smooth(durationMillis = 200, visibilityThreshold = 0.0001f)
                    } else {
                        tween(durationMillis = 200)
                    },
                )
            }
        }
        if (!expanded) onDismissed()
    }
    // Once the panel is back within the caller's reach, the caller draws it in its own space:
    // the frozen matrix keeps the remaining spring attached to a moving or transforming page.
    LaunchedEffect(expanded) {
        if (expanded) return@LaunchedEffect
        snapshotFlow {
            val host = hostCoordinates?.takeIf { it.isAttached } ?: return@snapshotFlow false
            val node = caller.coordinates.value?.takeIf { it.isAttached }
                ?: return@snapshotFlow false
            if (panelSize == IntSize.Zero) return@snapshotFlow false
            val panel = Size(panelSize.width.toFloat(), panelSize.height.toFloat())
            val origin = host.localBoundingBoxOf(node, clipBounds = false).center
            val displacement = (panelTargetCenter(panel) - origin) * translationProgress.value
            popupWithinHandoffDistance(displacement, callerBounds.size, transition.handoffDistance)
        }.first { it }
        val host = hostCoordinates?.takeIf { it.isAttached } ?: return@LaunchedEffect
        val node = caller.coordinates.value?.takeIf { it.isAttached } ?: return@LaunchedEffect
        returning.origin = host.localBoundingBoxOf(node, clipBounds = false).center
        // Freeze the conversion once: subsequent page transforms must carry the remaining spring.
        returning.transform = Matrix().also {
            host.transformFrom(node, it)
            it.invert()
        }
    }
    // Reopening mid-close: a new request re-captures the caller where it is now, then re-expands.
    LaunchedEffect(caller.openCount) {
        if (caller.openCount == 0) return@LaunchedEffect
        val host = hostCoordinates?.takeIf { it.isAttached }
        val node = caller.coordinates.value?.takeIf { it.isAttached }
        if (host != null && node != null) {
            callerBounds = host.localBoundingBoxOf(node, clipBounds = false)
        }
        returning.transform = null
        expanded = true
    }
    BackHandler(enabled = expanded, onBack = requestDismiss)

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer()
                .drawBehind {
                    if (restingScrimAlpha > 0f) {
                        drawRect(
                            scrimColor,
                            alpha = (opacity.value.coerceIn(
                                0f,
                                1f
                            ) * restingScrimAlpha).coerceIn(0f, 1f),
                        )
                    }
                }
                .then(
                    if (expanded) {
                        Modifier
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                    down.consume()
                                    var isTap = true
                                    do {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.changes.any {
                                                it.id != down.id ||
                                                        (it.position - down.position).getDistance() > viewConfiguration.touchSlop
                                            }) isTap = false
                                        // Consume only on the scrim, before the ancestor draggable sees movement.
                                        event.changes.forEach { it.consume() }
                                    } while (event.changes.any { it.pressed })
                                    if (isTap) requestDismiss()
                                }
                            }
                            .semantics {
                                contentDescription = "关闭"
                                onClick {
                                    requestDismiss()
                                    true
                                }
                            }
                    } else {
                        Modifier
                    }
                )
        )
        // Measure and position in the same inset coordinate space, including when an ancestor
        // already consumed system bars. The full-size scrim remains outside this safe region.
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(transition.margin)
                .onSizeChanged { viewportSize = it }
                .onGloballyPositioned { coordinates ->
                    hostCoordinates = coordinates
                    if (callerBounds == Rect.Zero) {
                        caller.coordinates.value?.takeIf { it.isAttached }?.let {
                            callerBounds = coordinates.localBoundingBoxOf(it, clipBounds = false)
                        }
                    }
                },
        ) {
            Layout(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    // Menu mode owns the panel width; modal mode lets the content decide.
                    .then(
                        if (menuMode) {
                            Modifier.widthIn(transition.minWidth, transition.maxWidth)
                        } else {
                            Modifier
                        }
                    )
                    .onSizeChanged { if (expanded) panelSize = it }
                    .graphicsLayer {
                        if (panelSize == IntSize.Zero) return@graphicsLayer
                        val targetCenter =
                            panelTargetCenter(Size(size.width, size.height))
                        val panelCenter = targetCenter + (callerBounds.center - targetCenter) *
                                (1f - translationProgress.value)
                        // Apply live movement after the spring so following never gains animation lag.
                        val follow = if (expanded) Offset.Zero else {
                            (liveCallerCenter() ?: returnOrigin) - returnOrigin
                        }
                        translationX = panelCenter.x - size.width / 2f + follow.x
                        translationY = panelCenter.y - size.height / 2f + follow.y
                    }
                    .drawWithContent {
                        if (expanded) drawContent()
                        if (returning.transform == null) {
                            // Shadow first, outside the mask: the blur has to spill past the panel.
                            if (callerBounds.width >= 1f) {
                                drawPopupShadow(
                                    shadow = shadow,
                                    bounds = popupRevealBounds(
                                        callerBounds.size,
                                        size,
                                        scaleProgress.value,
                                    ),
                                    alpha = opacity.value,
                                )
                            }
                            drawReveal()
                        }
                    },
                content = {
                    Box(
                        // Isolate content invalidation from reveal frames; recording still follows
                        // content changes, while the outer draw only replays the live panel layer.
                        Modifier
                            .graphicsLayer()
                            .drawWithContent {
                                if (expanded) panelLayer.record { this@drawWithContent.drawContent() }
                            },
                    ) { content(requestDismiss) }
                },
            ) { measurables, constraints ->
                // Keep the width minimum: in menu mode widthIn owns it.
                val panel = measurables.single().measure(constraints.copy(minHeight = 0))
                // The exit draws the snapshot recorded while expanded, so the node has to report that
                // snapshot's size as well; placing live children would let them intercept taps.
                val snapshot = panelLayer.size
                val useSnapshot = !expanded && snapshot.width > 0
                layout(
                    width = if (useSnapshot) snapshot.width else panel.width,
                    height = if (useSnapshot) snapshot.height else panel.height,
                ) {
                    if (expanded) panel.place(0, 0)
                }
            }
        }
    }
}

private fun DrawScope.drawPopupReveal(
    caller: PopupCaller,
    callerBounds: Rect,
    callerLayer: GraphicsLayer,
    panelLayer: GraphicsLayer,
    panelShape: RoundedRectangularShape,
    scaleProgress: Float,
    opacity: Float,
    containerColor: Color,
) {
    if (callerBounds.width < 1f || callerBounds.height < 1f) {
        drawLayer(panelLayer)
        return
    }
    val recordingDensity = Density(density, fontScale)
    val bounds = popupRevealBounds(callerBounds.size, size, scaleProgress)
    // Resolve each endpoint at its own size; capsule corners would otherwise grow with the reveal.
    val start = caller.shape.corners(callerBounds.size, layoutDirection, this)
    val end = panelShape.corners(size, layoutDirection, this)
    val fraction = scaleProgress.coerceAtLeast(0f)
    val topLeft = lerp(start.topLeft, end.topLeft, fraction).toDp()
    val topRight = lerp(start.topRight, end.topRight, fraction).toDp()
    val bottomRight = lerp(start.bottomRight, end.bottomRight, fraction).toDp()
    val bottomLeft = lerp(start.bottomLeft, end.bottomLeft, fraction).toDp()
    val ltr = layoutDirection == LayoutDirection.Ltr
    val shape = UnevenRoundedRectangle(
        topStart = if (ltr) topLeft else topRight,
        topEnd = if (ltr) topRight else topLeft,
        bottomEnd = if (ltr) bottomRight else bottomLeft,
        bottomStart = if (ltr) bottomLeft else bottomRight,
    )
    cornerMaskClip(bounds, shape) {
        if (caller.layer.size != IntSize.Zero && callerLayer.size != caller.layer.size) {
            // The display list references the live child layer, so colour updates do not need
            // another wrapper recording. A size change does require updating its local bounds.
            callerLayer.record(recordingDensity, layoutDirection, caller.layer.size) {
                drawLayer(caller.layer)
            }
        }
        val contentAlpha = opacity.coerceIn(0f, 1f)
        val callerColor = caller.containerColor?.invoke() ?: Color.Unspecified
        // The recording already contains translucent fills; backing them again compounds alpha.
        if (callerColor.alpha == 1f) drawRect(callerColor)
        val callerScale = minOf(
            bounds.width / callerBounds.width,
            bounds.height / callerBounds.height,
        )
        callerLayer.pivotOffset = Offset(callerLayer.size.width / 2f, callerLayer.size.height / 2f)
        callerLayer.scaleX = callerScale
        callerLayer.scaleY = callerScale
        callerLayer.alpha = 1f - contentAlpha
        translate(
            (size.width - callerBounds.width) / 2f,
            (size.height - callerBounds.height) / 2f,
        ) { drawLayer(callerLayer) }
        if (panelLayer.size != IntSize.Zero) {
            val contentScale = minOf(bounds.width / size.width, bounds.height / size.height)
            panelLayer.pivotOffset = center
            panelLayer.scaleX = contentScale
            panelLayer.scaleY = contentScale
            // One path at every alpha: no panel-sized buffer to clip the surface, and no jump
            // when the fade finishes.
            if (containerColor.alpha > 0f) {
                drawRect(
                    containerColor.copy(alpha = containerColor.alpha * contentAlpha),
                    topLeft = bounds.topLeft,
                    size = bounds.size,
                )
            }
            panelLayer.alpha = contentAlpha
            drawLayer(panelLayer)
        }
    }
}

internal fun popupWithinHandoffDistance(
    displacement: Offset,
    callerSize: Size,
    distance: Float,
): Boolean = displacement.getDistance() <= minOf(callerSize.width, callerSize.height) * distance

private fun DrawScope.drawPopupShadow(shadow: PopupMenuShadow, bounds: Rect, alpha: Float) {
    if (bounds.width <= 0f || bounds.height <= 0f || alpha <= 0f) return
    translate(bounds.left, bounds.top) {
        with(shadow) { draw(bounds.size, alpha) }
    }
}

private fun popupRevealBounds(callerSize: Size, panelSize: Size, progress: Float): Rect {
    // Only the lower end is clamped: a bouncy scale spring is allowed to overshoot the panel,
    // otherwise the elasticity never shows.
    val fraction = progress.coerceAtLeast(0f)
    val width = lerp(callerSize.width, panelSize.width, fraction)
    val height = lerp(callerSize.height, panelSize.height, fraction)
    return Rect(
        (panelSize.width - width) / 2f,
        (panelSize.height - height) / 2f,
        (panelSize.width + width) / 2f,
        (panelSize.height + height) / 2f,
    )
}

internal fun popupPlacement(
    callerBounds: Rect,
    panelSize: Size,
    viewport: Size,
    margin: Float,
    offset: Offset,
    mode: PopupMode,
    anchoredToCaller: Boolean,
    expandUpwards: Boolean,
    layoutDirection: LayoutDirection,
): Offset {
    // Modal always hangs off the caller; Menu docks to the bottom unless it asks to anchor.
    val onCaller = mode == PopupMode.Modal || anchoredToCaller
    val maxX = (viewport.width - panelSize.width - margin).coerceAtLeast(margin)
    val maxY = (viewport.height - panelSize.height - margin).coerceAtLeast(margin)
    val ltr = layoutDirection == LayoutDirection.Ltr
    val x = when {
        // Modal centres on the caller.
        mode == PopupMode.Modal -> callerBounds.center.x - panelSize.width / 2f + offset.x
        // Menu attaches its end edge to the caller's end edge; a caller on the start half attaches
        // by the start edge instead.
        onCaller -> {
            val callerOnEndHalf =
                if (ltr) callerBounds.center.x > viewport.width / 2f
                else callerBounds.center.x < viewport.width / 2f
            when {
                ltr && callerOnEndHalf -> callerBounds.right - panelSize.width + offset.x
                ltr -> callerBounds.left + offset.x
                callerOnEndHalf -> callerBounds.left + offset.x
                else -> callerBounds.right - panelSize.width + offset.x
            }
        }

        else -> (viewport.width - panelSize.width) / 2f + offset.x
    }.coerceIn(margin, maxX)
    val y = if (onCaller) {
        if (expandUpwards) callerBounds.bottom - panelSize.height + offset.y
        else callerBounds.top + offset.y
    } else {
        viewport.height - panelSize.height + offset.y
    }.coerceIn(margin, maxY)
    return Offset(x, y)
}
