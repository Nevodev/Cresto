package com.nevoit.cresto.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.nevoit.cresto.feature.calendar.MonthlyPagerCalendar
import com.nevoit.cresto.theme.GlasenseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarPagerStateTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun restoringKeepsTheBrowsedMonthAndChangingSelectionStillNavigates() {
        val restoration = StateRestorationTester(compose)
        val selected = mutableStateOf(LocalDate.of(2026, 11, 21))
        var displayed = YearMonth.from(selected.value)
        restoration.setContent {
            GlasenseTheme {
                MonthlyPagerCalendar(
                    modifier = Modifier.testTag("calendar"),
                    selectedDate = selected.value,
                    onDateSelected = { selected.value = it },
                    onMonthChanged = { displayed = it },
                    collapseFractionProvider = { 0f },
                    initialDate = LocalDate.of(2026, 11, 21),
                )
            }
        }
        compose.onNodeWithTag("calendar").performTouchInput {
            swipe(Offset(width * 0.85f, height / 2f), Offset(width * 0.15f, height / 2f), 500)
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(YearMonth.of(2026, 12), displayed) }

        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(YearMonth.of(2026, 12), displayed)
            selected.value = LocalDate.of(2027, 2, 3)
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(YearMonth.of(2027, 2), displayed) }
    }
}
