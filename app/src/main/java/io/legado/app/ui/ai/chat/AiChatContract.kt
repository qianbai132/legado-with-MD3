package io.legado.app.ui.ai.chat

import androidx.compose.runtime.Stable
import io.legado.app.data.entities.AiArtifact
import io.legado.app.domain.model.AiErrorKind
import io.legado.app.domain.model.AiMessagePart
import io.legado.app.domain.model.AiReasoningLevel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Stable
data class AiChatUiState(
    val conversations: ImmutableList<AiChatConversationUi> = persistentListOf(),
    val messages: ImmutableList<AiChatMessageUi> = persistentListOf(),
    val currentConversationId: String? = null,
    val reasoningLevel: AiReasoningLevel = AiReasoningLevel.AUTO,
    val isSending: Boolean = false,
    val streamingMessage: AiChatMessageUi? = null,
    val pendingToolConfirmation: AiToolConfirmationUi? = null,
    val readingContext: AiReadingContext? = null,
    val workspaceOpen: Boolean = false,
    val workspaceArtifacts: ImmutableList<AiArtifact> = persistentListOf(),
    /** 工作区已导出的文件路径（真文件工作区）。 */
    val workspaceFiles: ImmutableList<String> = persistentListOf(),
    /** 阅读页技能小窗标题栏显示的技能名（仅标识，不注入正文）。 */
    val activeSkillName: String? = null,
    /** 供输入框「添加」菜单选择的技能（已启用，含书源制作）。 */
    val availableSkills: ImmutableList<AiSkillOptionUi> = persistentListOf(),
    /** 顶栏可切换的 AI 模型列表（已启用）。 */
    val availableModels: ImmutableList<AiModelOptionUi> = persistentListOf(),
    /** 当前选中的模型 id；空表示用任务预设的默认模型。 */
    val currentModelId: String? = null
)

/** 顶栏模型选择器里的一项。 */
@Stable
data class AiModelOptionUi(
    val id: String,
    val displayName: String,
    val providerName: String,
)

/** 输入框「添加」菜单里的一项技能。 */
@Stable
data class AiSkillOptionUi(
    val id: String,
    val name: String,
    val description: String
)

@Stable
data class AiReadingContext(
    val bookUrl: String,
    val bookName: String,
    val bookAuthor: String = "",
    val chapterIndex: Int? = null,
    val chapterTitle: String = ""
)

@Stable
data class AiChatConversationUi(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val isSelected: Boolean,
    val providerName: String = "",
    val modelName: String = "",
    val isPinned: Boolean = false
)

@Stable
data class AiChatMessageUi(
    val id: String,
    val role: String,
    val parts: ImmutableList<AiMessagePart> = persistentListOf(),
    val content: String,
    val reasoning: String?,
    val toolTrace: String?,
    val createdAt: Long,
    val thinkingDuration: Int = 0,
    val bookResults: ImmutableList<AiChatBookResultUi> = persistentListOf(),
    val branchIndex: Int = 0,
    val totalBranches: Int = 1,
    val parentMessageId: String? = null
)

@Stable
data class AiChatBookResultUi(
    val bookUrl: String,
    val name: String,
    val author: String,
    val origin: String?,
    val coverPath: String?,
    val latestChapterTitle: String?,
    val currentChapterTitle: String?,
    val intro: String?
)

@Stable
data class AiToolConfirmationUi(
    val title: String,
    val description: String
)

sealed interface AiChatIntent {
    data object NewConversation : AiChatIntent
    data class SelectConversation(val id: String) : AiChatIntent
    data class SendMessage(val content: String) : AiChatIntent
    data object StopGenerating : AiChatIntent
    data object ConfirmPendingTool : AiChatIntent
    data object RejectPendingTool : AiChatIntent
    data class UpdateReasoningLevel(val level: AiReasoningLevel) : AiChatIntent
    /** 顶栏切换当前对话使用的模型。 */
    data class SelectModel(val modelId: String) : AiChatIntent
    data class RegenerateMessage(val messageId: String) : AiChatIntent
    data class SwitchBranch(val messageId: String) : AiChatIntent
    data class DeleteConversation(val id: String) : AiChatIntent
    /** 折叠菜单多选模式下批量删除。 */
    data class DeleteConversations(val ids: List<String>) : AiChatIntent
    data class RenameConversation(val id: String, val title: String) : AiChatIntent
    /** 折叠菜单多选模式下批量置顶 / 取消置顶。 */
    data class SetConversationsPinned(val ids: List<String>, val pinned: Boolean) : AiChatIntent
    data class SetReadingContext(val context: AiReadingContext?) : AiChatIntent
    /** 从阅读页技能悬浮窗进入：加载指定技能内容并作为首条消息自动开聊。 */
    data class ApplySkill(val skillId: String) : AiChatIntent
    /**
     * 打开「某本书的对话小窗」：按 bookUrl 找/建独立会话，设置阅读上下文。
     * skillId 非空时只在标题栏显示技能名，并发一句轻量提示让 AI 自己用 load_skill 加载正文。
     */
    data class OpenBookChat(
        val context: AiReadingContext,
        val skillId: String? = null,
        val skillName: String? = null,
        /** 带技能启动时，用这句作为首条消息；为空则用默认的技能提示语。 */
        val initialDraft: String = ""
    ) : AiChatIntent
    /** 把当前对话保存到工作区（作为当前书的产物）。 */
    data object SaveConversationToWorkspace : AiChatIntent
    data object ToggleWorkspace : AiChatIntent
    data object CloseWorkspace : AiChatIntent
    /** 把工作区产物导出为真实 .md 文件并刷新文件列表。 */
    data object ExportWorkspace : AiChatIntent
    /** 仅刷新工作区文件列表。 */
    data object RefreshWorkspaceFiles : AiChatIntent
    /** 清空当前小窗会话的全部消息（保留会话与书名映射）。 */
    data object ClearBookChatMessages : AiChatIntent
    /** 为当前小窗的书新建一个全新会话，并更新 bookUrl 映射。 */
    data object NewBookChat : AiChatIntent
    /**
     * 每次打开悬浮窗都新开一个会话：设置上下文并总是新建会话，
     * 不复用 bookUrl 映射，也不更新长期映射（避免污染下次打开）。
     */
    data class FreshBookChat(
        val context: AiReadingContext,
        val skillId: String? = null,
        val skillName: String? = null
    ) : AiChatIntent

    // ---- 添加菜单 ----
    /**
     * 从「添加」菜单点技能：新建一个全新对话任务，标题用技能名，
     * 并把该技能作为首条消息的意图交给 AI（AI 用 load_skill 加载正文）。
     */
    data class StartSkillTask(val skillId: String) : AiChatIntent
}

sealed interface AiChatEffect {
    data class ShowMessage(val message: String) : AiChatEffect

    /** 失败提示：kind 决定本地化文案，raw 保留原始错误供「详情」查看。 */
    data class ShowAiError(val kind: AiErrorKind, val raw: String) : AiChatEffect
}
