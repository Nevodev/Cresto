package com.nevoit.cresto.navigation

import android.os.Trace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy as LayerCompositingStrategy
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.UnplacedAwareModifierNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.RoundedRectangularShape
import com.kyant.shapes.UnevenRoundedRectangle
import com.nevoit.cresto.navigation.clip.cornerMaskClip
import com.nevoit.cresto.navigation.clip.cornerMaskClipInLayer
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.atan2

private const val PageContentRevealFraction = 0.3f

internal class NavigationCallerVisibility {
    var owner: NavigationCallerTransition? by mutableStateOf(null)
    var placedCaller: NavigationCaller? by mutableStateOf(null)
    var coordinates: LayoutCoordinates? by mutableStateOf(null)
    var boundsInHost: Rect? by mutableStateOf(null)
    var visibleBoundsInHost: Rect? by mutableStateOf(null)

    fun updateCoordinates(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        owner?.let {
            boundsInHost = it.boundsInHost(coordinates)
            visibleBoundsInHost = it.visibleBoundsInHost(coordinates)
        }
    }
}

internal val LocalNavigationCallers =
    staticCompositionLocalOf<MutableMap<Any, NavigationCallerVisibility>?> { null }

@Stable
class NavigationCaller internal constructor(
    internal val layer: GraphicsLayer,
    internal val shape: RoundedRectangularShape,
    internal val backgroundColor: Color,
    internal val visibility: NavigationCallerVisibility,
    internal val enabled: Boolean,
) {
    internal var coordinates: LayoutCoordinates? = null
    internal var recordingRequested by mutableStateOf(false)
        private set
    internal var recorded by mutableStateOf(false)

    internal suspend fun prepare(
        host: LayoutCoordinates?,
    ): NavigationCallerTransition? {
        if (!enabled || visibility.owner != null || coordinates?.isAttached != true || host?.isAttached != true) return null
        if (!prepareLayer()) return null
        val source = coordinates?.takeIf { it.isAttached }
        val bounds = if (source != null && host.isAttached) host.localBoundingBoxOf(source, clipBounds = false) else null
        if (bounds == null || bounds.isEmpty) return null
        return NavigationCallerTransition(this, bounds, host)
    }

    internal suspend fun prepareLayer(): Boolean {
        if (recordingRequested || coordinates?.isAttached != true) return false
        layer.compositingStrategy = LayerCompositingStrategy.Offscreen
        recordingRequested = true
        try {
            // Start with the original card rasterized offscreen; transitions only transform that layer.
            snapshotFlow { recorded || visibility.placedCaller !== this@NavigationCaller }.first { it }
            return recorded && coordinates?.isAttached == true && layer.size != IntSize.Zero
        } finally {
            recordingRequested = false
            recorded = false
        }
    }
}

@Composable
fun rememberNavigationCaller(
    shape: RoundedRectangularShape,
    backgroundColor: Color,
    key: Any,
): NavigationCaller {
    val layer = rememberGraphicsLayer()
    val enabled = LocalContinuousTransitionsEnabled.current
    val callers = LocalNavigationCallers.current
    // The page can leave composition while covered; recreated callers must inherit the active takeover.
    val visibility = if (callers != null) {
        callers.getOrPut(key) { NavigationCallerVisibility() }
    } else remember(key) { NavigationCallerVisibility() }
    val caller = remember(layer, shape, backgroundColor, visibility, enabled) {
        NavigationCaller(layer, shape, backgroundColor, visibility, enabled)
    }
    return caller
}

private data class NavigationCallerCoordinatesElement(val caller: NavigationCaller) :
    ModifierNodeElement<NavigationCallerCoordinatesNode>() {
    override fun create() = NavigationCallerCoordinatesNode(caller)
    override fun update(node: NavigationCallerCoordinatesNode) = node.update(caller)
    override fun InspectorInfo.inspectableProperties() {
        name = "navigationCallerCoordinates"
    }
}

private class NavigationCallerCoordinatesNode(var caller: NavigationCaller) :
    Modifier.Node(), GlobalPositionAwareModifierNode, UnplacedAwareModifierNode {
    private var coordinates: LayoutCoordinates? = null

    fun update(caller: NavigationCaller) {
        val coordinates = coordinates
        clear()
        this.caller = caller
        coordinates?.takeIf { it.isAttached }?.let(::onGloballyPositioned)
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        caller.coordinates = coordinates
        caller.visibility.placedCaller = caller
        caller.visibility.updateCoordinates(coordinates)
    }

    // Lazy layouts can retain attached nodes after they stop placing them.
    override fun onUnplaced() = clear()
    override fun onDetach() = clear()

    private fun clear() {
        if (caller.visibility.coordinates === coordinates) {
            caller.visibility.placedCaller = null
            caller.visibility.coordinates = null
            caller.visibility.visibleBoundsInHost = null
        }
        if (caller.coordinates === coordinates) caller.coordinates = null
        coordinates = null
    }
}

/** Disable [clipContent] only when the content draws its own rounded background and stays inside it. */
fun Modifier.navigationCaller(caller: NavigationCaller, clipContent: Boolean = true): Modifier =
    then(NavigationCallerCoordinatesElement(caller))
        .drawWithContent {
            val owner = caller.visibility.owner
            if (caller.recordingRequested || owner != null) {
                caller.layer.compositingStrategy = LayerCompositingStrategy.Offscreen
                // The implicit record overload retargets LayoutNodeDrawScope to itself. Density-dependent
                // work must run in child draw nodes, which install their coordinator's concrete density.
                caller.layer.record {
                    this@drawWithContent.drawContent()
                }
                if (owner == null) {
                    // The host mutates this same layer during transitions; prepare it in its normal pose.
                    caller.layer.pivotOffset = Offset.Zero
                    caller.layer.scaleX = 1f
                    caller.layer.scaleY = 1f
                    caller.layer.translationX = 0f
                    caller.layer.translationY = 0f
                    caller.layer.alpha = 1f
                    drawLayer(caller.layer)
                }
                caller.recorded = true
            } else drawContent()
        }
        .then(
            if (clipContent) Modifier
                .cornerMaskClip(caller.shape)
                .drawBehind { drawRect(caller.backgroundColor) }
            else Modifier
        )

internal class NavigationCallerTransition(
    val caller: NavigationCaller,
    val bounds: Rect,
    private val host: LayoutCoordinates,
) {
    val layer: GraphicsLayer?
        get() = caller.visibility.placedCaller?.layer?.takeIf { it.size != IntSize.Zero }
    private var disposed = false
    private var origin by mutableStateOf(bounds.topLeft)
    private var returnOrigin by mutableStateOf<Offset?>(null)
    private var returnCorrection = Offset.Zero
    private var returnStartProgress = 1f
    private var following by mutableStateOf<NavigationCallerFollowing?>(null)
    private val callerToHost = Matrix()

    internal val followsCaller: Boolean get() = following != null

    internal suspend fun prepareReturn(): Boolean {
        if (disposed) return false
        val placedCaller = caller.visibility.placedCaller ?: return false
        return placedCaller.prepareLayer() && !disposed && caller.visibility.placedCaller === placedCaller && hasVisibleCaller()
    }

    internal fun boundsInHost(coordinates: LayoutCoordinates): Rect? =
        if (host.isAttached && coordinates.isAttached) {
            host.localBoundingBoxOf(coordinates, clipBounds = false)
        } else null

    private fun currentPosition(): Offset {
        // Keep observing movement and size after the anchor freezes, including a drag after the spring stops.
        val lastBounds = caller.visibility.boundsInHost
        following?.let { return it.bounds.topLeft }
        return caller.visibility.coordinates?.let(::boundsInHost)?.topLeft ?: lastBounds?.topLeft ?: bounds.topLeft
    }

    private fun animatedPosition(progress: Float): Offset {
        val fraction = progress.coerceAtMost(1f)
        val anchor = returnOrigin
        val follow = if (anchor != null) currentPosition() - anchor else Offset.Zero
        val correction = if (anchor != null) {
            returnCorrection * (fraction / returnStartProgress)
        } else Offset.Zero
        return origin * (1f - fraction) + follow + correction
    }

    fun beginReturn(progress: Float) {
        if (returnOrigin != null) return
        val position = animatedPosition(progress)
        val anchor = currentPosition()
        origin = anchor
        returnCorrection = position - anchor * (1f - progress)
        returnStartProgress = progress.takeIf { it != 0f } ?: 1f
        returnOrigin = anchor
    }

    fun resumeOpen(progress: Float) {
        if (returnOrigin == null) return
        val remaining = 1f - progress.coerceAtMost(1f)
        val position = presentation(animatedPosition(progress))?.position ?: animatedPosition(progress)
        origin = if (remaining > 0f) position / remaining else bounds.topLeft
        returnOrigin = null
        following = null
    }

    fun takeOver() {
        caller.visibility.owner = this
        caller.visibility.coordinates?.takeIf { it.isAttached }?.let(caller.visibility::updateCoordinates)
    }

    fun release() {
        following = null
        if (caller.visibility.owner === this) {
            caller.visibility.owner = null
            caller.visibility.placedCaller?.layer?.compositingStrategy = LayerCompositingStrategy.Auto
        }
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        release()
    }

    internal fun targetBounds(): Rect? = caller.visibility.coordinates?.takeIf { it.isAttached }
        ?.let(::boundsInHost)

    internal fun visibleBoundsInHost(coordinates: LayoutCoordinates): Rect? {
        if (!host.isAttached || !coordinates.isAttached) return null
        return host.localBoundingBoxOf(coordinates, clipBounds = true)
            .intersect(Rect(Offset.Zero, host.size.toSize()))
            .takeUnless { it.isEmpty }
    }

    internal fun hasVisibleCaller(): Boolean {
        val coordinates = caller.visibility.coordinates?.takeIf { it.isAttached && host.isAttached } ?: return false
        return (caller.visibility.visibleBoundsInHost ?: visibleBoundsInHost(coordinates))?.isEmpty == false
    }

    internal fun followCaller(scaleProgress: Float, clipProgress: Float) {
        if (following != null || !hasVisibleCaller()) return
        val coordinates = caller.visibility.coordinates?.takeIf { it.isAttached } ?: return
        val target = targetBounds() ?: return
        callerToHost.reset()
        host.transformFrom(coordinates, callerToHost)
        // Overscroll can retain a stale inverse at identity; invert the forward transform ourselves.
        val inverse = Matrix(callerToHost.values.copyOf()).also { it.invert() }
        Trace.beginSection("Navigation:followCaller")
        try {
            following = NavigationCallerFollowing(target, scaleProgress, clipProgress, inverse)
        } finally {
            Trace.endSection()
        }
    }

    internal fun presentation(position: Offset): NavigationCallerPresentation? {
        val following = following ?: return null
        caller.visibility.coordinates?.takeIf { it.isAttached && host.isAttached }?.let {
            callerToHost.reset()
            host.transformFrom(it, callerToHost)
        }
        return navigationCallerPresentation(position, following.hostToCaller, callerToHost)
    }

    fun frame(
        size: Size,
        progress: Float,
        scaleProgress: Float = progress,
        clip: Boolean = false,
    ): Rect {
        // Preserve the return spring's undershoot in geometry; opacity and corners stay clamped separately.
        val fraction = progress.coerceAtMost(1f)
        val scaleFraction = scaleProgress.coerceAtMost(1f)
        val following = following
        val target = following?.bounds?.size ?: bounds.size
        val startProgress = if (clip) following?.clipProgress else following?.scaleProgress
        return Rect(
            // Live caller movement is added after the spring, without a second animation.
            animatedPosition(fraction),
            navigationCallerFrameSize(bounds.size, target, size, scaleFraction, startProgress),
        )
    }
}

private class NavigationCallerFollowing(
    val bounds: Rect,
    val scaleProgress: Float,
    val clipProgress: Float,
    val hostToCaller: Matrix,
)

internal class NavigationCallerPresentation(
    val position: Offset,
    val scaleX: Float,
    val scaleY: Float,
    val rotationZ: Float,
)

internal fun navigationCallerPresentation(
    position: Offset,
    hostToCaller: Matrix,
    callerToHost: Matrix,
): NavigationCallerPresentation {
    fun map(point: Offset) = callerToHost.map(hostToCaller.map(point))
    val origin = map(position)
    // A long basis avoids amplifying float rounding into visible scale jitter at large host offsets.
    val horizontal = (map(position + Offset(1024f, 0f)) - origin) / 1024f
    val vertical = (map(position + Offset(0f, 1024f)) - origin) / 1024f
    val orientation = if (horizontal.x * vertical.y - horizontal.y * vertical.x < 0f) -1f else 1f
    return NavigationCallerPresentation(
        origin,
        horizontal.getDistance(),
        vertical.getDistance() * orientation,
        Math.toDegrees(atan2(horizontal.y, horizontal.x).toDouble()).toFloat(),
    )
}

internal fun navigationCallerFrameSize(
    original: Size,
    target: Size,
    page: Size,
    progress: Float,
    followProgress: Float?,
): Size {
    val correction = if (followProgress != null && followProgress != 0f) {
        (1f - followProgress) * progress / followProgress
    } else 0f
    return Size(
        (lerp(target.width, page.width, progress) + (original.width - target.width) * correction).coerceAtLeast(1f),
        (lerp(target.height, page.height, progress) + (original.height - target.height) * correction).coerceAtLeast(1f),
    )
}

internal fun navigationCallerWithinFollowDistance(frame: Rect, target: Rect, distance: Float): Boolean =
    maxOf(
        abs(frame.left - target.left), abs(frame.top - target.top),
        abs(frame.right - target.right), abs(frame.bottom - target.bottom),
    ) <= minOf(target.width, target.height) * distance

@Composable
internal fun rememberNavigationCallerTransition(
    source: NavigationCallerTransition,
    progress: State<Float>,
    scaleProgress: State<Float>,
    clipProgress: State<Float>,
    isTransitioning: State<Boolean>,
    pageShape: RoundedRectangularShape,
    returning: Boolean,
    gestureActive: Boolean,
    followDistance: Float,
): Modifier {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val rectangle = remember { RoundedRectangle(0.dp) }
    var pageSize by remember { mutableStateOf(IntSize.Zero) }
    val startCorners = remember(source, direction, density) {
        source.caller.shape.corners(source.bounds.size, direction, density)
    }
    val endCorners = remember(pageShape, pageSize, direction, density) {
        pageShape.corners(pageSize.toSize(), direction, density)
    }
    val maximumRadiusPx = maxOf(
        startCorners.topLeft, startCorners.topRight, startCorners.bottomRight, startCorners.bottomLeft,
        endCorners.topLeft, endCorners.topRight, endCorners.bottomRight, endCorners.bottomLeft,
    )
    val revealShape = {
        val fraction = clipProgress.value.coerceIn(0f, 1f)
        if (!isTransitioning.value && fraction == 1f) rectangle else {
            with(density) {
                val topLeft = lerp(startCorners.topLeft, endCorners.topLeft, fraction).toDp()
                val topRight = lerp(startCorners.topRight, endCorners.topRight, fraction).toDp()
                val bottomRight = lerp(startCorners.bottomRight, endCorners.bottomRight, fraction).toDp()
                val bottomLeft = lerp(startCorners.bottomLeft, endCorners.bottomLeft, fraction).toDp()
                val ltr = direction == LayoutDirection.Ltr
                UnevenRoundedRectangle(
                    topStart = if (ltr) topLeft else topRight,
                    topEnd = if (ltr) topRight else topLeft,
                    bottomEnd = if (ltr) bottomRight else bottomLeft,
                    bottomStart = if (ltr) bottomLeft else bottomRight,
                )
            }
        }
    }
    val revealFrame = { size: Size ->
        val frame = source.frame(size, progress.value, scaleProgress.value)
        val bottom = source.frame(size, clipProgress.value.coerceIn(0f, 1f), clip = true).bottom
        Rect(
            left = frame.left,
            top = frame.top,
            right = frame.right,
            // Only the bottom reveal edge stays free of the return spring's undershoot.
            bottom = maxOf(bottom, frame.top + 1f),
        )
    }

    LaunchedEffect(source, returning, gestureActive, followDistance) {
        if (!returning || gestureActive || source.followsCaller) return@LaunchedEffect
        snapshotFlow {
            val target = source.targetBounds()
            pageSize != IntSize.Zero && target != null && source.hasVisibleCaller() &&
                navigationCallerWithinFollowDistance(revealFrame(pageSize.toSize()), target, followDistance)
        }.first { it }
        source.followCaller(scaleProgress.value, clipProgress.value.coerceIn(0f, 1f))
    }

    return Modifier
        .onSizeChanged { pageSize = it }
        .graphicsLayer {
            val frame = revealFrame(size)
            val presentation = source.presentation(frame.topLeft)
            // Keep drawing and hit testing in the navigation layer; only its presentation follows the caller.
            transformOrigin = TransformOrigin(0f, 0f)
            translationX = presentation?.position?.x ?: frame.left
            translationY = presentation?.position?.y ?: frame.top
            scaleX = presentation?.scaleX ?: 1f
            scaleY = presentation?.scaleY ?: 1f
            rotationZ = presentation?.rotationZ ?: 0f
            // The corner textures mask the pixels; keep hit bounds rectangular to avoid path masks.
            clip = isTransitioning.value
            shape = object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density,
                ): Outline = Outline.Rectangle(Rect(Offset.Zero, frame.size))
            }
        }
        // NavigationHost already isolates this entire reveal; retain the inner page raster cache.
        .cornerMaskClipInLayer(
            bounds = { size ->
                Rect(Offset.Zero, revealFrame(size).size)
            },
            shape = revealShape,
            maximumRadiusPx = maximumRadiusPx,
        )
        .drawWithContent {
            val fraction = progress.value.coerceIn(0f, 1f)
            if (!isTransitioning.value && fraction == 1f) {
                drawContent()
            } else {
                val frame = revealFrame(size)
                val sourceLayer = source.layer

                if (sourceLayer != null) {
                    sourceLayer.pivotOffset = Offset.Zero
                    sourceLayer.scaleX = frame.width / sourceLayer.size.width
                    sourceLayer.scaleY = sourceLayer.scaleX
                    sourceLayer.translationX = 0f
                    sourceLayer.translationY = 0f
                    sourceLayer.alpha = 1f
                    drawLayer(sourceLayer)
                } else {
                    // A removed caller no longer has a live layer; the page can still finish its side exit.
                    drawRect(source.caller.backgroundColor, size = frame.size)
                }

                drawContent()
            }
        }
        .graphicsLayer {
            // Transform the actual page so Compose maps pointer coordinates with its presentation.
            val frame = revealFrame(size)
            transformOrigin = TransformOrigin(0f, 0f)
            scaleX = frame.width / size.width
            scaleY = scaleX
            translationX = 0f
            translationY = 0f
        }
        .graphicsLayer {
            // Resolve text at the page's original size before the outer layer scales it.
            compositingStrategy = if (isTransitioning.value) {
                CompositingStrategy.Offscreen
            } else CompositingStrategy.Auto
            alpha = (progress.value / PageContentRevealFraction).coerceIn(0f, 1f)
        }
}
