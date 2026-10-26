package com.kinetic.autoclicker.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kinetic.autoclicker.R
import com.kinetic.autoclicker.domain.model.ActionType
import com.kinetic.autoclicker.presentation.components.SwipeDirectionLineCanvas
import com.kinetic.autoclicker.presentation.components.SwipeEndReticlePin
import com.kinetic.autoclicker.presentation.components.TargetReticlePin
import com.kinetic.autoclicker.presentation.contract.MainUiIntent
import com.kinetic.autoclicker.presentation.contract.MainUiState
import com.kinetic.autoclicker.presentation.theme.KineticComponentShape
import com.kinetic.autoclicker.presentation.theme.LocalAppStrings
import com.kinetic.autoclicker.presentation.theme.LocalKineticPalette
import kotlin.math.roundToInt

@Composable
fun TargetsOverlayScreen(
    uiState: MainUiState,
    onIntent: (MainUiIntent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val palette = LocalKineticPalette.current
    val strings = LocalAppStrings.current
    val density = LocalDensity.current
    val engineState = uiState.engineState
    val scope = rememberCoroutineScope()

    // KURAL: Test ekranına girildiğinde açık olan yüzen panel otomatik kapatılır
    LaunchedEffect(Unit) {
        if (engineState.isOverlayVisible) {
            onIntent(MainUiIntent.ToggleOverlayController)
        }
    }

    var canvasSize by remember { mutableStateOf(IntSize(800, 1000)) }
    var selectedTargetId by remember { mutableStateOf<String?>(null) }

    val visibleTargets = engineState.targets
    val selectedTarget = visibleTargets.find { it.id == selectedTargetId }
    val activeTargetForCoords = selectedTarget ?: visibleTargets.firstOrNull()

    val screenW = engineState.screenWidthPx.toFloat().coerceAtLeast(360f)
    val screenH = engineState.screenHeightPx.toFloat().coerceAtLeast(640f)
    val pinHalfPx = with(density) { 28.dp.toPx() }

    // Manzara Arka Planının Kayma (Pan/Scroll) Koordinatları
    val landscapePanX = remember { Animatable(0f) }
    val landscapePanY = remember { Animatable(0f) }

    // Tıklamada Manzaranın Hafif Yaylanması (Titreşimsiz Geri Tepki)
    var isTappedFeedback by remember { mutableStateOf(false) }
    val landscapeScale by animateFloatAsState(
        targetValue = if (isTappedFeedback) 0.985f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "landscape_tap_scale"
    )

    // Tıklamada Dokunma Noktasında Yayılan Halka
    var testRipplePos by remember { mutableStateOf<Offset?>(null) }
    val rippleProgress = remember { Animatable(0f) }

    var isTestRunning by remember { mutableStateOf(false) }
    var activeTestTargetIndex by remember { mutableIntStateOf(0) }

    // KURAL: Test ekranından ayrılınca testi güvenle durdur
    DisposableEffect(Unit) {
        onDispose {
            isTestRunning = false
        }
    }

    // GÜVENLİ TEST SİMÜLASYONU (Sandboxed In-App Test Engine):
    // Kullanıcı test ekranındayken Android sistemine gerçek dokunma ASLA gönderilmez!
    // Hareketler sadece bu test tuvalinde görsel olarak (su dalgası, kaydırma ve sayaç) çalıştırılır.
    LaunchedEffect(isTestRunning, visibleTargets) {
        if (!isTestRunning || visibleTargets.isEmpty()) return@LaunchedEffect

        var cursor = 0
        while (isActive && isTestRunning && visibleTargets.isNotEmpty()) {
            val currentIndex = cursor % visibleTargets.size
            activeTestTargetIndex = currentIndex
            val currentTarget = visibleTargets[currentIndex]

            val cWidth = canvasSize.width.toFloat().coerceAtLeast(240f)
            val cHeight = canvasSize.height.toFloat().coerceAtLeast(320f)
            val tX = currentTarget.normalizedX.coerceIn(0.08f, 0.92f) * cWidth
            val tY = currentTarget.normalizedY.coerceIn(0.08f, 0.88f) * cHeight

            if (currentTarget.actionType == ActionType.SWIPE) {
                // 1. Basılı Tutma süresi
                if (currentTarget.holdBeforeSwipeMs > 0L) {
                    delay(currentTarget.holdBeforeSwipeMs)
                }

                // 2. Kaydırma animasyonu (Doğa arka planı parmak yönünde fiziksel kayar)
                val endXNorm = currentTarget.normalizedEndX ?: (currentTarget.normalizedX + 0.16f)
                val endYNorm = currentTarget.normalizedEndY ?: (currentTarget.normalizedY - 0.22f)
                val deltaNormX = endXNorm - currentTarget.normalizedX
                val deltaNormY = endYNorm - currentTarget.normalizedY

                val targetPanX = landscapePanX.value + deltaNormX * 260f
                val targetPanY = landscapePanY.value + deltaNormY * 260f

                val swipeDurationMs = currentTarget.swipeDurationMs.coerceIn(100L, 2000L).toInt()
                launch { landscapePanX.animateTo(targetPanX, tween(swipeDurationMs, easing = LinearEasing)) }
                landscapePanY.animateTo(targetPanY, tween(swipeDurationMs, easing = LinearEasing))
            } else {
                // Tıklama animasyonu: Titreşimsiz su dalgası & yaylanma
                testRipplePos = Offset(tX, tY)
                isTappedFeedback = true
                rippleProgress.snapTo(0f)
                val holdMs = currentTarget.holdDurationMs.coerceIn(20L, 400L).toInt()
                launch { rippleProgress.animateTo(1f, tween(durationMillis = (holdMs + 180).coerceAtLeast(220))) }
                delay(holdMs.toLong())
                isTappedFeedback = false
            }

            // Sayacı artır
            onIntent(MainUiIntent.IncrementExecutionCount)

            // Aralık (Interval) süresi kadar bekle
            val intervalMs = currentTarget.intervalMs.coerceAtLeast(50L)
            delay(intervalMs)

            cursor++
        }
        isTestRunning = false
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Üst Başlık & Hızlı Hedef Yönetimi
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = strings.activeOverlayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (visibleTargets.isEmpty()) "Test için bir hareket ekleyin" else "${visibleTargets.size} hedef aktif • Düzenlemek için dokunun",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.onSurfaceVariant
                )
            }

            // Hızlı Aksiyon Ekleme / Silme Butonları
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Seçili Hedefi Sil Butonu (#2, #3 vs. doğrudan silinir ve tüm sayılar 3->2, 4->3 re-index olur!)
                if (selectedTarget != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.errorContainer)
                            .clickable {
                                onIntent(MainUiIntent.RemoveTargetById(selectedTarget.id))
                                selectedTargetId = null
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hedefi Sil",
                            tint = palette.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "#${selectedTarget.orderIndex} Sil",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // + Tıklama Ekle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.surfaceContainerHigh)
                        .clickable { onIntent(MainUiIntent.AddTarget(ActionType.TAP)) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "+ Tık",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // + Kaydırma Ekle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.primaryContainer)
                        .clickable { onIntent(MainUiIntent.AddTarget(ActionType.SWIPE)) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "+ Kaydır",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedTarget == null && visibleTargets.isNotEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(palette.surfaceContainerHigh)
                            .clickable { onIntent(MainUiIntent.RemoveLastTarget) }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "Sonuncuyu Sil",
                            tint = palette.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Test Alanı: Kullanıcının Görseli ile Canlı Doğa Arka Planı
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(490.dp)
                .shadow(6.dp, KineticComponentShape)
                .clip(KineticComponentShape)
                .background(palette.surfaceContainerLowest)
                .border(1.dp, palette.outlineVariant.copy(alpha = 0.40f), KineticComponentShape)
                .onSizeChanged { size ->
                    if (size.width > 0 && size.height > 0) {
                        canvasSize = size
                    }
                }
        ) {
            // 1. CANLI DOĞA ARKA PLANI: Çift Kopya Nesne Havuzu (Object Pooling Seamless Loop)
            // Bellek şişmesini önleyen kopya havuzu:
            // Biri ekran dışına çıktığı anda silinmez, sıfırdan oluşturulmak yerine hemen diğerinin ardına taşınır.
            val bgPainter = painterResource(id = R.drawable.test_nature_background)
            val cW = canvasSize.width.toFloat().coerceAtLeast(100f)
            val cH = canvasSize.height.toFloat().coerceAtLeast(100f)

            // Pan değerinin modülo ile sürekli sarılması (wrapping)
            val panXVal = landscapePanX.value
            val panYVal = landscapePanY.value
            val wrapX = ((panXVal % cW) + cW) % cW
            val wrapY = ((panYVal % cH) + cH) % cH

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(landscapeScale)
                    .pointerInput(Unit) {
                        // Kullanıcı elle serbestçe sürükleyebilir (sınırsız kesintisiz akış)
                        detectDragGestures { _, dragAmount ->
                            scope.launch {
                                landscapePanX.snapTo(landscapePanX.value + dragAmount.x)
                                landscapePanY.snapTo(landscapePanY.value + dragAmount.y)
                            }
                        }
                    }
            ) {
                // Havuz Kopya 1
                Image(
                    painter = bgPainter,
                    contentDescription = "Doğa Arka Planı Kopya 1",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x = (wrapX - cW).roundToInt(),
                                y = (wrapY - cH).roundToInt()
                            )
                        }
                )
                // Havuz Kopya 2 (Yan yana duran ikinci havuz parçası)
                Image(
                    painter = bgPainter,
                    contentDescription = "Doğa Arka Planı Kopya 2",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x = wrapX.roundToInt(),
                                y = (wrapY - cH).roundToInt()
                            )
                        }
                )
                // Havuz Kopya 3 (Alt havuz parçası)
                Image(
                    painter = bgPainter,
                    contentDescription = "Doğa Arka Planı Kopya 3",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x = (wrapX - cW).roundToInt(),
                                y = wrapY.roundToInt()
                            )
                        }
                )
                // Havuz Kopya 4 (Diyagonal havuz parçası)
                Image(
                    painter = bgPainter,
                    contentDescription = "Doğa Arka Planı Kopya 4",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x = wrapX.roundToInt(),
                                y = wrapY.roundToInt()
                            )
                        }
                )
            }

            // 2. Tıklama Anında Yayılan Su Dalgası Efekti (Ripple)
            Canvas(modifier = Modifier.fillMaxSize()) {
                testRipplePos?.let { pos ->
                    val progress = rippleProgress.value
                    if (progress > 0f && progress < 1f) {
                        val maxRadius = 42.dp.toPx()
                        val currentRadius = maxRadius * progress
                        val alpha = (1f - progress) * 0.90f
                        drawCircle(
                            color = palette.primary.copy(alpha = alpha),
                            radius = currentRadius,
                            center = pos,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = palette.primary.copy(alpha = alpha * 0.65f),
                            radius = 8.dp.toPx() * (1f - progress * 0.4f),
                            center = pos
                        )
                    }
                }
            }

            // 3. Sol Üst: Koordinat ve Bilgi Rozeti
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 14.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceContainerHigh.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(15.dp)
                )
                val coordText = activeTargetForCoords?.let {
                    "#${it.orderIndex}: X=${it.x.roundToInt()} Y=${it.y.roundToInt()}"
                } ?: "X=0 Y=0"
                Text(
                    text = coordText,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 3B. Sağ Üst: Seçili Hedef Hızlı Aksiyon Rozeti (Doğrudan Sil / Ayarla)
            if (selectedTarget != null) {
                val sel = selectedTarget
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 14.dp, top = 14.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(palette.surfaceContainerHigh.copy(alpha = 0.95f))
                        .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "#${sel.orderIndex} ${if (sel.actionType == ActionType.SWIPE) "Kaydır" else "Tık"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    // Sil Butonu
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.errorContainer)
                            .clickable {
                                onIntent(MainUiIntent.RemoveTargetById(sel.id))
                                selectedTargetId = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Sil",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Ayarla Butonu
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.primaryContainer)
                            .clickable {
                                onIntent(MainUiIntent.OpenClickSettings(sel.id))
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Ayarla",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Test Tahtası Genişlik / Yükseklik Oranları
            val cWidth = canvasSize.width.toFloat().coerceAtLeast(240f)
            val cHeight = canvasSize.height.toFloat().coerceAtLeast(320f)

            // 4. Kaydırma (Swipe) Yön Çizgileri ve Akıcı Parmak İzi
            val swipePairs = visibleTargets
                .filter { it.actionType == ActionType.SWIPE }
                .map { target ->
                    val startCenterX = (target.normalizedX.coerceIn(0.08f, 0.92f) * cWidth)
                    val startCenterY = (target.normalizedY.coerceIn(0.08f, 0.88f) * cHeight)
                    val normEndX = target.normalizedEndX ?: (target.normalizedX + 0.16f).coerceIn(0.08f, 0.92f)
                    val normEndY = target.normalizedEndY ?: (target.normalizedY - 0.22f).coerceIn(0.08f, 0.88f)
                    val endCenterX = (normEndX.coerceIn(0.08f, 0.92f) * cWidth)
                    val endCenterY = (normEndY.coerceIn(0.08f, 0.88f) * cHeight)
                    Offset(startCenterX, startCenterY) to Offset(endCenterX, endCenterY)
                }

            if (swipePairs.isNotEmpty()) {
                SwipeDirectionLineCanvas(
                    swipePairs = swipePairs,
                    isEngineRunning = isTestRunning,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 5. Hedef Pimleri (#1A Başlangıç / Tekli Tıklama)
            visibleTargets.forEachIndexed { index, target ->
                val pinCenterX = (target.normalizedX.coerceIn(0.08f, 0.92f) * cWidth)
                val pinCenterY = (target.normalizedY.coerceIn(0.08f, 0.88f) * cHeight)

                TargetReticlePin(
                    orderIndex = target.orderIndex,
                    actionType = target.actionType,
                    isActive = isTestRunning,
                    isPulsing = isTestRunning && activeTestTargetIndex == index,
                    isSelected = selectedTargetId == target.id,
                    showDragHint = false,
                    onTap = {
                        selectedTargetId = target.id
                    },
                    onDelete = {
                        onIntent(MainUiIntent.RemoveTargetById(target.id))
                        selectedTargetId = null
                    },
                    onDrag = { dx, dy ->
                        selectedTargetId = target.id
                        val scaleX = screenW / cWidth
                        val scaleY = screenH / cHeight
                        val nextScreenX = (target.x + dx * scaleX).coerceIn(24f, screenW - 24f)
                        val nextScreenY = (target.y + dy * scaleY).coerceIn(24f, screenH - 24f)
                        onIntent(
                            MainUiIntent.UpdateTargetPosition(
                                targetId = target.id,
                                x = nextScreenX,
                                y = nextScreenY
                            )
                        )
                    },
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (pinCenterX - pinHalfPx).roundToInt(),
                                y = (pinCenterY - pinHalfPx).roundToInt()
                            )
                        }
                )

                // Kaydırma Bitiş Pimi (#1B)
                if (target.actionType == ActionType.SWIPE) {
                    val normEndX = target.normalizedEndX ?: (target.normalizedX + 0.16f).coerceIn(0.08f, 0.92f)
                    val normEndY = target.normalizedEndY ?: (target.normalizedY - 0.22f).coerceIn(0.08f, 0.88f)
                    val endCenterX = (normEndX.coerceIn(0.08f, 0.92f) * cWidth)
                    val endCenterY = (normEndY.coerceIn(0.08f, 0.88f) * cHeight)
                    val currentScreenEndX = target.endX ?: (normEndX * screenW)
                    val currentScreenEndY = target.endY ?: (normEndY * screenH)

                    SwipeEndReticlePin(
                        orderIndex = target.orderIndex,
                        isActive = isTestRunning,
                        onTap = {
                            selectedTargetId = target.id
                        },
                        onDrag = { dx, dy ->
                            selectedTargetId = target.id
                            val scaleX = screenW / cWidth
                            val scaleY = screenH / cHeight
                            val nextScreenEndX = (currentScreenEndX + dx * scaleX).coerceIn(24f, screenW - 24f)
                            val nextScreenEndY = (currentScreenEndY + dy * scaleY).coerceIn(24f, screenH - 24f)
                            onIntent(
                                MainUiIntent.UpdateSwipeEndPosition(
                                    targetId = target.id,
                                    endX = nextScreenEndX,
                                    endY = nextScreenEndY
                                )
                            )
                        },
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = (endCenterX - pinHalfPx).roundToInt(),
                                    y = (endCenterY - pinHalfPx).roundToInt()
                                )
                            }
                    )
                }
            }

            // 5B. Hedef Yoksa: "İlk Hedefinizi Ekleyin" Seçim Kartı
            if (visibleTargets.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .shadow(12.dp, KineticComponentShape)
                        .clip(KineticComponentShape)
                        .background(palette.surfaceContainerHigh.copy(alpha = 0.96f))
                        .border(1.dp, palette.outlineVariant.copy(alpha = 0.5f), KineticComponentShape)
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = strings.initialTargetChooserTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.initialTargetChooserDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.16f))
                                .border(1.dp, palette.primary.copy(alpha = 0.5f), CircleShape)
                                .clickable { onIntent(MainUiIntent.AddTarget(ActionType.TAP)) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Tıklama Ekle",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.secondary.copy(alpha = 0.16f))
                                .border(1.dp, palette.secondary.copy(alpha = 0.5f), CircleShape)
                                .clickable { onIntent(MainUiIntent.AddTarget(ActionType.SWIPE)) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = null,
                                tint = palette.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Kaydırma Ekle",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 6. Test Alanı İçi Sade ve Net "TESTİ ÇALIŞTIR / DURDUR" Kontrol Butonu
            val isRunning = isTestRunning
            val testBtnBg = if (isRunning) palette.errorContainer else palette.primaryContainer
            val testBtnContent = if (isRunning) palette.onErrorContainer else palette.onPrimaryContainer

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(testBtnBg)
                    .clickable { isTestRunning = !isTestRunning }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = testBtnContent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Testi Durdur" else "Testi Başlat",
                    style = MaterialTheme.typography.titleSmall,
                    color = testBtnContent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // TIKLAMA SAYACI (Test Alanının Hemen Altında)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, KineticComponentShape)
                .clip(KineticComponentShape)
                .background(palette.surfaceContainer)
                .border(1.dp, palette.outlineVariant.copy(alpha = 0.35f), KineticComponentShape)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isTestRunning) palette.primaryContainer else palette.surfaceContainerHigh)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = null,
                        tint = if (isTestRunning) palette.onPrimaryContainer else palette.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = strings.clickCounterTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${engineState.totalClicksExecuted}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = palette.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isTestRunning) "işlem yapılıyor..." else "toplam işlem",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }

            if (engineState.totalClicksExecuted > 0L) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.surfaceContainerHigh)
                        .clickable { onIntent(MainUiIntent.ResetTotalClicks) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = strings.clickCounterReset,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Alt Çubuk: Hızlı Hız & Ayar Butonu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, KineticComponentShape)
                .clip(KineticComponentShape)
                .background(palette.surfaceContainer)
                .border(1.dp, palette.outlineVariant.copy(alpha = 0.35f), KineticComponentShape)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        onIntent(MainUiIntent.OpenClickSettings(activeTargetForCoords?.id))
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceContainerHigh)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "Tıklama ve Kaydırma Hızı",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${engineState.globalSettings.intervalValue} ms aralıkla çalışır",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.onSurfaceVariant
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(palette.surfaceContainerHigh)
                    .clickable {
                        onIntent(MainUiIntent.OpenClickSettings(activeTargetForCoords?.id))
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Ayarla",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
