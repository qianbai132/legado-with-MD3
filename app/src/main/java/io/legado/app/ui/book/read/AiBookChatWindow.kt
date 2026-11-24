package io.legado.app.ui.book.read

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.R
import io.legado.app.domain.model.AiErrorKind
import io.legado.app.domain.model.AiMessageRole
import io.legado.app.ui.ai.chat.AiAddMenuTrigger
import io.legado.app.ui.ai.chat.AiChatEffect
import io.legado.app.ui.ai.chat.AiChatIntent
import io.legado.app.ui.ai.chat.AiChatOverflowMenu
import io.legado.app.ui.ai.chat.AiChatViewModel
import io.legado.app.ui.ai.chat.AiConversationMenuSheet
import io.legado.app.ui.ai.chat.AiModelPicker
import io.legado.app.ui.ai.chat.AiReadingContext
import io.legado.app.ui.ai.chat.AiWorkspaceSheet
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.alert.AppAlertDialog
import io.legado.app.ui.widget.components.button.series.MediumTonalButton
import io.legado.app.ui.widget.components.button.series.SmallPlainButton
import io.legado.app.ui.widget.components.text.AppText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

private fun aiMiniErrorText(kind: AiErrorKind): Int = when (kind) {
    AiErrorKind.RATE_LIMIT -> R.string.ai_error_rate_limit
    AiErrorKind.AUTH -> R.string.ai_error_auth
    AiErrorKind.SERVER -> R.string.ai_error_server
    AiErrorKind.NETWORK -> R.string.ai_error_network
    AiErrorKind.UNKNOWN -> R.string.ai_error_generic
}

/**
 * 阅读页内的 AI 对话悬浮小窗：可拖动、比早期版本更小，每本书一个独立会话。
 * 顶栏左侧折叠菜单放「新对话 + 历史会话」，右侧溢出菜单放「新对话 / 工作区 / 保存 / 删除」；
 * 输入栏前置「添加」触发器，含思考模式与全部技能（含书源制作），点技能新开一个对话任务。
 * 布局与主问答框一致，只是压缩到浮层尺寸。
 */
@Composable
fun AiBookChatWindow(
    bookUrl: String,
    bookName: String,
    chapterTitle: String,
    skillId: String?,
    skillName: String?,
    nonce: String = "",
    onMinimize: () -> Unit,
    onDismiss: () -> Unit,
) {
    // nonce 每次打开都不同 → 每次都是全新 ViewModel（新会话）。
    val viewModel: AiChatViewModel = koinViewModel(key = "bookChat_${bookUrl.hashCode()}_$nonce")
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenW = configuration.screenWidthDp.toFloat()
    val screenH = configuration.screenHeightDp.toFloat()
    // 进一步缩小：宽度上限 480，高度取屏高 50%（300~480）。
    val winW = (screenW - 56f).coerceAtMost(480f)
    val winH = (screenH * 0.5f).coerceIn(300f, 480f)

    var offsetX by remember { mutableFloatStateOf((screenW - winW) / 2f) }
    var offsetY by remember { mutableFloatStateOf((screenH - winH) / 2f) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(screenW, screenH) {
        offsetX = offsetX.coerceIn(0f, (screenW - winW).coerceAtLeast(0f))
        offsetY = offsetY.coerceIn(0f, (screenH - winH).coerceAtLeast(0f))
    }
    val shownX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = if (dragging) snap() else tween(200, easing = FastOutSlowInEasing),
        label = "miniChatX",
    )
    val shownY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = if (dragging) snap() else tween(200, easing = FastOutSlowInEasing),
        label = "miniChatY",
    )

    val context = LocalContext.current
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is AiChatEffect.ShowMessage -> toast = effect.message
                is AiChatEffect.ShowAiError -> toast = context.getString(aiMiniErrorText(effect.kind))
            }
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2200)
            toast = null
        }
    }
    LaunchedEffect(nonce, bookUrl, skillId) {
        viewModel.onIntent(
            AiChatIntent.FreshBookChat(
                context = AiReadingContext(
                    bookUrl = bookUrl,
                    bookName = bookName,
                    chapterTitle = chapterTitle,
                ),
                skillId = skillId,
                skillName = skillName,
            )
        )
    }

    var menuSheetShown by remember { mutableStateOf(false) }
    var overflowExpanded by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = LegadoTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 12.dp,
        tonalElevation = 3.dp,
        modifier = Modifier
            .offset(x = shownX.dp, y = shownY.dp)
            .width(winW.dp)
            .heightIn(min = 300.dp, max = 480.dp),
    ) {
        Column {
            // 顶栏：左折叠菜单 / 中间可拖动标题 / 右溢出菜单。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediumTonalButton(
                    onClick = { menuSheetShown = true },
                    icon = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.ai_chat_menu),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { dragging = true },
                                onDragCancel = { dragging = false },
                                onDragEnd = { dragging = false },
                            ) { change, amount ->
                                change.consume()
                                offsetX = (offsetX + amount.x / density.density)
                                    .coerceIn(0f, (screenW - winW).coerceAtLeast(0f))
                                offsetY = (offsetY + amount.y / density.density)
                                    .coerceIn(0f, (screenH - winH).coerceAtLeast(0f))
                            }
                        }
                        .padding(horizontal = 8.dp),
                ) {
                    AppText(
                        text = state.activeSkillName?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.ai_chat),
                        style = LegadoTheme.typography.titleSmallEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AppText(
                        text = bookName,
                        style = LegadoTheme.typography.labelSmall,
                        color = LegadoTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AiModelPicker(
                    models = state.availableModels,
                    currentModelId = state.currentModelId,
                    fallbackLabel = state.activeSkillName.orEmpty(),
                    onSelect = { viewModel.onIntent(AiChatIntent.SelectModel(it)) },
                )
                Box {
                    MediumTonalButton(
                        onClick = { overflowExpanded = true },
                        icon = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.ai_more),
                    )
                    AiChatOverflowMenu(
                        expanded = overflowExpanded,
                        onDismiss = { overflowExpanded = false },
                        onNewChat = { viewModel.onIntent(AiChatIntent.NewBookChat) },
                        onWorkspace = { viewModel.onIntent(AiChatIntent.ToggleWorkspace) },
                        onSave = { viewModel.onIntent(AiChatIntent.SaveConversationToWorkspace) },
                        onDelete = {
                            viewModel.uiState.value.currentConversationId?.let {
                                conversationToDelete = it
                            }
                        },
                        onMinimize = onMinimize,
                        onBack = onDismiss,
                    )
                }
            }

            if (toast != null) {
                AppText(
                    text = toast.orEmpty(),
                    style = LegadoTheme.typography.labelMedium,
                    color = LegadoTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 消息列表。
            val listState = rememberLazyListState()
            LaunchedEffect(state.messages.size, state.streamingMessage != null) {
                if (state.messages.isNotEmpty()) {
                    runCatching { listState.animateScrollToItem(state.messages.size - 1) }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    val isUser = msg.role == AiMessageRole.USER
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isUser) LegadoTheme.colorScheme.primaryContainer
                        else LegadoTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.padding(vertical = 3.dp),
                    ) {
                        AppText(
                            text = msg.content,
                            style = LegadoTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
                state.streamingMessage?.let { streaming ->
                    item {
                        AppText(
                            text = streaming.content.ifBlank { "…" },
                            style = LegadoTheme.typography.bodyMedium,
                            color = LegadoTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }

            // 输入栏：前置「添加」触发器（思考模式 + 技能），点技能新开任务。
            var draft by remember { mutableStateOf("") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AiAddMenuTrigger(
                    reasoningLevel = state.reasoningLevel,
                    skills = state.availableSkills,
                    enabled = !state.isSending,
                    onLevelChange = { viewModel.onIntent(AiChatIntent.UpdateReasoningLevel(it)) },
                    onStartSkill = { viewModel.onIntent(AiChatIntent.StartSkillTask(it)) },
                )
                Spacer(Modifier.size(6.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LegadoTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 96.dp),
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        enabled = !state.isSending,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        textStyle = LegadoTheme.typography.bodyMedium.copy(
                            color = LegadoTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(LegadoTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            if (draft.isEmpty()) {
                                AppText(
                                    text = stringResource(R.string.ai_chat_input_hint),
                                    style = LegadoTheme.typography.bodyMedium,
                                    color = LegadoTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        },
                    )
                }
                Spacer(Modifier.size(6.dp))
                SmallPlainButton(
                    onClick = {
                        val text = draft.trim()
                        if (text.isNotBlank()) {
                            draft = ""
                            viewModel.onIntent(AiChatIntent.SendMessage(text))
                        }
                    },
                    icon = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.ai_send),
                )
            }
        }
    }

    if (state.workspaceOpen) {
        AiWorkspaceSheet(
            artifacts = state.workspaceArtifacts,
            workspaceFiles = state.workspaceFiles,
            onExport = { viewModel.onIntent(AiChatIntent.ExportWorkspace) },
            onDismiss = { viewModel.onIntent(AiChatIntent.CloseWorkspace) },
        )
    }

    AiConversationMenuSheet(
        show = menuSheetShown,
        onDismissRequest = { menuSheetShown = false },
        conversations = state.conversations,
        currentConversationId = state.currentConversationId,
        onNewChat = { viewModel.onIntent(AiChatIntent.NewBookChat) },
        onSelectConversation = { viewModel.onIntent(AiChatIntent.SelectConversation(it)) },
        onDeleteConversation = { viewModel.onIntent(AiChatIntent.DeleteConversation(it)) },
    )

    AppAlertDialog(
        show = conversationToDelete != null,
        onDismissRequest = { conversationToDelete = null },
        title = stringResource(R.string.ai_delete_current),
        text = stringResource(R.string.ai_delete_current_confirm),
        confirmText = stringResource(R.string.delete),
        onConfirm = {
            conversationToDelete?.let { viewModel.onIntent(AiChatIntent.DeleteConversation(it)) }
            conversationToDelete = null
        },
        dismissText = stringResource(R.string.cancel),
        onDismiss = { conversationToDelete = null },
    )
}
