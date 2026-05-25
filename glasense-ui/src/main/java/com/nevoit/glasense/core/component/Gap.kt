package com.nevoit.glasense.core.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VGap(gap: Dp = 12.dp) {
    Spacer(modifier = Modifier.height(gap))
}

@Composable
fun HGap(gap: Dp = 12.dp) {
    Spacer(modifier = Modifier.width(gap))
}
