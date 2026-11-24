package io.legado.app.ui.ai.chat

import io.legado.app.data.entities.AiArtifact
import splitties.init.appCtx
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AI 工作区的文件化能力：把数据库中的产物导出为可读的 .md 文件，
 * 让"工作区"真正成为一个能找到 AI 产出文件的地方。
 */
object AiWorkspaceFiles {
    /** 产物导出目录：应用外部文件目录下，用户可在文件管理器中访问。 */
    val exportDir: File
        get() = File(appCtx.getExternalFilesDir(null) ?: appCtx.filesDir, "ai_workspace")

    /** 产物分类：用于把摘要总结等按类做成资料卡。 */
    data class Category(
        val key: String,
        val title: String,
        val taskTypes: Set<String>,
    )

    val CATEGORIES = listOf(
        Category("summary", "摘要总结", setOf(
            "SUMMARIZE_CHAPTER", "SUMMARIZE_BOOK", "summarize_chapter", "summarize_book"
        )),
        Category("translate", "翻译", setOf(
            "TRANSLATE_CHAPTER", "translate_chapter"
        )),
        Category("character", "人物与设定", setOf(
            "IDENTIFY_CHARACTERS", "CAST_ASSIGN", "identify_characters", "cast_assign",
            "BOOK_KNOWLEDGE", "book_knowledge", "BOOK_OUTLINE", "book_outline"
        )),
        Category("polish", "文本加工", setOf(
            "EXPLAIN_SELECTION", "CLEAN_SELECTION", "TEXT_FACTORY", "REWRITE_TEXT",
            "explain_selection", "clean_selection", "text_factory", "rewrite_text"
        )),
        Category("speech", "朗读演出", setOf(
            "ANALYZE_SPEECH", "analyze_speech"
        )),
        Category("note", "笔记与对话", setOf(
            "ai_note", "chat_save", "CHAT"
        )),
    )

    /** 未知类型归入"其他"。 */
    fun categoryOf(taskType: String): Category {
        val upper = taskType.uppercase(Locale.ROOT)
        return CATEGORIES.firstOrNull { category ->
            category.taskTypes.any { it.uppercase(Locale.ROOT) == upper }
        } ?: Category("other", "其他", emptySet())
    }

    /** 把单条产物导出为 .md 文件，返回文件；失败返回 null。 */
    fun exportArtifact(artifact: AiArtifact): File? = runCatching {
        exportDir.mkdirs()
        val category = categoryOf(artifact.taskType)
        val categoryDir = File(exportDir, category.key).apply { mkdirs() }
        val time = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date(artifact.createdAt))
        val safeName = artifact.taskType.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
        val file = File(categoryDir, "${time}_${safeName}_${artifact.id.take(8)}.md")
        val bookLine = artifact.bookUrl.takeIf { it.isNotBlank() }?.let { "- 来源书籍：`$it`" } ?: "- 来源书籍：通用"
        val body = buildString {
            append("# ${category.title} · ${artifact.taskType}\n\n")
            append("$bookLine\n")
            append("- 任务类型：`${artifact.taskType}`\n")
            append("- 生成时间：$time\n")
            append("- 状态：${statusLabel(artifact.status)}\n\n")
            append("---\n\n")
            append(artifact.output?.ifBlank { "（无内容）" } ?: artifact.errorMessage ?: "（无内容）")
            append("\n")
        }
        file.writeText(body, Charsets.UTF_8)
        file
    }.getOrNull()

    /** 导出全部产物，返回成功导出的文件数。 */
    fun exportAll(artifacts: List<AiArtifact>): Int =
        artifacts.sumOf { if (exportArtifact(it) != null) 1 else 0 }

    /** 列出已导出的全部 .md 文件（按修改时间倒序）。 */
    fun listExportedFiles(): List<File> {
        if (!exportDir.exists()) return emptyList()
        return exportDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("md", ignoreCase = true) }
            .toList()
            .sortedByDescending { it.lastModified() }
    }

    private fun statusLabel(status: Int): String = when (status) {
        AiArtifact.STATUS_SUCCESS -> "成功"
        AiArtifact.STATUS_FAILED -> "失败"
        AiArtifact.STATUS_RUNNING -> "进行中"
        else -> "待处理"
    }
}
