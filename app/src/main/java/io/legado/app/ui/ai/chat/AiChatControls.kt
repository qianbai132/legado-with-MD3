package io.legado.app.ui.ai.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.domain.model.AiReasoningLevel
import io.legado.app.ui.ai.AiReasoningLevelControl
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.button.series.MediumTonalButton
import io.legado.app.ui.widget.components.card.NormalCard
import io.legado.app.ui.widget.components.divider.PillDivider
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenu
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenuItem
import io.legado.app.ui.widget.components.alert.AppAlertDialog
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.text.AppText
import kotlinx.collections.immutable.ImmutableList

/**
 * 输入框前的「添加」触发器：一个 + 按钮，点开一个底部面板。
 * 面板里放思考模式选择 + 全部已启用技能（含书源制作）。
 * 点技能回调 [onStartSkill]，由调用方新开一个对话任务。
 */
@Composable
fun AiAddMenuTrigger(
    reasoningLevel: AiReasoningLevel,
    skills: ImmutableList<AiSkillOptionUi>,
    enabled: Boolean,
    onLevelChange: (AiReasoningLevel) -> Unit,
    onStartSkill: (String) -> Unit,
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    MediumTonalButton(
        onClick = { showSheet = true },
        icon = Icons.Default.Add,
        enabled = enabled,
        contentDescription = stringResource(R.string.ai_add),
    )
    AppModalBottomSheet(
        show = showSheet,
        onDismissRequest = { showSheet = false },
        title = stringResource(R.string.ai_add),
        animateContentSize = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
        ) {
            AppText(
                text = stringResource(R.string.ai_thinking_mode),
                style = LegadoTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
            AiReasoningLevelControl(level = reasoningLevel, onLevelChange = onLevelChange)
            PillDivider()
            AppText(
                text = stringResource(R.string.ai_skills),
                style = LegadoTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            if (skills.isEmpty()) {
                AppText(
                    text = stringResource(R.string.ai_add_no_skills),
                    style = LegadoTheme.typography.bodyMedium,
                    color = LegadoTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                )
            } else {
                skills.forEach { skill ->
                    NormalCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        onClick = {
                            showSheet = false
                            onStartSkill(skill.id)
                        },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            AppText(
                                text = skill.name,
                                style = LegadoTheme.typography.bodyLarge,
                            )
                            if (skill.description.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                AppText(
                                    text = skill.description,
                                    style = LegadoTheme.typography.bodySmall,
                                    color = LegadoTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * 「新对话 + 历史会话」菜单面板（用于阅读页小窗的左上角折叠菜单）。
 * 全屏问答页使用侧滑抽屉，这里用小面板以适配浮层小窗。
 */
@Composable
fun AiConversationMenuSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    conversations: ImmutableList<AiChatConversationUi>,
    currentConversationId: String?,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
) {
    // 长按会话弹出删除确认，避免误触。
    var conversationToDelete by remember { mutableStateOf<String?>(null) }
    AppModalBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.ai_recent_chats),
        animateContentSize = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
        ) {
            NormalCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                onClick = {
                    onDismissRequest()
                    onNewChat()
                },
            ) {
                AppText(
                    text = stringResource(R.string.ai_new_chat),
                    style = LegadoTheme.typography.bodyLarge,
                    modifier = Modifier.padding(12.dp),
                )
            }
            conversations.forEach { conversation ->
                val selected = conversation.id == currentConversationId
                NormalCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    onClick = {
                        onDismissRequest()
                        onSelectConversation(conversation.id)
                    },
                    onLongClick = { conversationToDelete = conversation.id },
                    containerColor = if (selected) {
                        LegadoTheme.colorScheme.primaryContainer
                    } else {
                        LegadoTheme.colorScheme.surface
                    },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        AppText(
                            text = conversation.title.ifBlank { stringResource(R.string.ai_chat) },
                            style = LegadoTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                        AppText(
                            text = formatRelativeTime(conversation.updatedAt),
                            style = LegadoTheme.typography.labelSmall,
                            color = LegadoTheme.colorScheme.outline,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    AppAlertDialog(
        show = conversationToDelete != null,
        onDismissRequest = { conversationToDelete = null },
        title = stringResource(R.string.delete),
        text = stringResource(R.string.sure_del),
        confirmText = stringResource(R.string.delete),
        onConfirm = {
            conversationToDelete?.let { onDeleteConversation(it) }
            conversationToDelete = null
        },
        dismissText = stringResource(R.string.cancel),
        onDismiss = { conversationToDelete = null },
    )
}

/** 顶栏右上角溢出菜单：新对话 / 工作区 / 保存 / 删除。 */
@Composable
fun AiChatOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNewChat: () -> Unit,
    onWorkspace: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onMinimize: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
) {
    RoundDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) { dismiss ->
        RoundDropdownMenuItem(
            text = stringResource(R.string.ai_new_chat),
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            onClick = { dismiss(); onNewChat() },
        )
        RoundDropdownMenuItem(
            text = stringResource(R.string.ai_workspace),
            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
            onClick = { dismiss(); onWorkspace() },
        )
        RoundDropdownMenuItem(
            text = stringResource(R.string.ai_save_conversation),
            leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
            onClick = { dismiss(); onSave() },
        )
        RoundDropdownMenuItem(
            text = stringResource(R.string.ai_delete_current),
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            color = LegadoTheme.colorScheme.error,
            onClick = { dismiss(); onDelete() },
        )
        if (onMinimize != null || onBack != null) {
            PillDivider()
        }
        if (onMinimize != null) {
            RoundDropdownMenuItem(
                text = stringResource(R.string.ai_minimize),
                leadingIcon = { Icon(Icons.Default.Minimize, contentDescription = null) },
                onClick = { dismiss(); onMinimize() },
            )
        }
        if (onBack != null) {
            RoundDropdownMenuItem(
                text = stringResource(R.string.back),
                onClick = { dismiss(); onBack() },
            )
        }
    }
}
