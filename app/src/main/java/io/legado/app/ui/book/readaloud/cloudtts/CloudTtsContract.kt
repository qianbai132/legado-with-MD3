package io.legado.app.ui.book.readaloud.cloudtts

import android.net.Uri
import androidx.compose.runtime.Stable
import io.legado.app.data.entities.HttpTTS
import io.legado.app.domain.model.readaloud.CloudTtsSynthesisDiagnostics
import io.legado.app.domain.model.readaloud.ReadAloudVoice
import io.legado.app.ui.widget.components.importComponents.BaseImportUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Stable
data class CloudTtsUiState(
    val loading: Boolean = true,
    val engines: ImmutableList<CloudTtsEngineItemUi> = persistentListOf(),
    val systemEngines: ImmutableList<TtsManagedEngineItemUi> = persistentListOf(),
    val httpEngines: ImmutableList<TtsManagedEngineItemUi> = persistentListOf(),
    val voices: ImmutableList<CloudTtsVoiceItemUi> = persistentListOf(),
    val discoveredVoices: ImmutableList<CloudTtsDiscoveredVoiceUi> = persistentListOf(),
    val availableEngines: ImmutableList<TtsEngineOptionUi> = persistentListOf(),
    val engineEditor: CloudTtsEngineEditorUi? = null,
    val voiceEditor: TtsVoicePresetEditorUi? = null,
    val httpTtsEditor: HttpTTS? = null,
    val httpTtsImportState: BaseImportUiState<HttpTTS> = BaseImportUiState.Idle,
    val showVoiceEnginePicker: Boolean = false,
    val activeDialog: CloudTtsDialog? = null,
    val testing: Boolean = false,
    val discovering: Boolean = false,
    val selectedTab: CloudTtsTab = CloudTtsTab.Voices,
    /** 试听文本（用户改过的或内置默认），试听悬浮窗打开时填它。 */
    val previewText: String = "",
    /**
     * 音色批量选择模式：开了之后行尾的试听/删除按钮换成勾选，
     * 列表上方出现「全选 / 删除(n) / 退出」。
     */
    val voiceBatchMode: Boolean = false,
    val selectedVoiceIds: ImmutableSet<String> = persistentSetOf(),
    /** 「添加到声音池」弹层：非空 = 打开，带着要加进去的音色 id。 */
    val poolPicker: VoicePoolPickerUi? = null,
    /**
     * 被用户排除的音色数（源自带目录里不想用的那些）。
     *
     * 大于 0 时才给「恢复」入口——没有排除项时那个按钮按下去没有任何意义。
     */
    val excludedVoiceCount: Int = 0,
    /** 最近一次连接测试/试听的诊断快照，供诊断面板展示；null 表示还没有。 */
    val lastDiagnostics: CloudTtsSynthesisDiagnostics? = null,
    /** 连接测试是否成功（配合 [lastDiagnostics] 决定面板标题与基调）。 */
    val lastTestSucceeded: Boolean = false,
    /** 集中式 TTS 调试台是否打开。 */
    val playgroundOpen: Boolean = false,
    /** 调试台自身的可编辑状态。 */
    val playground: CloudTtsPlaygroundUi = CloudTtsPlaygroundUi(),
)

/**
 * 集中式 TTS 调试台（对齐 MiMo Studio）的可编辑状态。
 *
 * 全程不落库：改引擎/音色/指令/文本只影响调试台里的这一次合成。
 */
@Stable
data class CloudTtsPlaygroundUi(
    val engineType: String = ReadAloudVoice.ENGINE_CLOUD,
    val engineId: String = "",
    /** 真正发给 TTS API 的音色 speakerId。 */
    val voiceId: String = "",
    /** UI 下拉里选中的音色数据库 id（用于回显下拉）。 */
    val voiceUiId: String = "",
    val instruct: String = "",
    val text: String = "",
    val style: String = "",
    val speed: String = "1.0",
    val synthesizing: Boolean = false,
    /** 最近若干次合成的诊断， newest first。 */
    val history: ImmutableList<CloudTtsSynthesisDiagnostics> = persistentListOf(),
    /** 最近一次合成音频的播放路径（交给 Screen 用 MediaPlayer 播放）。 */
    val lastAudioPath: String? = null,
)

/** 把音色加进角色声音池的弹层内容：待加的音色 + 可选的池列表。 */
@Stable
data class VoicePoolPickerUi(
    val voiceIds: ImmutableList<String>,
    val voiceLabel: String,
    val pools: ImmutableList<VoicePoolOptionUi>,
)

@Stable
data class VoicePoolOptionUi(
    val id: String,
    val name: String,
    val groupName: String,
    val memberCount: Int,
)

enum class CloudTtsTab { Voices, Engines }

@Stable
data class CloudTtsEngineItemUi(
    val id: String,
    val title: String,
    val summary: String,
    val selected: Boolean = false,
)

@Stable
data class TtsManagedEngineItemUi(
    val engineType: String,
    val engineId: String,
    val title: String,
    val summary: String = "",
    val selected: Boolean = false,
    val loginUrl: String = "",
)
@Stable data class CloudTtsVoiceItemUi(
    val id: String,
    val title: String,
    val summary: String,
    val deletable: Boolean,
    val editable: Boolean,
)
@Stable data class TtsEngineOptionUi(
    val engineType: String,
    val engineId: String,
    val title: String,
    val summary: String,
    val catalogHint: String,
    val remoteCatalog: Boolean = false,
)
@Stable data class CloudTtsDiscoveredVoiceUi(
    val id: String,
    val label: String,
    val locale: String,
    val styles: ImmutableList<String>,
    val roles: ImmutableList<String>,
)

@Stable
data class CloudTtsEngineEditorUi(
    val editingEngineId: String? = null,
    val name: String = "",
    val provider: String = "mimo",
    val baseUrl: String = "",
    val apiKey: String = "",
    val secretKey: String = "",
    val region: String = "",
    val appId: String = "",
    val model: String = "",
    val optionsJson: String = "{}",
    /**
     * 连接测试成功后发现的音色。合并流程：所有 provider 统一在引擎弹窗内
     * 测试连接时拉取，勾选后在保存引擎时一并创建，不必再单独跑一遍音色向导。
     */
    val discoveredVoices: ImmutableList<CloudTtsDiscoveredVoiceUi> = persistentListOf(),
    /** 在引擎弹窗内勾选、保存引擎时一并创建的音色 id。 */
    val selectedVoiceIds: ImmutableSet<String> = persistentSetOf(),
    /** 是否已完成一次音色发现——区分「还没测试」与「测过但确实 0 音色」。 */
    val voicesDiscovered: Boolean = false,
)

@Stable
data class TtsVoicePresetEditorUi(
    val editingVoiceId: String? = null,
    val engineType: String = "",
    val engineId: String = "",
    val engineName: String = "",
    val voiceId: String = "",
    val voiceName: String = "",
    val locale: String = "",
    val style: String = "",
    val role: String = "",
    val instructions: String = "",
    val automaticEmotion: Boolean = true,
    val characterPersonality: Boolean = true,
    val thoughtPerformance: Boolean = true,
    val speed: String = "1.0",
    val pitch: String = "1.0",
    val volume: String = "1.0",
    val format: String = "mp3",
    val formatOptions: ImmutableList<String> = persistentListOf(),
)

sealed interface CloudTtsIntent {
    data class SelectTab(val tab: CloudTtsTab) : CloudTtsIntent
    data class SetBookContext(val bookUrl: String?) : CloudTtsIntent
    data object AddEngine : CloudTtsIntent
    data object AddVoice : CloudTtsIntent
    data class AddVoiceForEngine(val engineType: String, val engineId: String) : CloudTtsIntent
    data class EditEngine(val id: String) : CloudTtsIntent
    data class DeleteEngine(val id: String) : CloudTtsIntent
    data class SetDefaultEngine(val engineType: String, val engineId: String) : CloudTtsIntent
    data object ApplyDefaultEngineGlobally : CloudTtsIntent
    data object ApplyDefaultEngineForBook : CloudTtsIntent
    data class EditHttpTts(val engineId: String? = null) : CloudTtsIntent
    data class SaveHttpTts(val value: HttpTTS) : CloudTtsIntent
    data class DeleteHttpTts(val engineId: String) : CloudTtsIntent
    data class OpenHttpTtsLogin(val engineId: String) : CloudTtsIntent
    data object DismissHttpTtsEditor : CloudTtsIntent
    data class ImportHttpTtsSource(val text: String) : CloudTtsIntent
    data object ImportHttpTtsFile : CloudTtsIntent
    data class ImportHttpTtsFileSelected(val uri: Uri) : CloudTtsIntent
    data object CancelHttpTtsImport : CloudTtsIntent
    data class ToggleHttpTtsImportSelection(val index: Int) : CloudTtsIntent
    data class ToggleHttpTtsImportAll(val selected: Boolean) : CloudTtsIntent
    data class UpdateHttpTtsImportItem(val index: Int, val value: HttpTTS) : CloudTtsIntent
    data object SaveImportedHttpTts : CloudTtsIntent
    data object ExportHttpTtsFile : CloudTtsIntent
    data class ExportHttpTtsFileSelected(val uri: Uri) : CloudTtsIntent
    data object ExportHttpTtsUrl : CloudTtsIntent
    data object ClearTtsCache : CloudTtsIntent
       data class RequestDeleteVoice(val id: String) : CloudTtsIntent
    data object ConfirmDeleteVoice : CloudTtsIntent
    /** 进入/退出音色批量选择模式（退出会清空已勾选的）。 */
    data object ToggleVoiceBatchMode : CloudTtsIntent
    data class ToggleVoiceSelection(val id: String) : CloudTtsIntent
    /** 全选/取消全选当前列表里可删的音色。 */
    data object ToggleSelectAllVoices : CloudTtsIntent
    data object RequestDeleteSelectedVoices : CloudTtsIntent
    data object ConfirmDeleteSelectedVoices : CloudTtsIntent
    /** 把排除掉的音色全部放回来（源自带目录的那些）。 */
    data object RestoreExcludedVoices : CloudTtsIntent
    /** 打开「添加到声音池」弹层；[ids] 为空时按批量勾选的那一批处理。 */
    data class RequestAddToPool(val ids: List<String>) : CloudTtsIntent
    data object DismissPoolPicker : CloudTtsIntent
    data class ConfirmAddToPool(val poolId: String) : CloudTtsIntent
    data class EditVoice(val id: String) : CloudTtsIntent
    data class UpdateEngineEditor(val editor: CloudTtsEngineEditorUi) : CloudTtsIntent
    /** 引擎弹窗内：勾选/取消某个发现到的音色。 */
    data class ToggleEditorVoice(val voiceId: String) : CloudTtsIntent
    /** 引擎弹窗内：全选/取消全选发现到的音色。 */
    data object ToggleAllEditorVoices : CloudTtsIntent
    data class UpdateVoiceEditor(val editor: TtsVoicePresetEditorUi) : CloudTtsIntent
    data object DismissEngineEditor : CloudTtsIntent
    data object DismissVoiceEditor : CloudTtsIntent
    data object DismissVoiceEnginePicker : CloudTtsIntent
    data class SelectEngine(val engineType: String, val engineId: String) : CloudTtsIntent
    data object DiscoverVoices : CloudTtsIntent
    data class SelectVoice(val id: String) : CloudTtsIntent
    data object TestEngine : CloudTtsIntent
    data object Preview : CloudTtsIntent
    /**
     * 列表行上的试听按钮：不打开编辑弹窗，直接按该音色合成一句。
     * [text] 非空 = 用这句（试听悬浮窗里正在编辑的草稿），否则用已保存的默认文本。
     */
    data class PreviewVoice(val id: String, val text: String? = null) : CloudTtsIntent
    /**
     * 带临时参数的试听（试听调试弹窗）：风格/指令/语速只作用这一次，不写回音色。
     */
    data class PreviewVoiceTuned(
        val id: String,
        val text: String,
        val style: String,
        val instructions: String,
        val speed: String,
    ) : CloudTtsIntent
    /** 保存默认试听文本（试听悬浮窗的「确定」）。 */
    data class SavePreviewText(val text: String) : CloudTtsIntent
    data object Save : CloudTtsIntent
    data object DismissError : CloudTtsIntent
    data object CopyError : CloudTtsIntent
    data class ReportError(val message: String) : CloudTtsIntent
    /** 打开最近一次合成的诊断报告面板。 */
    data object ShowDiagnostics : CloudTtsIntent
    data object DismissDiagnostics : CloudTtsIntent
    /** 复制诊断报告文本。 */
    data object CopyDiagnostics : CloudTtsIntent
    // —— 集中式 TTS 调试台 ——
    data object OpenPlayground : CloudTtsIntent
    data object ClosePlayground : CloudTtsIntent
    data class UpdatePlayground(val value: CloudTtsPlaygroundUi) : CloudTtsIntent
    /** 调试台里选中某个音色（UI 模型 id），由 VM 解析成真正的 speakerId。 */
    data class PlaygroundSelectVoice(val voiceUiId: String) : CloudTtsIntent
    /** 用调试台当前参数合成一次，成功播放音频并写入历史。 */
    data object SynthesizePlayground : CloudTtsIntent
    data object ClearPlaygroundHistory : CloudTtsIntent
}

sealed interface CloudTtsEffect {
    data class ShowToast(val message: String) : CloudTtsEffect
    data class PlayPreview(val path: String) : CloudTtsEffect
    data class CopyText(val text: String, val message: String) : CloudTtsEffect
    data object OpenHttpTtsImportPicker : CloudTtsEffect
    data object OpenHttpTtsExportPicker : CloudTtsEffect
    data class OpenHttpTtsLogin(val engineId: Long) : CloudTtsEffect
}

sealed interface CloudTtsDialog {
    data class Error(val message: String) : CloudTtsDialog
    data class DeleteVoice(val id: String, val title: String) : CloudTtsDialog
    /** 批量删除确认：[count] 只用于文案，真正删的是 [ids]。 */
    data class DeleteVoices(val ids: ImmutableList<String>, val count: Int) : CloudTtsDialog
    data class DefaultEngineScope(val value: String?, val title: String) : CloudTtsDialog
    /** 连接测试/试听的合成诊断报告。 */
    data class Diagnostics(val value: CloudTtsSynthesisDiagnostics, val success: Boolean) : CloudTtsDialog
}
