package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat_conversations")
data class AiChatConversation(
    @PrimaryKey
    val id: String,
    val title: String,
    val reasoningLevel: String = "auto",
    val modelProfileId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** 会话置顶：置顶的会话在列表里排在前面。 */
    val isPinned: Boolean = false
)
