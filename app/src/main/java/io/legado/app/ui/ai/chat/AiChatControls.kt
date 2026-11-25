package io.legado.app.ui.ai.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.domain.model.AiReasoningLevel
import io.legado.app.ui.ai.AiReasoningLevelControl
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.alert.AppAlertDialog
import io.legado.app.ui.widget.components.button.series.MediumTonalButton
import io.legado.app.ui.widget.components.card.NormalCard
import io.legado.app.ui.widget.components.divider.PillDivider
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenu
import io.legado.app.ui.widget.components.menuItem.RoundDropdownMenuItem
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.text.AppText
import kotlinx.collections.immutable.ImmutableList

/**
 * 输入框前的「添加」触发器：一个 + 按钮，点开一个底部面板。
 * 面板里放切换模型 + 思考模式 + 全部已启用技能（含书源制作）。
 * 点技能回调 [onStartSkill]，由调用方新开一个对话任务。
 */
@Composable
fun AiAddMenuTrigger(
    reasoningLevel: AiReasoningLevel,
    skills: ImmutableList<AiSkillOptionUi>,
    enabled: Boolean,
    models: ImmutableList<AiModelOptionUi> = kotlinx.collections.immutable.persistentListOf(),
    currentModelId: String? = null,
    onSelectModel: (String) -> Unit = {},
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
            // 切换模型：模型选择横栏从顶栏移到这里。
            if (models.isNotEmpty()) {
                AppText(
                    text = stringResource(R.string.ai_switch_model),
                    style = LegadoTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
                val currentLabel = models.firstOrNull { it.id == currentModelId }?.displayName
                AppText(
                    text = currentLabel?.let { stringResource(R.string.ai_current_value, it) }
                        ?: stringResource(R.string.ai_not_set),
                    style = LegadoTheme.typography.bodySmall,
                    color = LegadoTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
                models.forEach { model ->
                    NormalCard(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        onClick = {
                            onSelectModel(model.id)
                            showSheet = false
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (model.id == currentModelId) {
                                    LegadoTheme.colorScheme.primary
                                } else {
                                    LegadoTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            AppText(
                                text = model.displayName,
                                style = LegadoTheme.typography.bodyMedium,
                                maxLines = 1,
                            )
                        }
                    }
                }
                PillDivider()
            }
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
 *
 * - 单击会话：进入该会话。
 * - 长按会话：进入多选模式（默认选中被长按的会话）。
 * - 多选模式：可勾选多个会话，一键置顶 / 取消置顶、重命名（仅单选）、删除。
 */
@Composable
fun AiConversationMenuSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    conversations: ImmutableList<AiChatConversationUi>,
    currentConversationId: String?,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversations: (List<String>) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onSetPinned: (List<String>, Boolean) -> Unit,
) {
    var multiSelect by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var renameTargetId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf(TextFieldValue("")) }

    // 关闭时重置多选状态。
    if (!show) {
        if (multiSelect || selectedIds.isNotEmpty()) {
            multiSelect = false
            selectedIds = emptySet()
        }
    }

    AppModalBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = stringResource(
            if (multiSelect) R.string.ai_multi_select else R.string.ai_recent_chats
        ),
        animateContentSize = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (multiSelect) {
                // 多选操作栏：全选 / 置顶 / 重命名 / 删除 / 退出。
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MediumTonalButton(
                        onClick = {
                            selectedIds = if (selectedIds.size == conversations.size) {
                                emptySet()
                            } else {
                                conversations.map { it.id }.toSet()
                            }
                        },
                        text = stringResource(R.string.select_all),
                    )
                    Spacer(Modifier.width(8.dp))
                    MediumTonalButton(
                        onClick = {
                            onSetPinned(selectedIds.toList(), true)
                            multiSelect = false
                            selectedIds = emptySet()
                        },
                        icon = Icons.Default.PushPin,
                        text = stringResource(R.string.ai_pin),
                        enabled = selectedIds.isNotEmpty(),
                    )
                    Spacer(Modifier.width(8.dp))
                    MediumTonalButton(
                        onClick = {
                            onSetPinned(selectedIds.toList(), false)
                            multiSelect = false
                            selectedIds = emptySet()
                        },
                        icon = Icons.Outlined.PushPin,
                        text = stringResource(R.string.ai_unpin),
                        enabled = selectedIds.isNotEmpty(),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MediumTonalButton(
                        onClick = {
                            renameTargetId?.let { id ->
                                val text = renameText.text.trim()
                                if (text.isNotBlank()) onRenameConversation(id, text)
                            }
                            renameTargetId = null
                            multiSelect = false
                            selectedIds = emptySet()
                        },
                        text = stringResource(R.string.ai_rename),
                        enabled = selectedIds.size == 1,
                    )
                    Spacer(Modifier.width(8.dp))
                    MediumTonalButton(
                        onClick = {
                            onDeleteConversations(selectedIds.toList())
                            multiSelect = false
                            selectedIds = emptySet()
                        },
                        icon = Icons.Default.Delete,
                        text = stringResource(R.string.delete),
                        enabled = selectedIds.isNotEmpty(),
                    )
                    Spacer(Modifier.width(8.dp))
                    MediumTonalButton(
                        onClick = {
                            multiSelect = false
                            selectedIds = emptySet()
                        },
                        icon = Icons.Default.Close,
                        text = stringResource(R.string.cancel),
                    )
                }
                PillDivider()
            } else {
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
            }
            conversations.forEach { conversation ->
                val selected = conversation.id == currentConversationId
                val checked = conversation.id in selectedIds
                NormalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    onClick = {
                        if (multiSelect) {
                            selectedIds = if (checked) {
                                selectedIds - conversation.id
                            } else {
                                selectedIds + conversation.id
                            }
                        } else {
                            onDismissRequest()
                            onSelectConversation(conversation.id)
                        }
                    },
                    onLongClick = {
                        if (!multiSelect) {
                            multiSelect = true
                            selectedIds = setOf(conversation.id)
                        } else {
                            renameTargetId = conversation.id
                            renameText = TextFieldValue(conversation.title)
                        }
                    },
                    containerColor = when {
                        checked -> LegadoTheme.colorScheme.primaryContainer
                        selected -> LegadoTheme.colorScheme.secondaryContainer
                        else -> LegadoTheme.colorScheme.surface
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (multiSelect) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (conversation.isPinned) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = stringResource(R.string.ai_pin),
                                        modifier = Modifier.size(14.dp),
                                        tint = LegadoTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                AppText(
                                    text = conversation.title.ifBlank { stringResource(R.string.ai_chat) },
                                    style = LegadoTheme.typography.bodyMedium,
                                    maxLines = 1,
                                )
                            }
                            AppText(
                                text = formatRelativeTime(conversation.updatedAt),
                                style = LegadoTheme.typography.labelSmall,
                                color = LegadoTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    // 重命名对话框（多选模式下仅单选时可用；这里也支持长按单项直接改）。
    AppAlertDialog(
        show = renameTargetId != null,
        onDismissRequest = { renameTargetId = null },
        title = stringResource(R.string.ai_rename),
        confirmText = stringResource(R.string.ok),
        onConfirm = {
            renameTargetId?.let { id ->
                val text = renameText.text.trim()
                if (text.isNotBlank()) onRenameConversation(id, text)
            }
            renameTargetId = null
        },
        dismissText = stringResource(R.string.cancel),
        onDismiss = { renameTargetId = null },
        content = {
        BasicTextField(
            value = renameText,
            onValueChange = { renameText = it },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            textStyle = LegadoTheme.typography.bodyLarge.copy(
                color = LegadoTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(LegadoTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (renameText.text.isEmpty()) {
                    AppText(
                        text = stringResource(R.string.ai_chat),
                        style = LegadoTheme.typography.bodyLarge,
                        color = LegadoTheme.colorScheme.onSurfaceVariant,
                    )
                }
                inner()
            },
        )
        },
    )
}

/**
 * 顶栏右上角溢出菜单（精简版）：新建对话 / 最大化 / 最小化 / 关闭。
 * [onMaximize] / [onMinimize] 为空时不显示对应项（例如全屏问答页没有最小化）。
 */
@Composable
fun AiChatOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNewChat: () -> Unit,
    onMaximize: (() -> Unit)? = null,
    onMinimize: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
) {
    RoundDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) { dismiss ->
        RoundDropdownMenuItem(
            text = stringResource(R.string.ai_new_chat),
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            onClick = { dismiss(); onNewChat() },
        )
        if (onMaximize != null) {
            RoundDropdownMenuItem(
                text = stringResource(R.string.ai_maximize),
                leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null) },
                onClick = { dismiss(); onMaximize() },
            )
        }
        if (onMinimize != null) {
            RoundDropdownMenuItem(
                text = stringResource(R.string.ai_minimize),
                leadingIcon = { Icon(Icons.Default.Minimize, contentDescription = null) },
                onClick = { dismiss(); onMinimize() },
            )
        }
        if (onClose != null) {
            RoundDropdownMenuItem(
                text = stringResource(R.string.close),
                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                onClick = { dismiss(); onClose() },
            )
        }
    }
}
