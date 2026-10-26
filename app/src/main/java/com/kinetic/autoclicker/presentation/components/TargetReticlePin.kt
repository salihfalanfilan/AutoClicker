package com.kinetic.autoclicker.presentation.components

import android.view.MotionEvent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kinetic.autoclicker.domain.model.ActionType
import com.kinetic.autoclicker.presentation.theme.LocalAppStrings
import com.kinetic.autoclicker.presentation.theme.LocalKineticPalette
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TargetReticlePin(
    orderIndex: Int,
    actionType: ActionType = ActionType.TAP,
    isActive: Boolean = false,
    isPulsing: Boolean = false,
    isSelected: Boolean = false,
    showDragHint: Boolean = false,
    onTap: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onDrag: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current

    val currentIsActive by rememberUpdatedState(isActive)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDrag by rememberUpdatedState(onDrag)

    val disallowIntercept = remember { RequestDisallowInterceptTouchEvent() }
    var downRawX by remember { mutableFloatStateOf(0f) }
    var downRawY by remember { mutableFloatStateOf(0f) }
    var lastRawX by remember { mutableFloatStateOf(0f) }
    var lastRawY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Sadece gerçek tıklama (isPulsing) anında anlık dokunma tepkisi (sahte sürekli animasyon yok)
    val clickScale by animateFloatAsState(
        targetValue = if (isPulsing || isDragging) 0.90f else 1.0f,
        animationSpec = tween(durationMillis = 90),
        label = "click_scale"
    )

    // Gerçek dokunma gerçekleştiğinde oluşan anlık genişleyen halka
    val tapRippleScale by animateFloatAsState(
        targetValue = if (isPulsing) 1.35f else 1.0f,
        animationSpec = tween(durationMillis = 180),
        label = "tap_ripple_scale"
    )
    val tapRippleAlpha by animateFloatAsState(
        targetValue = if (isPulsing) 0.60f else 0.0f,
        animationSpec = tween(durationMillis = 180),
        label = "tap_ripple_alpha"
    )

    // Tıklama (TAP) her zaman Birincil Renk (#299EEC), Kaydırma (SWIPE) her zaman İkincil Renk (#9449DB)
    val ringColor = if (actionType == ActionType.SWIPE) palette.secondary else palette.primary
    val fillTint = ringColor.copy(alpha = if (isDragging) 0.36f else 0.20f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .pointerInteropFilter(requestDisallowInterceptTouchEvent = disallowIntercept) { event ->
                if (currentIsActive) {
                    isDragging = false
                    return@pointerInteropFilter false
                }
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        disallowIntercept.invoke(true)
                        downRawX = event.rawX
                        downRawY = event.rawY
                        lastRawX = event.rawX
                        lastRawY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val totalDist = hypot(event.rawX - downRawX, event.rawY - downRawY)
                        if (!isDragging && totalDist > 6f) {
                            isDragging = true
                        }
                        if (isDragging) {
                            val dx = event.rawX - lastRawX
                            val dy = event.rawY - lastRawY
                            lastRawX = event.rawX
                            lastRawY = event.rawY
                            if (dx != 0f || dy != 0f) {
                                currentOnDrag(dx, dy)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        disallowIntercept.invoke(false)
                        if (!isDragging) {
                            currentOnTap()
                        }
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        disallowIntercept.invoke(false)
                        isDragging = false
                        true
                    }
                    else -> false
                }
            }
    ) {
        if (showDragHint) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(palette.surfaceContainerHighest.copy(alpha = 0.95f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.TouchApp,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = strings.dragToAdjustPosition,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .scale(clickScale)
        ) {
            // Gerçek Anlık Dokunma Tepkisi (Sadece gerçek tıklama anında görünür, boşta dururken asla sahte nabız atmaz)
            if (tapRippleAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(tapRippleScale)
                        .clip(CircleShape)
                        .background(ringColor.copy(alpha = tapRippleAlpha))
                )
            }

            // High-Contrast Target Boundary Ring (48dp core)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .shadow(
                        elevation = if (isSelected) 16.dp else 10.dp,
                        shape = CircleShape,
                        ambientColor = ringColor,
                        spotColor = ringColor
                    )
                    .clip(CircleShape)
                    .background(fillTint)
                    .border(
                        width = if (isSelected) 3.dp else 2.dp,
                        color = if (isSelected) palette.onSurface else ringColor,
                        shape = CircleShape
                    )
            ) {
                Canvas(modifier = Modifier.size(30.dp)) {
                    val strokePx = 2.dp.toPx()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.36f

                    drawCircle(
                        color = ringColor.copy(alpha = 0.85f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokePx)
                    )

                    drawLine(
                        color = ringColor,
                        start = Offset(center.x, size.height * 0.08f),
                        end = Offset(center.x, size.height * 0.28f),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = ringColor,
                        start = Offset(center.x, size.height * 0.72f),
                        end = Offset(center.x, size.height * 0.92f),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = ringColor,
                        start = Offset(size.width * 0.08f, center.y),
                        end = Offset(size.width * 0.28f, center.y),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = ringColor,
                        start = Offset(size.width * 0.72f, center.y),
                        end = Offset(size.width * 0.92f, center.y),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )

                    drawCircle(
                        color = ringColor,
                        radius = 2.5.dp.toPx(),
                        center = center
                    )
                }
            }

            // Sequential Order Badge Attached To Top-Right Corner
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(22.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (actionType == ActionType.SWIPE) palette.secondary else palette.primary)
            ) {
                Text(
                    text = if (actionType == ActionType.SWIPE) "${orderIndex}A" else orderIndex.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (actionType == ActionType.SWIPE) palette.onSecondary else palette.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick Delete Badge on Top-Left Corner when Target is Selected
            if (isSelected && onDelete != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(22.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(palette.error)
                        .clickable(onClick = onDelete)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Hedefi Sil",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * Draggable End-Point Pin (#1B) for Swipe gestures so the user can independently drag
 * where the swipe ends on the screen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SwipeEndReticlePin(
    orderIndex: Int,
    isActive: Boolean = false,
    onTap: () -> Unit = {},
    onDrag: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val palette = LocalKineticPalette.current
    val currentIsActive by rememberUpdatedState(isActive)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDrag by rememberUpdatedState(onDrag)

    val disallowIntercept = remember { RequestDisallowInterceptTouchEvent() }
    var downRawX by remember { mutableFloatStateOf(0f) }
    var downRawY by remember { mutableFloatStateOf(0f) }
    var lastRawX by remember { mutableFloatStateOf(0f) }
    var lastRawY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val ringColor = palette.secondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(56.dp)
            .pointerInteropFilter(requestDisallowInterceptTouchEvent = disallowIntercept) { event ->
                if (currentIsActive) {
                    isDragging = false
                    return@pointerInteropFilter false
                }
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        disallowIntercept.invoke(true)
                        downRawX = event.rawX
                        downRawY = event.rawY
                        lastRawX = event.rawX
                        lastRawY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val totalDist = hypot(event.rawX - downRawX, event.rawY - downRawY)
                        if (!isDragging && totalDist > 6f) {
                            isDragging = true
                        }
                        if (isDragging) {
                            val dx = event.rawX - lastRawX
                            val dy = event.rawY - lastRawY
                            lastRawX = event.rawX
                            lastRawY = event.rawY
                            if (dx != 0f || dy != 0f) {
                                currentOnDrag(dx, dy)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        disallowIntercept.invoke(false)
                        if (!isDragging) {
                            currentOnTap()
                        }
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        disallowIntercept.invoke(false)
                        isDragging = false
                        true
                    }
                    else -> false
                }
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = ringColor,
                    spotColor = ringColor
                )
                .clip(CircleShape)
                .background(ringColor.copy(alpha = if (isDragging) 0.40f else 0.25f))
                .border(width = 2.dp, color = ringColor, shape = CircleShape)
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val strokePx = 2.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = ringColor,
                    radius = size.minDimension * 0.22f,
                    center = center
                )
                drawCircle(
                    color = ringColor.copy(alpha = 0.85f),
                    radius = size.minDimension * 0.42f,
                    center = center,
                    style = Stroke(width = strokePx)
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .size(22.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(ringColor)
        ) {
            Text(
                text = "${orderIndex}B",
                style = MaterialTheme.typography.labelSmall,
                color = palette.onSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Draws a directional arrow line from [start] to [end] for Swipe gestures,
 * with flowing animated dash and an authentic gliding touch particle when [isEngineRunning] is active.
 */
@Composable
fun SwipeDirectionLineCanvas(
    swipePairs: List<Pair<Offset, Offset>>,
    isEngineRunning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalKineticPalette.current
    val lineColor = palette.secondary

    // Akıcı kaydırma hissi için animasyonlar (sadece motor çalışırken akar)
    val transition = rememberInfiniteTransition(label = "swipe_glide")
    val glideProgress by if (isEngineRunning && swipePairs.isNotEmpty()) {
        transition.animateFloat(
            initialValue = 0.05f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "glide_progress"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val dashPhase by if (isEngineRunning && swipePairs.isNotEmpty()) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = -28f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "dash_phase"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), dashPhase)

        swipePairs.forEach { (start, end) ->
            val dx = end.x - start.x
            val dy = end.y - start.y
            val dist = hypot(dx, dy)
            if (dist > 12f) {
                // Temel yön çizgisi
                drawLine(
                    color = lineColor.copy(alpha = if (isEngineRunning) 0.90f else 0.70f),
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                    pathEffect = dashEffect
                )

                val angle = atan2(dy, dx)
                val arrowSize = 14.dp.toPx()
                val midX = (start.x + end.x) / 2f
                val midY = (start.y + end.y) / 2f

                // Yön gösterici ok
                val arrowPath = Path().apply {
                    moveTo(
                        midX + arrowSize * 0.6f * cos(angle),
                        midY + arrowSize * 0.6f * sin(angle)
                    )
                    lineTo(
                        midX - arrowSize * 0.6f * cos(angle - 0.65f),
                        midY - arrowSize * 0.6f * sin(angle - 0.65f)
                    )
                    lineTo(
                        midX - arrowSize * 0.6f * cos(angle + 0.65f),
                        midY - arrowSize * 0.6f * sin(angle + 0.65f)
                    )
                    close()
                }
                drawPath(path = arrowPath, color = lineColor)

                // Kaydırma Hissini Veren Akıcı Parmak Dokunma Efekti (Gliding Touch Point & Trail)
                if (isEngineRunning && glideProgress > 0f) {
                    val touchX = start.x + dx * glideProgress
                    val touchY = start.y + dy * glideProgress
                    val touchRadius = 14.dp.toPx()

                    // Dış parlama halkası
                    drawCircle(
                        color = lineColor.copy(alpha = 0.35f),
                        radius = touchRadius,
                        center = Offset(touchX, touchY)
                    )
                    // İç dokunma çekirdeği (parmak temas noktası)
                    drawCircle(
                        color = lineColor,
                        radius = touchRadius * 0.45f,
                        center = Offset(touchX, touchY)
                    )

                    // Arkasında hafif hareket izi (motion tail)
                    val tailFraction = (glideProgress - 0.15f).coerceAtLeast(0f)
                    val tailX = start.x + dx * tailFraction
                    val tailY = start.y + dy * tailFraction
                    drawLine(
                        color = lineColor.copy(alpha = 0.5f),
                        start = Offset(tailX, tailY),
                        end = Offset(touchX, touchY),
                        strokeWidth = strokeWidth * 2f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeTrailIndicator(
    modifier: Modifier = Modifier,
    tint: Color = LocalKineticPalette.current.secondary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.7f))
        )
        Box(
            modifier = Modifier
                .width(8.dp)
                .size(height = 2.dp, width = 8.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.7f))
        )
        Box(
            modifier = Modifier
                .width(16.dp)
                .size(height = 2.dp, width = 16.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.7f))
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = tint.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
        )
    }
}
