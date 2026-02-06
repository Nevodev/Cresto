package com.nevoit.cresto.ui.components.packed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.nevoit.cresto.R
import com.nevoit.cresto.data.todo.liveactivity.FoodPickupPayload
import com.nevoit.cresto.data.todo.liveactivity.FoodPickupType
import com.nevoit.cresto.data.todo.liveactivity.LiveActivityEntity
import com.nevoit.cresto.data.todo.liveactivity.ParcelPickupPayload
import com.nevoit.cresto.ui.components.glasense.GlasenseButtonCompact
import com.nevoit.cresto.ui.components.glasense.ZeroHeightDivider
import com.nevoit.cresto.ui.theme.glasense.AppButtonColors
import com.nevoit.cresto.ui.theme.glasense.AppColors
import com.nevoit.cresto.ui.theme.glasense.AppSpecs
import com.nevoit.cresto.util.formatRelativeRealTime
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

val FoodPickupType.iconResId: Int
    @DrawableRes
    get() = when (this) {
        FoodPickupType.la_unspecified -> R.drawable.la_unspecified
        FoodPickupType.la_takeaway -> R.drawable.la_takeaway
        FoodPickupType.la_hot -> R.drawable.la_hot
        FoodPickupType.la_lemonade -> R.drawable.la_lemonade
        FoodPickupType.la_iced_ganlu -> R.drawable.la_iced_ganlu
        FoodPickupType.la_iced_grape -> R.drawable.la_iced_grape
        FoodPickupType.la_iced_mango -> R.drawable.la_iced_mango
        FoodPickupType.la_iced_americano -> R.drawable.la_iced_americano
        FoodPickupType.la_iced_latte -> R.drawable.la_iced_latte
        FoodPickupType.la_iced_tea -> R.drawable.la_iced_tea
        FoodPickupType.la_iced_milktea -> R.drawable.la_iced_milktea
        FoodPickupType.la_iced_lemon_tea -> R.drawable.la_iced_lemon_tea
        FoodPickupType.la_iced_matcha_latte -> R.drawable.la_iced_matcha_latte
        FoodPickupType.la_iced_bubble_tea -> R.drawable.la_iced_bubble_tea
    }

@Composable
fun LiveActivityItem(
    entity: LiveActivityEntity,
    modifier: Modifier = Modifier,
    onDone: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppColors.cardBackground,
                shape = ContinuousRoundedRectangle(AppSpecs.cardCorner * 2)
            )
            .padding(12.dp)
    ) {
        when (entity.content) {
            is FoodPickupPayload -> FoodPickupItem(
                payload = entity.content,
                onDone = onDone,
                time = entity.createTime
            )

            is ParcelPickupPayload -> ParcelPickupItem(payload = entity.content)
        }
    }
}

@Composable
private fun FoodPickupItem(
    payload: FoodPickupPayload,
    onDone: () -> Unit,
    time: LocalDateTime
) {
    val context = LocalContext.current
    var timeString by remember(time, context) {
        mutableStateOf(
            formatRelativeRealTime(
                time,
                context
            )
        )
    }

    LaunchedEffect(time) {
        while (true) {
            timeString = formatRelativeRealTime(time, context)

            val now = LocalDateTime.now()
            val diffSeconds = ChronoUnit.SECONDS.between(time, now)

            if (diffSeconds <= 300) {
                delay(30_000L)
            } else {
                delay(60_000L)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(payload.foodType.iconResId),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        HGap()
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payload.storeName,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.contentVariant
                    )
                    Text(
                        text = payload.pickupCode,
                        fontSize = 24.sp
                    )
                }
                GlasenseButtonCompact(
                    onClick = onDone,
                    colors = AppButtonColors.secondary(),
                    padding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = "确认取餐",
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.contentVariant
                    )
                }
            }
            VGap()
            ZeroHeightDivider()
            VGap()
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = payload.foodName,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.contentVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = timeString,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    color = AppColors.contentVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun BoxScope.ParcelPickupItem(
    payload: ParcelPickupPayload
) {

}