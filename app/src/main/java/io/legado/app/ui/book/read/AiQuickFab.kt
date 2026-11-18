package io.legado.app.ui.book.read

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.theme.LegadoTheme
import kotlinx.coroutines.delay

private const val AI_FAB_SIZE_DP = 64f
private const val AI_FAB_TOP_LIMIT_DP = 72f
private const val AI_FAB_BOTTOM_GAP_DP = 96f

/**
 * 阅读页 AI 悬浮球（应用内浮层，不申请 SYSTEM_ALERT_WINDOW）。
 *
 * - 正圆 64dp；拖动时 snap 跟手，松手 tween 吸附到左右边缘。
 * - 贴边静止数秒后半隐藏（只露出半颗），点一下恢复。
 * - 点击直接打开缩小的 AI 对话小窗（新对话 / 历史会话 / 思考模式 / 技能都在小窗里）。
 */
@Composable
fun AiQuickFab(
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val screenHeightDp = configuration.screenHeightDp.toFloat()
    val half = AI_FAB_SIZE_DP / 2f
    val minCenterX = 0f
    val maxCenterX = screenWidthDp
    val minCenterY = AI_FAB_TOP_LIMIT_DP + half
    val maxCenterY = (screenHeightDp - AI_FAB_BOTTOM_GAP_DP - half).coerceAtLeast(minCenterY)

    var centerX by remember { mutableFloatStateOf(maxCenterX - half) }
    var centerY by remember { mutableFloatStateOf(maxCenterY) }
    LaunchedEffect(maxCenterX, maxCenterY) {
        centerX = centerX.coerceIn(minCenterX, maxCenterX)
        centerY = centerY.coerceIn(minCenterY, maxCenterY)
    }

    var dragging by remember { mutableStateOf(false) }
    var docked by remember { mutableIntStateOf(0) }
    var collapsed by remember { mutableStateOf(false) }

    LaunchedEffect(docked, collapsed, dragging) {
        if (docked != 0 && !collapsed && !dragging) {
            delay(3000)
            collapsed = true
        }
    }

    val expansion by animateFloatAsState(
        targetValue = if (collapsed) 0f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "aiFabExpansion",
    )

    val displayCenterXTarget = when {
        collapsed && docked == 1 -> screenWidthDp.toFloat()
        collapsed && docked == -1 -> 0f
        else -> centerX
    }
    val displayCenterX by animateFloatAsState(
        targetValue = displayCenterXTarget,
        animationSpec = if (dragging) snap() else tween(240, easing = FastOutSlowInEasing),
        label = "aiFabX",
    )
    val displayCenterY by animateFloatAsState(
        targetValue = centerY,
        animationSpec = if (dragging) snap() else tween(240, easing = FastOutSlowInEasing),
        label = "aiFabY",
    )

    val leftDp = displayCenterX - half
    val topDp = displayCenterY - half

    Box(modifier = modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = {
                if (collapsed) {
                    collapsed = false
                } else {
                    onOpenChat()
                }
            },
            modifier = Modifier
                .offset(x = leftDp.dp, y = topDp.dp)
                .scale(0.9f + 0.1f * expansion)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragging = true
                            collapsed = false
                        },
                        onDragCancel = { dragging = false },
                        onDragEnd = {
                            dragging = false
                            docked = if (centerX < screenWidthDp / 2f) -1 else 1
                            centerX = if (docked < 0) minCenterX else maxCenterX
                            centerY = centerY.coerceIn(minCenterY, maxCenterY)
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x / density.density
                        val dy = dragAmount.y / density.density
                        centerX = (centerX + dx).coerceIn(minCenterX, maxCenterX)
                        centerY = (centerY + dy).coerceIn(minCenterY, maxCenterY)
                        docked = 0
                    }
                },
            shape = CircleShape,
            containerColor = LegadoTheme.colorScheme.primaryContainer,
            contentColor = LegadoTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(R.string.ai_quick_fab),
            )
        }
    }
}
