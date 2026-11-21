package com.nevoit.cresto.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/** A page in the navigator back stack with a stable identity for saved state. */
@Immutable
data class PageEntry<out D : Any>(
    val id: Long,
    val destination: D,
)

/**
 * A small stack-based navigator for a single Compose host.
 *
 * The entry id is independent from the destination value. This lets the host keep each page's
 * saveable state and `ViewModel` store while a page is covered, and release both when the page is
 * removed from the stack.
 */
@Stable
class Navigator<D : Any> internal constructor(
    initialBackStack: List<PageEntry<D>>,
    nextEntryId: Long,
) {
    constructor(root: D) : this(
        initialBackStack = listOf(PageEntry(id = 0L, destination = root)),
        nextEntryId = 1L,
    )

    /** The current root-to-top page entries. */
    var backStack by mutableStateOf(initialBackStack.toList())
        private set

    internal var nextEntryId = nextEntryId
        private set

    /** The page currently shown above every other entry. */
    val current: PageEntry<D>
        get() = backStack.last()

    init {
        require(initialBackStack.isNotEmpty()) { "A navigator must have a root page" }
        require(initialBackStack.map(PageEntry<D>::id).distinct().size == initialBackStack.size) {
            "Every page entry must have a unique id"
        }
        require(nextEntryId > initialBackStack.maxOf(PageEntry<D>::id)) {
            "The next page id must be greater than every restored id"
        }
    }

    /** Pushes [destination] as a new page and returns its stable entry. */
    fun open(destination: D): PageEntry<D> {
        val entry = PageEntry(
            id = nextEntryId++,
            destination = destination,
        )
        backStack = backStack + entry
        return entry
    }

    internal fun reopen(entry: PageEntry<D>) {
        require(backStack.none { it.id == entry.id }) {
            "A reopened page entry must not already be in the back stack"
        }
        backStack = backStack + entry
    }

    /**
     * Removes the current page unless it is the root or the top entry changed meanwhile.
     * Returns the removed entry when a close was accepted.
     */
    fun closeTop(expectedEntryId: Long = current.id): PageEntry<D>? {
        if (backStack.size == 1 || current.id != expectedEntryId) return null

        val removed = current
        backStack = backStack.dropLast(1)
        return removed
    }
}

/**
 * JSON is the portable encoding of a saved stack: `rememberSaveable` hands its values to the
 * platform, and only primitives and strings are guaranteed to survive that trip on every target.
 */
private val NavigatorJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Creates the default [Saver] for a destination back stack.
 *
 * The saved form starts with the next entry id, followed by an id and an encoded destination per entry.
 */
internal fun <D : Any> navigatorSaver(
    root: D,
    destinationSerializer: KSerializer<D>,
): Saver<Navigator<D>, Any> = listSaver(
    save = { navigator ->
        buildList {
            add(navigator.nextEntryId)
            navigator.backStack.forEach { entry ->
                add(entry.id)
                add(NavigatorJson.encodeToString(destinationSerializer, entry.destination))
            }
        }
    },
    restore = { saved ->
        restoreNavigator(
            saved = saved,
            root = root,
            destinationSerializer = destinationSerializer,
        )
    },
)

/**
 * Creates and remembers a [Navigator], restoring its back stack after the host is recreated.
 * [root] must remain the first destination for saved data to be accepted.
 *
 * @param destinationSerializer the serializer for the sealed destination type, used to save and
 * restore the stack.
 */
@Composable
fun <D : Any> rememberNavigator(
    root: D,
    destinationSerializer: KSerializer<D>,
): Navigator<D> = rememberSaveable(
    saver = navigatorSaver(root, destinationSerializer),
) {
    Navigator(root)
}

/**
 * [rememberNavigator] for a `@Serializable` destination type, which is the common case:
 * `rememberNavigator<Destination>(Destination.Login)`.
 */
@Composable
inline fun <reified D : Any> rememberNavigator(root: D): Navigator<D> =
    rememberNavigator(root, serializer<D>())

private fun <D : Any> restoreNavigator(
    saved: List<Any>,
    root: D,
    destinationSerializer: KSerializer<D>,
): Navigator<D>? {
    if (saved.isEmpty() || (saved.size - 1) % 2 != 0) return null

    val nextEntryId = saved.firstOrNull() as? Long ?: return null
    val entries = saved
        .drop(1)
        .chunked(2)
        .map { encodedEntry ->
            val id = encodedEntry[0] as? Long ?: return null
            val encodedDestination = encodedEntry[1] as? String ?: return null
            // A destination whose shape changed since it was saved is not worth failing over: the
            // whole stack is dropped and the root is recreated instead.
            val destination = runCatching {
                NavigatorJson.decodeFromString(destinationSerializer, encodedDestination)
            }.getOrNull() ?: return null
            PageEntry(id = id, destination = destination)
        }

    if (entries.firstOrNull()?.destination != root) return null
    if (entries.drop(1).any { it.destination == root }) return null

    return runCatching {
        Navigator(
            initialBackStack = entries,
            nextEntryId = nextEntryId,
        )
    }.getOrNull()
}

/** Navigation callbacks and the destination for one rendered page. */
@Stable
class NavigationScope<D : Any> internal constructor(
    val entry: PageEntry<D>,
    private val navigateTo: (D) -> Unit,
    private val goBack: () -> Unit,
    private val navigateFromCaller: ((D, NavigationCaller) -> Unit)? = null,
    private val finishExits: () -> Unit = {},
) {
    /** The destination represented by the page currently rendering this scope. */
    val destination: D
        get() = entry.destination

    /** Pushes a destination onto the host's back stack. */
    fun navigate(destination: D) {
        navigateTo(destination)
    }

    fun navigate(destination: D, caller: NavigationCaller) {
        val open = navigateFromCaller
        if (open != null) open(destination, caller) else navigateTo(destination)
    }

    /** Requests a back navigation. The root page ignores this request. */
    fun back() {
        goBack()
    }

    /** Releases pages returning to this page before its tab content changes. */
    fun finishExitAnimations() {
        finishExits()
    }
}
