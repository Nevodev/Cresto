package com.nevoit.cresto.feature.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceNode
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.EmittableCheckBox
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.nevoit.cresto.R

@Composable
fun CheckBox(
    checked: Boolean,
    onCheckedChange: Action?,
    modifier: GlanceModifier = GlanceModifier,
    checkedColor: ColorProvider,
    uncheckedColor: ColorProvider
) {
    val colors = CheckboxDefaults.colors(
        checkedColor = checkedColor,
        uncheckedColor = uncheckedColor
    )
    val finalModifier = onCheckedChange?.let { action ->
        modifier
            .size(24.dp)
            .clickable(
                onClick = action,
                rippleOverride = R.drawable.widget_ripple
            )
    } ?: modifier.size(24.dp)

    @Suppress("RestrictedApi")
    GlanceNode(
        factory = { EmittableCheckBox(colors) },
        update = {
            this.set(checked) { this.checked = it }
            this.set(finalModifier) { this.modifier = it }
            this.set(colors) { this.colors = it }
            this.set("") { this.text = it }
            this.set(1) { this.maxLines = it }
        }
    )
}
