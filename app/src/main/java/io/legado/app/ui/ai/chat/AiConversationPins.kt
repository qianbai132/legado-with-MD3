package io.legado.app.ui.ai.chat

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import splitties.init.appCtx

/**
 * 会话置顶状态。
 *
 * 没有存进 ai_chat_conversations 表：那个表加列需要升 Room 版本，
 * 而仓库里没有导出 136 的 schema，AutoMigration 生成不出来。
 * 置顶本身只是列表排序偏好，放本地偏好里足够，也不影响数据备份。
 */
object AiConversationPins {

    private const val PREFS_NAME = "ai_conversation_pins"
    private const val KEY_PINNED_IDS = "pinned_ids"

    private val prefs: SharedPreferences by lazy {
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _pinnedIds = MutableStateFlow(load())
    val pinnedIds: StateFlow<Set<String>> = _pinnedIds.asStateFlow()

    private fun load(): Set<String> {
        return runCatching {
            prefs.getStringSet(KEY_PINNED_IDS, emptySet())?.toSet().orEmpty()
        }.getOrDefault(emptySet())
    }

    fun setPinned(conversationId: String, pinned: Boolean) {
        val current = _pinnedIds.value
        val next = if (pinned) current + conversationId else current - conversationId
        if (next == current) return
        _pinnedIds.value = next
        runCatching { prefs.edit().putStringSet(KEY_PINNED_IDS, next).apply() }
    }

    fun setPinned(conversationIds: Collection<String>, pinned: Boolean) {
        if (conversationIds.isEmpty()) return
        val current = _pinnedIds.value
        val next = if (pinned) current + conversationIds else current - conversationIds.toSet()
        if (next == current) return
        _pinnedIds.value = next
        runCatching { prefs.edit().putStringSet(KEY_PINNED_IDS, next).apply() }
    }

    /** 会话被删除时清掉残留的置顶记录。 */
    fun remove(conversationIds: Collection<String>) {
        if (conversationIds.isEmpty()) return
        val current = _pinnedIds.value
        val next = current - conversationIds.toSet()
        if (next == current) return
        _pinnedIds.value = next
        runCatching { prefs.edit().putStringSet(KEY_PINNED_IDS, next).apply() }
    }
}
