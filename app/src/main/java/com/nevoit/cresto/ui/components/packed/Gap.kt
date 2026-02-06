package com.nevoit.cresto.ui.components.packed

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VGap() {
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun HGap() {
    Spacer(modifier = Modifier.width(12.dp))
}