package com.nevoit.cresto.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.kyant.shapes.RoundedRectangularShape
import com.nevoit.cresto.navigation.clip.cornerMaskClipInLayer
import com.nevoit.cresto.navigation.shape.deviceCornerShape
import com.nevoit.glasense.theme.tokens.Springs
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel as androidxViewModel

/**
 * Share of the entrance travel a page must complete before it *looks* settled — the default
 * threshold of [PageTransitionState.awaitNearlySettled].
 */
private const val DefaultRevealProgress = 0.995f

/**
 * The entrance/exit state of the page currently rendering, handed to that page through
 * [LocalPageTransitionState].
 *
 * Exists so a page can hold off work that would stutter its own entrance — a full list landing
 * halfway through the slide, for instance. [progress] changes every frame, so `awaitSettled` /
 * `awaitNearlySettled` (or a `snapshotFlow` of your own) is the only safe way to wait on it.
 */
@Stable
class PageTransitionState internal constructor(
    /** 0 = the page is fully off its resting place, 1 = in place. */
    val progress: State<Float>,
    /** True while the entrance or exit animation is still running. */
    val isTransitioning: State<Boolean>,
    /**
     * Full slide distance of this page, in device px (= host width). `(1 - progress) * slideDistancePx`
     * is how far the page still has to travel; zero when the parent gives no width to slide across.
     */
    val slideDistancePx: Float,
) {
    /** Suspends until the page has finished moving into or out of place. */
    suspend fun awaitSettled() {
        snapshotFlow { isTransitioning.value }.first { !it }
    }

    /**
     * Suspends until the page *looks* settled, skipping the tail of the slide.
     *
     * The default threshold follows [DefaultRevealProgress]: by then the
     * spring has only a few px left and is moving at near-zero speed, so the eye already reads the
     * page as in place — while that tail still costs a good chunk of the entrance. Pass
     * [remainingDistancePx] to use an absolute distance (24f, say) instead; both are the same test.
     */
    suspend fun awaitNearlySettled(
        remainingDistancePx: Float = (1f - DefaultRevealProgress) * slideDistancePx,
    ) {
        val threshold = remainingDistancePx.coerceAtLeast(0f)
        snapshotFlow { isTransitioning.value && (1f - progress.value) * slideDistancePx > threshold }
            .first { !it }
    }
}

/**
 * Null outside a [NavigationHost] (previews, isolated tests) — pages must treat that as "no
 * transition to wait for" rather than skipping their content.
 */
val LocalPageTransitionState = staticCompositionLocalOf<PageTransitionState?> { null }

private val LocalPageBackground = staticCompositionLocalOf<MutableState<ColorProducer?>?> { null }
private val LocalNavigationContentReady = staticCompositionLocalOf<MutableState<Boolean>?> { null }

/** Holds the entrance at its starting frame until the page has real content to lay out. */
@Composable
fun ReportNavigationReady(ready: Boolean) {
    val contentReady = LocalNavigationContentReady.current
    DisposableEffect(contentReady) {
        onDispose { contentReady?.value = true }
    }
    SideEffect { contentReady?.value = ready }
}

/** Shares the page's drawn background with the host while transitions expose it. */
@Composable
fun ReportNavigationBackground(color: ColorProducer) {
    val background = LocalPageBackground.current
    val currentColor by rememberUpdatedState(color)
    DisposableEffect(background) {
        background?.value = ColorProducer { currentColor() }
        onDispose { background?.value = null }
    }
}

private class PageTransition(
    initialProgress: Float,
    initialUnderlayProgress: Float,
    parentEntryId: Long?,
    parentLifecycle: Lifecycle,
    caller: NavigationCallerTransition? = null,
) {
    var caller by mutableStateOf(caller)
    val progress = Animatable(initialProgress)
    var callerScaleProgress = caller?.let { Animatable(initialProgress) }
    val underlayProgress = Animatable(initialUnderlayProgress)
    var parentEntryId by mutableStateOf(parentEntryId)
    var ready by mutableStateOf(false)
    val contentReady = mutableStateOf(true)
    var composed = false
    var targetProgress by mutableFloatStateOf(1f)
    val isTransitioning = mutableStateOf(initialProgress != 1f)
    val background = mutableStateOf<ColorProducer?>(null)
    var callerGestureActive by mutableStateOf(false)
    var callerReturnStarted by mutableStateOf(false)
    var callerReturnTargetLost by mutableStateOf(false)
    var callerSideExit by mutableStateOf<CallerSideExit?>(null)
    val lifecycleOwner = NavigationPageLifecycleOwner(parentLifecycle)
    val callers = mutableMapOf<Any, NavigationCallerVisibility>()
}

private class CallerSideExit(pageProgress: Float, scaleProgress: Float, clipProgress: Float) {
    val progress = Animatable(0f)
    val pageProgress: State<Float> = mutableStateOf(pageProgress)
    val scaleProgress: State<Float> = mutableStateOf(scaleProgress)
    val clipProgress: State<Float> = mutableStateOf(clipProgress)
}

private class NavigationPageLifecycleOwner(
    private val parentLifecycle: Lifecycle,
) : LifecycleOwner, LifecycleEventObserver {
    private val registry = LifecycleRegistry(this)
    private var maxState = Lifecycle.State.CREATED

    override val lifecycle: Lifecycle = registry

    init {
        parentLifecycle.addObserver(this)
        syncState()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        syncState()
    }

    fun moveTo(state: Lifecycle.State) {
        maxState = state
        syncState()
    }

    fun destroy() {
        parentLifecycle.removeObserver(this)
        registry.currentState = Lifecycle.State.DESTROYED
    }

    private fun syncState() {
        registry.currentState = when {
            parentLifecycle.currentState == Lifecycle.State.DESTROYED -> {
                Lifecycle.State.DESTROYED
            }

            !parentLifecycle.currentState.isAtLeast(Lifecycle.State.CREATED) -> {
                Lifecycle.State.INITIALIZED
            }

            else -> minOf(parentLifecycle.currentState, maxState)
        }
    }
}

// Host-level controls for callers outside the page tree (a persistent bottom bar that
// wants to pop back to the root when a tab is tapped).
@Stable
class NavigationHostHandle internal constructor(
    private val closeToRootInternal: (returnToCaller: Boolean) -> Unit,
) {
    /** Closes every page above the root. No-op when already at the root. */
    fun closeToRoot() = closeToRootInternal(true)

    fun closeToRoot(returnToCaller: Boolean) = closeToRootInternal(returnToCaller)
}

/**
 * Renders the current page, live caller owners and pages participating in a transition, coordinating system back,
 * saveable state, lifecycle state, and per-page ViewModel stores.
 *
 * The receiver content is invoked once for each visible page. Use [NavigationScope.destination] to
 * render the route and [NavigationScope.navigate] or [NavigationScope.back] from page actions. When
 * [pageViewModelStores] is omitted, the host creates one ViewModel-backed store manager for the
 * current ViewModel owner.
 *
 * The slide distance is the host's own width. [windowWidthPx] overrides it — two hosts side by side in
 * a two-pane layout each measure their own pane, which is what makes that layout work — and
 * [underlayTranslation], [scrimColor] and [scrimAlpha] are optional visual
 * policy hooks; they do not participate in back-stack or lifecycle decisions.
 * [onHostReady] hands out a [NavigationHostHandle] once, for callers outside the page tree.
 * [pageShape] controls page corners during transitions; nested hosts can use square pages.
 * [callerFollowDistance] starts following the caller's transforms near the return target while
 * the animated page remains above the underlay's scrim in this host.
 */
@Composable
fun <D : Any> NavigationHost(
    modifier: Modifier = Modifier,
    navigator: Navigator<D>,
    pageViewModelStores: PageViewModelStores? = null,
    windowWidthPx: Int? = null,
    animationSpec: AnimationSpec<Float>? = null,
    callerUnderlayScale: Float = 1f,
    callerFollowDistance: Float = 0.25f,
    underlayTranslation: (windowWidthPx: Int, progress: Float) -> Float = { windowWidthPx, progress ->
        -(windowWidthPx / 3f) * progress
    },
    scrimColor: Color = Color.Black,
    scrimAlpha: Float = 0.6f,
    onHostReady: (NavigationHostHandle) -> Unit = {},
    pageShape: RoundedRectangularShape = deviceCornerShape(),
    transitionBackground: Color = Color.Transparent,
    content: @Composable NavigationScope<D>.() -> Unit,
) {
    require(callerUnderlayScale > 0f && callerUnderlayScale <= 1f)
    require(callerFollowDistance.isFinite() && callerFollowDistance >= 0f)
    val continuousTransitionsEnabled by rememberUpdatedState(LocalContinuousTransitionsEnabled.current)
    val navigationScope = rememberCoroutineScope()
    var preparingCaller by remember { mutableStateOf(false) }
    var hostCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { hostCoordinates = it }) {
        // An unbounded parent (a horizontal scroller, say) has no meaningful slide distance; zero
        // makes the page appear in place instead of sliding from infinity.
        val hostWidthPx = windowWidthPx
            ?: constraints.maxWidth.takeIf { it in 1 until Int.MAX_VALUE }
            ?: 0
        val resolvedAnimationSpec = animationSpec
            ?: Springs.smooth(350, 0.0, if (hostWidthPx > 0) 1f / hostWidthPx else 1f)
        val callerEnterTranslationSpec = animationSpec
            ?: Springs.smooth(375, 0.0, if (hostWidthPx > 0) 1f / hostWidthPx else 1f)
        val callerEnterScaleSpec = animationSpec
            ?: Springs.smooth(350, 0.0, if (hostWidthPx > 0) 1f / hostWidthPx else 1f)
        // Progress also moves the caller vertically; width alone can leave a visible snap when the spring ends.
        val callerReturnVisibilityThreshold = 0.05f / maxOf(
            hostWidthPx,
            constraints.maxHeight.takeIf { it in 1 until Int.MAX_VALUE } ?: 0,
            1,
        )
        val callerReturnTranslationSpec = animationSpec
            ?: Springs.smooth(350, 0.1, callerReturnVisibilityThreshold)
        val callerReturnScaleSpec = animationSpec
            ?: Springs.smooth(350, 0.1, callerReturnVisibilityThreshold)

        val stores = pageViewModelStores ?: androidxViewModel<PageViewModelStores>()
        val parentLifecycle = LocalLifecycleOwner.current.lifecycle
        val initialBackStack = remember(navigator) { navigator.backStack }
        // A plain list in a state holder, not `mutableStateListOf`: the retained set is only ever
        // replaced wholesale, and `SnapshotStateList` is `android.os.Parcelable` in the Android source
        // set — a supertype a common source set cannot resolve, which is what makes an IDE report
        // "Cannot access 'android.os.Parcelable' which is a supertype of 'SnapshotStateList'".
        var retainedEntries by remember(navigator) { mutableStateOf(initialBackStack) }
        val transitions = remember(navigator, parentLifecycle) {
            mutableMapOf<Long, PageTransition>().apply {
                initialBackStack.forEachIndexed { index, entry ->
                    this[entry.id] = PageTransition(
                        initialProgress = 1f,
                        initialUnderlayProgress = 1f,
                        parentEntryId = initialBackStack.getOrNull(index - 1)?.id,
                        parentLifecycle = parentLifecycle,
                    )
                }
            }
        }
        val pageStateHolder = rememberSaveableStateHolder()

        fun entriesToCompose(): List<PageEntry<D>> {
            val ids = mutableSetOf(navigator.current.id)
            transitions.forEach { (id, transition) ->
                if (transition.caller != null || transition.isTransitioning.value ||
                    (transition.callerReturnTargetLost && transition.callerGestureActive)
                ) {
                    ids += id
                    transition.parentEntryId?.let { ids += it }
                }
            }
            return retainedEntries.filter { it.id in ids }
        }

        DisposableEffect(transitions) {
            onDispose {
                transitions.values.forEach { transition ->
                    transition.caller?.dispose()
                    transition.lifecycleOwner.destroy()
                }
            }
        }

        fun release(entry: PageEntry<D>) {
            retainedEntries -= entry
            transitions.remove(entry.id)?.let {
                it.caller?.dispose()
                it.lifecycleOwner.destroy()
            }
            pageStateHolder.removeState(entry.id)
            stores.remove(entry.id)
        }

        fun open(destination: D, preparedCaller: NavigationCallerTransition? = null) {
            val exitingEntry = retainedEntries.lastOrNull { entry ->
                entry.destination == destination && transitions[entry.id]?.targetProgress == 0f &&
                        transitions[entry.id]?.callerSideExit == null
            }
            if (exitingEntry != null) {
                preparedCaller?.dispose()
                val transition = transitions.getValue(exitingEntry.id)
                transition.caller?.resumeOpen(transition.progress.value)
                transition.callerReturnStarted = false
                transition.callerReturnTargetLost = false
                transition.parentEntryId = navigator.current.id
                navigator.reopen(exitingEntry)
                transition.targetProgress = 1f
                transition.isTransitioning.value = true
                transition.caller?.takeOver()
                // Move the entry back to the top of the draw order.
                retainedEntries = retainedEntries - exitingEntry + exitingEntry
                return
            }
            if (navigator.current.destination == destination) {
                preparedCaller?.dispose()
                return
            }

            val parentEntry = navigator.current
            val interruptedSibling = retainedEntries.asReversed().firstOrNull { candidate ->
                transitions.getValue(candidate.id).parentEntryId == parentEntry.id
            }
            val initialUnderlayProgress = interruptedSibling?.let { sibling ->
                transitions.getValue(sibling.id).underlayProgress.value
            } ?: 0f
            val entry = navigator.open(destination)
            preparedCaller?.takeOver()
            transitions[entry.id] = PageTransition(
                initialProgress = 0f,
                initialUnderlayProgress = initialUnderlayProgress,
                parentEntryId = parentEntry.id,
                parentLifecycle = parentLifecycle,
                caller = preparedCaller,
            )
            retainedEntries += entry
        }

        fun openFromCaller(destination: D, caller: NavigationCaller) {
            if (preparingCaller || navigator.current.destination == destination) return
            if (!continuousTransitionsEnabled) {
                open(destination)
                return
            }
            if (retainedEntries.any { entry ->
                    entry.destination == destination && transitions[entry.id]?.targetProgress == 0f &&
                            transitions[entry.id]?.callerSideExit == null
                }) {
                // Reverse the retained transition without interrupting its original caller.
                open(destination)
                return
            }
            val origin = navigator.current.id
            preparingCaller = true
            navigationScope.launch {
                try {
                    val prepared =
                        caller.prepare(hostCoordinates)
                    if (navigator.current.id != origin) prepared?.dispose()
                    else if (!continuousTransitionsEnabled) {
                        prepared?.dispose()
                        open(destination)
                    } else open(destination, prepared)
                } finally {
                    preparingCaller = false
                }
            }
        }

        fun closeTop() {
            val entry = navigator.current
            val transition = transitions[entry.id] ?: return
            if (navigator.closeTop(expectedEntryId = entry.id) == null) return

            if (!transition.composed) {
                // A batch pop can cross hidden pages. Keep the actual outgoing UI over the final
                // destination instead of composing and animating every intermediate page.
                transitions.values.forEach { candidate ->
                    if (candidate.parentEntryId == entry.id) {
                        candidate.parentEntryId = navigator.current.id
                    }
                }
                release(entry)
                return
            }
            transition.targetProgress = 0f
            transition.callerReturnStarted = false
            transition.callerReturnTargetLost = false
            transition.isTransitioning.value = true
            transition.caller?.takeOver()
        }

        fun finishReturnsTo(parentEntryId: Long) {
            val returningEntries = retainedEntries.filter { entry ->
                val transition = transitions.getValue(entry.id)
                transition.parentEntryId == parentEntryId && transition.targetProgress == 0f
            }
            returningEntries.forEach { entry ->
                finishReturnsTo(entry.id)
                release(entry)
            }
        }

        fun back() {
            if (navigator.backStack.size <= 1) return
            val entryId = navigator.current.id
            // An outgoing page can no longer return to a parent that is itself being popped.
            finishReturnsTo(entryId)
            if (navigator.current.id == entryId) closeTop()
        }

        val hostHandle = remember(navigator) {
            NavigationHostHandle(
                closeToRootInternal = { returnToCaller ->
                    if (!returnToCaller) {
                        val exitingEntries = retainedEntries.filter { entry ->
                            transitions.getValue(entry.id).targetProgress == 0f
                        }
                        val exitingIds = exitingEntries.mapTo(mutableSetOf()) { it.id }
                        transitions.values.forEach { transition ->
                            if (transition.parentEntryId in exitingIds) {
                                transition.parentEntryId = navigator.backStack.first().id
                            }
                        }
                        // A tab change removes the return destination; finish exits already in flight.
                        exitingEntries.forEach(::release)
                        retainedEntries.forEach { entry ->
                            val transition = transitions.getValue(entry.id)
                            if (transition.caller != null && transition.composed && transition.callerSideExit == null) {
                                // Freeze an interrupted reveal so switching tabs only moves it sideways.
                                transition.caller?.resumeOpen(transition.progress.value)
                                transition.callerSideExit = CallerSideExit(
                                    transition.progress.value,
                                    checkNotNull(transition.callerScaleProgress).value,
                                    transition.underlayProgress.value,
                                )
                            }
                        }
                    }
                    while (navigator.backStack.size > 1) closeTop()
                },
            )
        }
        LaunchedEffect(hostHandle) { onHostReady(hostHandle) }

        val visibleEntries = entriesToCompose()
        val visibleEntryIds = visibleEntries.map { it.id }
        val isTransitioning = transitions.values.any { it.isTransitioning.value }
        SideEffect {
            transitions.forEach { (id, transition) ->
                if (!continuousTransitionsEnabled && !transition.isTransitioning.value && transition.caller != null) {
                    // Keep the live caller until an in-flight transition finishes.
                    transition.caller?.dispose()
                    transition.caller = null
                    transition.callerScaleProgress = null
                }
                transition.lifecycleOwner.moveTo(
                    when {
                        id !in visibleEntryIds -> Lifecycle.State.CREATED
                        id == navigator.current.id && !isTransitioning -> Lifecycle.State.RESUMED
                        !isTransitioning -> Lifecycle.State.CREATED
                        else -> Lifecycle.State.STARTED
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().drawBehind {
            // Moving and shrinking pages can expose the host; settled pages own their backgrounds.
            if (isTransitioning) {
                // Match the transformed underlay, including when the top entry has left the back stack.
                val underlayId = visibleEntries.lastOrNull()?.let { transitions[it.id]?.parentEntryId }
                val background = transitions[underlayId]?.background?.value
                    ?.invoke() ?: transitionBackground
                if (background.alpha > 0f) drawRect(background)
            }
        }) {
            val entryTransitions = visibleEntries.map { entry -> transitions.getValue(entry.id) }
            val underlayDrivers = mutableMapOf<Long, PageTransition>()
            visibleEntries.asReversed().forEach { candidate ->
                val candidateTransition = transitions.getValue(candidate.id)
                val parentEntryId = candidateTransition.parentEntryId ?: return@forEach
                if (parentEntryId !in underlayDrivers) {
                    underlayDrivers[parentEntryId] = candidateTransition
                }
            }

            visibleEntries.forEachIndexed { index, entry ->
                key(entry.id) {
                    val isRoot = entry.id == initialBackStack.first().id
                    val transition = entryTransitions[index]
                    val underlayDriver = underlayDrivers[entry.id]
                    val callerSideExit = transition.callerSideExit
                    val underlayReturnsToCaller =
                        underlayDriver?.caller != null && underlayDriver.callerSideExit == null
                    val pageViewModelStoreOwner = stores.owner(entry.id)

                    DisposableEffect(transition) {
                        transition.composed = true
                        onDispose {
                            transition.composed = false
                            transition.ready = false
                        }
                    }

                    LaunchedEffect(transition, transition.targetProgress, callerSideExit, transition.callerReturnTargetLost) {
                        // Sibling disposal must not restart an animation already in flight. Wait
                        // once for this page and its underlay; disposal expires their ready signals.
                        snapshotFlow {
                            transition.ready &&
                                    (transition.targetProgress == 0f || transition.contentReady.value) &&
                                    (transition.targetProgress != 0f ||
                                            transitions[transition.parentEntryId]?.contentReady?.value != false) &&
                                    (transitions[transition.parentEntryId]?.ready != false)
                        }.first { it }

                        val targetProgress = transition.targetProgress
                        if (targetProgress == 0f && transition.callerReturnTargetLost) {
                            transition.progress.snapTo(0f)
                            transition.callerScaleProgress?.snapTo(0f)
                            transition.underlayProgress.snapTo(0f)
                            transition.isTransitioning.value = false
                            transition.callerReturnStarted = false
                            transition.caller?.release()
                            // Hide immediately, but keep an existing shared hit path until the drag ends.
                            snapshotFlow {
                                !transition.callerGestureActive || transition.targetProgress != targetProgress
                            }.first { it }
                            if (transition.targetProgress == targetProgress && transitions[entry.id] === transition) {
                                release(entry)
                            }
                            return@LaunchedEffect
                        }
                        if (targetProgress == 0f && callerSideExit == null) {
                            // The parent may have been recreated; its caller coordinates are ready only now.
                            transition.caller?.let { caller ->
                                if (!caller.hasVisibleCaller()) {
                                    // Placement readiness can precede global-position callbacks for lazy items.
                                    withFrameNanos { }
                                }
                                if (!caller.hasVisibleCaller() || !caller.prepareReturn()) {
                                    transition.callerSideExit = CallerSideExit(
                                        transition.progress.value,
                                        checkNotNull(transition.callerScaleProgress).value,
                                        transition.underlayProgress.value,
                                    )
                                    return@LaunchedEffect
                                }
                                caller.beginReturn(transition.progress.value)
                                transition.callerReturnStarted = true
                            }
                        }
                        if (
                            transition.progress.value != targetProgress ||
                            transition.callerScaleProgress?.let { it.value != targetProgress } == true ||
                            callerSideExit?.progress?.value?.let { it != 1f } == true ||
                            transition.underlayProgress.value != targetProgress
                        ) {
                            coroutineScope {
                                launch {
                                    if (callerSideExit != null) {
                                        callerSideExit.progress.animateTo(1f, resolvedAnimationSpec)
                                    } else transition.progress.animateTo(
                                        targetValue = targetProgress,
                                        animationSpec = if (transition.caller != null) {
                                            if (targetProgress == 0f) callerReturnTranslationSpec
                                            else callerEnterTranslationSpec
                                        } else resolvedAnimationSpec,
                                    )
                                }
                                transition.callerScaleProgress?.let { scaleProgress ->
                                    if (callerSideExit == null) launch {
                                        scaleProgress.animateTo(
                                            targetValue = targetProgress,
                                            animationSpec = if (targetProgress == 0f) callerReturnScaleSpec
                                            else callerEnterScaleSpec,
                                        )
                                    }
                                }
                                launch {
                                    transition.underlayProgress.animateTo(
                                        targetValue = targetProgress,
                                        animationSpec = resolvedAnimationSpec,
                                    )
                                }
                            }
                            if (callerSideExit == null) {
                                transition.progress.snapTo(targetProgress)
                                transition.callerScaleProgress?.snapTo(targetProgress)
                            }
                            transition.underlayProgress.snapTo(targetProgress)
                        }
                        if (targetProgress == 0f) {
                            // Removing a shared hit path mid-drag also cancels the underlay's scrolling.
                            // Keep it alive until every participating pointer is released or cancelled.
                            snapshotFlow {
                                !transition.callerGestureActive || transition.targetProgress != targetProgress
                            }.first { it }
                        }
                        if (transition.targetProgress != targetProgress || transitions[entry.id] !== transition) return@LaunchedEffect

                        transition.isTransitioning.value = false
                        transition.caller?.release()
                        if (targetProgress == 0f && (callerSideExit != null || transition.progress.value == 0f)) {
                            release(entry)
                        }
                    }

                    LaunchedEffect(transition, transition.targetProgress, callerSideExit, transition.callerReturnStarted) {
                        if (transition.targetProgress != 0f || callerSideExit != null || !transition.callerReturnStarted) {
                            return@LaunchedEffect
                        }
                        val caller = transition.caller ?: return@LaunchedEffect
                        snapshotFlow { caller.hasVisibleCaller() }.first { !it }
                        if (transition.targetProgress == 0f && transitions[entry.id] === transition) {
                            transition.callerReturnTargetLost = true
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationPageReady { transition.ready = true }
                            .graphicsLayer {
                                alpha = if (transition.callerReturnTargetLost) 0f else 1f
                                val ownTranslation = if (callerSideExit != null) {
                                    hostWidthPx * callerSideExit.progress.value
                                } else if (isRoot || transition.caller != null) {
                                    0f
                                } else {
                                    hostWidthPx * (1f - transition.progress.value)
                                }
                                translationX =
                                    ownTranslation + if (underlayReturnsToCaller) {
                                        0f
                                    } else {
                                        underlayTranslation(
                                            hostWidthPx,
                                            underlayDriver?.underlayProgress?.value ?: 0f,
                                        )
                                    }
                                val scale = if (underlayReturnsToCaller) {
                                    1f - (1f - callerUnderlayScale) *
                                            checkNotNull(underlayDriver).underlayProgress.value.coerceIn(
                                                0f,
                                                1f
                                            )
                                } else 1f
                                scaleX = scale
                                scaleY = scale
                            }
                            .graphicsLayer {
                                // Resolve the page at its original size before the outer layer transforms it.
                                compositingStrategy = if (
                                    transition.isTransitioning.value ||
                                    underlayDriver?.isTransitioning?.value == true
                                ) {
                                    CompositingStrategy.Offscreen
                                } else {
                                    CompositingStrategy.Auto
                                }
                            }
                            .cornerMaskClipInLayer(
                                shape = pageShape,
                                enabled = transition.caller == null && transition.isTransitioning.value,
                            )
                            .then(transition.caller?.let {
                                rememberNavigationCallerTransition(
                                    source = it,
                                    progress = callerSideExit?.pageProgress
                                        ?: transition.progress.asState(),
                                    scaleProgress = callerSideExit?.scaleProgress ?: checkNotNull(
                                        transition.callerScaleProgress
                                    ).asState(),
                                    clipProgress = callerSideExit?.clipProgress
                                        ?: transition.underlayProgress.asState(),
                                    isTransitioning = transition.isTransitioning,
                                    pageShape = pageShape,
                                    returning = transition.targetProgress == 0f && callerSideExit == null,
                                    gestureActive = transition.callerGestureActive,
                                    followDistance = callerFollowDistance,
                                )
                            } ?: Modifier)
                            .then(
                                if (transition.caller != null && transition.targetProgress == 0f &&
                                    callerSideExit == null
                                ) {
                                    Modifier.navigationCallerReturnInput(
                                        onGestureActiveChanged = {
                                            transition.callerGestureActive = it
                                        },
                                    )
                                } else Modifier,
                            )
                            .zIndex(index.toFloat()),
                    ) {
                        if (!isRoot) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(entry.id) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitPointerEvent()
                                            }
                                        }
                                    },
                            )
                        }

                        pageStateHolder.SaveableStateProvider(key = entry.id) {
                            CompositionLocalProvider(
                                LocalPageBackground provides transition.background,
                                LocalNavigationContentReady provides transition.contentReady,
                                LocalNavigationCallers provides transition.callers,
                                LocalLifecycleOwner provides transition.lifecycleOwner,
                                LocalViewModelStoreOwner provides pageViewModelStoreOwner,
                                // Remembered: a fresh instance every recomposition would invalidate
                                // the whole page under a static local.
                                LocalPageTransitionState provides remember(transition) {
                                    PageTransitionState(
                                        // `Animatable` is no longer a `State` (animation-core 1.12);
                                        // `asState()` is the only way to hand it out read-only.
                                        progress = transition.progress.asState(),
                                        isTransitioning = transition.isTransitioning,
                                        slideDistancePx = hostWidthPx.toFloat(),
                                    )
                                },
                            ) {
                                if (!isRoot) {
                                    BackHandler(
                                        enabled = entry.id == navigator.current.id,
                                        onBack = ::back,
                                    )
                                }
                                NavigationScope(
                                    entry = entry,
                                    // Live caller pages remain composed below the top entry.
                                    navigateTo = { if (entry.id == navigator.current.id) open(it) },
                                    goBack = { if (entry.id == navigator.current.id) back() },
                                    navigateFromCaller = { destination, caller ->
                                        if (entry.id == navigator.current.id) openFromCaller(destination, caller)
                                    },
                                    finishExits = {
                                        if (entry.id == navigator.current.id) finishReturnsTo(entry.id)
                                    },
                                ).content()
                            }
                        }

                        if (underlayDriver != null && !underlayReturnsToCaller && scrimAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = underlayDriver.underlayProgress.value
                                    }
                                    .background(scrimColor.copy(alpha = scrimAlpha)),
                            )
                        }
                        if (transition.caller != null && transition.targetProgress == 0f) {
                            // Block the exiting page's own controls without consuming the shared gesture.
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .pointerInput(entry.id) {
                                        awaitPointerEventScope {
                                            while (true) awaitPointerEvent()
                                        }
                                    },
                            )
                        }
                    }
                    // A retained caller can outlive its parent UI after the entrance settles.
                    if (index > 0 && transition.caller != null && callerSideExit == null && scrimAlpha > 0f &&
                        underlayDrivers[transition.parentEntryId] === transition
                    ) {
                        // The active sibling's scrim must cover older exits as well as their shared parent.
                        Box(
                            Modifier
                                .fillMaxSize()
                                .zIndex(index - 0.5f)
                                .graphicsLayer {
                                    alpha = transition.underlayProgress.value.coerceIn(0f, 1f)
                                }
                                .background(scrimColor.copy(alpha = scrimAlpha)),
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.navigationPageReady(onReady: () -> Unit): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
            onReady()
        }
    }
