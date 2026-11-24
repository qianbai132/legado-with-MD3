package io.legado.app.ui.ai.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.R
import io.legado.app.ui.book.read.AiBookChatWindow
import io.legado.app.ui.theme.LegadoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AI 对话悬浮窗的全局状态。悬浮窗从阅读页提升到 MainActivity 顶层后，可全 App 显示；
 * 最小化只收起窗口 UI，不销毁 ViewModel、不取消正在运行的生成任务。
 */
object AiFloatingChatController {
    data class ChatRequest(
        val bookUrl: String,
        val bookName: String,
        val chapterTitle: String = "",
        val skillId: String? = null,
        val skillName: String? = null,
        /** 每次打开生成，作为 ViewModel key，保证每次都是新会话。 */
        val nonce: String = "",
    )

    private val _request = MutableStateFlow<ChatRequest?>(null)
    val request = _request.asStateFlow()

    private val _minimized = MutableStateFlow(false)
    val minimized = _minimized.asStateFlow()

    /** 最小化气泡的位置（dp），跨页面保留。 */
    private val _bubbleX = MutableStateFlow(0f)
    val bubbleX = _bubbleX.asStateFlow()
    private val _bubbleY = MutableStateFlow(0f)
    val bubbleY = _bubbleY.asStateFlow()

    fun open(req: ChatRequest) {
        // 每次打开都分配新的 nonce → 新的 ViewModel 实例 → 新会话。
        _request.value = req.copy(nonce = System.nanoTime().toString())
        _minimized.value = false
    }

    fun minimize() {
        _minimized.value = true
    }

    fun restore() {
        _minimized.value = false
    }

    fun close() {
        _request.value = null
        _minimized.value = false
    }

    fun setBubblePosition(x: Float, y: Float) {
        _bubbleX.value = x
        _bubbleY.value = y
    }
}

/**
 * 挂在 MainActivity 顶层、浮在所有导航页面之上的 AI 对话宿主。
 * 有请求且未最小化时显示完整对话小窗；最小化时显示可拖动气泡，点击恢复。
 */
@Composable
fun AiFloatingChatHost() {
    val request by AiFloatingChatController.request.collectAsStateWithLifecycle()
    val minimized by AiFloatingChatController.minimized.collectAsStateWithLifecycle()
    val req = request ?: return
    if (minimized) {
        AiFloatingChatBubble(
            onRestore = { AiFloatingChatController.restore() },
            onClose = { AiFloatingChatController.close() },
        )
    } else {
        AiBookChatWindow(
            bookUrl = req.bookUrl,
            bookName = req.bookName,
            chapterTitle = req.chapterTitle,
            skillId = req.skillId,
            skillName = req.skillName,
            nonce = req.nonce,
            onMinimize = { AiFloatingChatController.minimize() },
            onDismiss = { AiFloatingChatController.close() },
        )
    }
}

/** 最小化后的悬浮气泡：可拖动、点击恢复对话、长按关闭。 */
@Composable
private fun AiFloatingChatBubble(
    onRestore: () -> Unit,
    onClose: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenW = configuration.screenWidthDp.toFloat()
    val screenH = configuration.screenHeightDp.toFloat()
    val size = 56f
    var offsetX by remember { mutableFloatStateOf(AiFloatingChatController.bubbleX.value) }
    var offsetY by remember { mutableFloatStateOf(AiFloatingChatController.bubbleY.value) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (offsetX == 0f && offsetY == 0f) {
            offsetX = (screenW - size - 16f).coerceAtLeast(0f)
            offsetY = (screenH - size - 96f).coerceAtLeast(0f)
        }
    }
    LaunchedEffect(screenW, screenH) {
        offsetX = offsetX.coerceIn(0f, (screenW - size).coerceAtLeast(0f))
        offsetY = offsetY.coerceIn(0f, (screenH - size).coerceAtLeast(0f))
    }
    val shownX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = if (dragging) snap() else tween(200, easing = FastOutSlowInEasing),
        label = "aiBubbleX",
    )
    val shownY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = if (dragging) snap() else tween(200, easing = FastOutSlowInEasing),
        label = "aiBubbleY",
    )
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = LegadoTheme.colorScheme.primaryContainer,
        shadowElevation = 10.dp,
        modifier = Modifier
            .offset(x = shownX.dp, y = shownY.dp)
            .size(size.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        AiFloatingChatController.setBubblePosition(offsetX, offsetY)
                    },
                    onDragCancel = { dragging = false },
                ) { change, amount ->
                    change.consume()
                    offsetX = (offsetX + amount.x / density.density)
                        .coerceIn(0f, (screenW - size).coerceAtLeast(0f))
                    offsetY = (offsetY + amount.y / density.density)
                        .coerceIn(0f, (screenH - size).coerceAtLeast(0f))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onRestore() },
                    onLongPress = { onClose() },
                )
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(R.string.ai_chat),
                tint = LegadoTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
