package com.nevoit.cresto.ui.components.bottomsheet

import androidx.annotation.StringRes
import com.nevoit.cresto.R

enum class RepeatOption(
    @StringRes val labelRes: Int,
    val rrule: String?
) {
    NONE(R.string.repeat_none, null),
    DAILY(R.string.repeat_daily, "FREQ=DAILY;INTERVAL=1"),
    WEEKLY(R.string.repeat_weekly, "FREQ=WEEKLY;INTERVAL=1"),
    MONTHLY(R.string.repeat_monthly, "FREQ=MONTHLY;INTERVAL=1"),
    YEARLY(R.string.repeat_yearly, "FREQ=YEARLY;INTERVAL=1");

    companion object {
        val selectableEntries: List<RepeatOption> = entries.filterNot { it == NONE }
    }
}
