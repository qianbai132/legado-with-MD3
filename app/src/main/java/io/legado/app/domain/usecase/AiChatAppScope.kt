package io.legado.app.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application-scoped coroutine scope for AI chat generation.
 *
 * Chat generation jobs launched here survive the destruction of the chat
 * Activity/ViewModel (e.g. when the user switches to the reader or locks the
 * screen). Results are still persisted to the chat database by the caller, so
 * returning to the chat shows the completed answer.
 */
class AiChatAppScope {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
