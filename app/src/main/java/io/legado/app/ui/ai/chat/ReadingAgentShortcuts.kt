package io.legado.app.ui.ai.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.card.NormalCard
import io.legado.app.ui.widget.components.text.AppText

/**
 * "阅读智能体"欢迎区：在有阅读上下文、且尚无消息时展示一组垂直问书快捷指令。
 * 每条指令点击后发送一条针对当前书籍/章节精心构造的 prompt，交由 AI 调本地书籍工具完成。
 */
@Composable
internal fun ReadingAgentWelcome(
    context: AiReadingContext?,
    onPrompt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookName = context?.bookName?.takeIf { it.isNotBlank() }
    val chapterTitle = context?.chapterTitle?.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = LegadoTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.size(8.dp))
            AppText(
                text = stringResource(R.string.ai_reading_agent),
                style = LegadoTheme.typography.titleMediumEmphasized,
            )
        }
        Spacer(Modifier.height(4.dp))
        AppText(
            text = stringResource(R.string.ai_reading_agent_hint),
            style = LegadoTheme.typography.bodyMedium,
            color = LegadoTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        val scopePhrase = buildScopePhrase(bookName, chapterTitle)
        val actions = listOf(
            QuickAction(
                icon = Icons.Default.MenuBook,
                label = stringResource(R.string.ai_quick_summary_chapter),
                prompt = stringResource(R.string.ai_prompt_summary_chapter, scopePhrase),
            ),
            QuickAction(
                icon = Icons.Default.AutoStories,
                label = stringResource(R.string.ai_quick_summary_book),
                prompt = stringResource(R.string.ai_prompt_summary_book, scopePhrase),
            ),
            QuickAction(
                icon = Icons.Default.Groups,
                label = stringResource(R.string.ai_quick_characters),
                prompt = stringResource(R.string.ai_prompt_characters, scopePhrase),
            ),
            QuickAction(
                icon = Icons.Default.Translate,
                label = stringResource(R.string.ai_quick_terms),
                prompt = stringResource(R.string.ai_prompt_terms, scopePhrase),
            ),
            QuickAction(
                icon = Icons.Default.Lightbulb,
                label = stringResource(R.string.ai_quick_plot),
                prompt = stringResource(R.string.ai_prompt_plot, scopePhrase),
            ),
            QuickAction(
                icon = Icons.Default.EditNote,
                label = stringResource(R.string.ai_quick_note),
                prompt = stringResource(R.string.ai_prompt_note, scopePhrase),
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            actions.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { action ->
                        NormalCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(96.dp),
                            onClick = { onPrompt(action.prompt) },
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = null,
                                    tint = LegadoTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                                AppText(
                                    text = action.label,
                                    style = LegadoTheme.typography.labelMediumEmphasized,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 把书名/章节拼成一句自然语言范围描述，注入到各 prompt 模板。 */
private fun buildScopePhrase(bookName: String?, chapterTitle: String?): String = buildString {
    if (bookName != null) {
        append("《").append(bookName).append("》")
    } else {
        append("当前正在阅读的书")
    }
    if (chapterTitle != null) {
        append("的「").append(chapterTitle).append("」这一章")
    }
}

private data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val prompt: String,
)
