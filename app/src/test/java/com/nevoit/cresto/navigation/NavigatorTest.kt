package com.nevoit.cresto.navigation

import androidx.compose.runtime.saveable.SaverScope
import com.nevoit.cresto.feature.settings.SettingsDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigatorTest {
    private val saver = navigatorSaver<AppDestination>(AppDestination.Main, AppDestination.serializer())
    private val saverScope = SaverScope { true }

    @Test
    fun repeatedDestinationsHaveIndependentIdsAndRootCannotBePopped() {
        val navigator = Navigator<AppDestination>(AppDestination.Main)
        val first = navigator.open(AppDestination.Detail(42))
        val second = navigator.open(AppDestination.Detail(42))

        assertNotEquals(first.id, second.id)
        assertEquals(second, navigator.closeTop(second.id))
        assertNull(navigator.closeTop(second.id))
        assertEquals(first, navigator.current)
        assertEquals(first, navigator.closeTop(first.id))
        assertNull(navigator.closeTop())
        assertEquals(AppDestination.Main, navigator.current.destination)
    }

    @Test
    fun savedStackRestoresRoutesArgumentsAndNextIdentity() {
        val navigator = Navigator<AppDestination>(AppDestination.Main)
        navigator.open(AppDestination.Detail(42))
        navigator.open(AppDestination.Settings(SettingsDestination.APPEARANCE))
        val removed = navigator.open(AppDestination.RecentlyDeleted)
        navigator.closeTop()

        val saved = with(saver) { saverScope.save(navigator) }!!
        val restored = saver.restore(saved)!!

        assertEquals(navigator.backStack, restored.backStack)
        assertEquals(removed.id + 1, restored.open(AppDestination.Detail(43)).id)
    }

    @Test
    fun corruptedOrIncompatibleSavedStacksAreRejected() {
        val navigator = Navigator<AppDestination>(AppDestination.Main)
        navigator.open(AppDestination.Detail(42))
        val saved = (with(saver) { saverScope.save(navigator) }!! as List<*>).map { requireNotNull(it) }

        assertNull(saver.restore(emptyList<Any>()))
        assertNull(saver.restore(saved.dropLast(1)))
        assertNull(saver.restore(saved.toMutableList().apply { this[2] = "invalid json" }))
        assertNull(saver.restore(saved.toMutableList().apply { this[3] = this[1] }))
        assertNull(saver.restore(saved.toMutableList().apply { this[0] = 0L }))
        val differentRoot = navigatorSaver<AppDestination>(
            AppDestination.Settings(), AppDestination.serializer(),
        )
        assertNull(differentRoot.restore(saved))
    }
}
