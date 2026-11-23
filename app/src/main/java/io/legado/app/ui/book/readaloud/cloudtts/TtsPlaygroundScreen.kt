package io.legado.app.ui.book.readaloud.cloudtts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.domain.model.readaloud.ReadAloudVoice
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppTextField
import io.legado.app.ui.widget.components.button.series.MediumTonalButton
import io.legado.app.ui.widget.components.menu.AppChoiceDropdown
import io.legado.app.ui.widget.components.menu.AppChoiceOption
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.text.AppText
import kotlinx.collections.immutable.toImmutableList

/**
 * 集中式 TTS 调试台（对齐 MiMo Studio）：引擎 / 音色 / 指令 / 风格 / 语速 / 文本
 * 一次呈现，合成后把诊断写入历史，可反复调参对比。全程不落库。
 */
@Composable
internal fun TtsPlaygroundSheet(
    state: CloudTtsUiState,
    onIntent: (CloudTtsIntent) -> Unit,
) {
    AppModalBottomSheet(
        data = state.playground.takeIf { state.playgroundOpen },
        onDismissRequest = { onIntent(CloudTtsIntent.ClosePlayground) },
        title = stringResource(R.string.cloud_tts_pg_title),
    ) { pg ->
        val update = { value: CloudTtsPlaygroundUi -> onIntent(CloudTtsIntent.UpdatePlayground(value)) }
        val defaultLabel = stringResource(R.string.cloud_tts_default)
        val engineChoices = remember(state.availableEngines) {
            buildList {
                add(AppChoiceOption("", defaultLabel))
                state.availableEngines.forEach {
                    add(AppChoiceOption(it.engineId, it.title))
                }
            }.toImmutableList()
        }
        val engineTitle = state.availableEngines
            .firstOrNull { it.engineId == pg.engineId }?.title
            ?: stringResource(R.string.cloud_tts_choose_engine)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                AppText(
                    text = stringResource(R.string.cloud_tts_pg_hint),
                    style = LegadoTheme.typography.bodySmall,
                    color = LegadoTheme.colorScheme.onSurfaceVariant,
                )
            }
            // 引擎
            item {
                AppChoiceDropdown(
                    options = engineChoices,
                    selectedId = pg.engineId,
                    onSelect = { id ->
                        val picked = state.availableEngines.firstOrNull { it.engineId == id }
                        update(pg.copy(engineId = id, engineType = picked?.engineType ?: ReadAloudVoice.ENGINE_CLOUD, voiceId = "", voiceUiId = ""))
                    },
                    title = engineTitle,
                    modifier = Modifier.fillMaxWidth(),
                    compact = true,
                    triggerShape = IconButtonDefaults.extraSmallRoundShape,
                ) {
                    MediumTonalButton(
                        onClick = null,
                        text = engineTitle,
                        modifier = this.modifier.fillMaxWidth(),
                    )
                }
            }
            // 音色
            item {
                val voiceChoices = remember(state.voices) {
                    buildList {
                        add(AppChoiceOption("", defaultLabel))
                        state.voices.forEach {
                            add(AppChoiceOption(it.id, it.title))
                        }
                    }.toImmutableList()
                }
                val voiceTitle = state.voices.firstOrNull { it.id == pg.voiceUiId }?.title
                    ?: pg.voiceId.ifBlank { stringResource(R.string.cloud_tts_select_voice) }
                AppChoiceDropdown(
                    options = voiceChoices,
                    selectedId = pg.voiceUiId,
                    onSelect = { id -> onIntent(CloudTtsIntent.PlaygroundSelectVoice(id)) },
                    title = voiceTitle,
                    modifier = Modifier.fillMaxWidth(),
                    compact = true,
                    triggerShape = IconButtonDefaults.extraSmallRoundShape,
                ) {
                    MediumTonalButton(
                        onClick = null,
                        text = voiceTitle,
                        modifier = this.modifier.fillMaxWidth(),
                    )
                }
            }
            // 风格（若当前音色有 styles）
            item {
                val styles = state.discoveredVoices
                    .firstOrNull { it.id == pg.voiceId }?.styles.orEmpty()
                if (styles.isNotEmpty()) {
                    val styleChoices = remember(styles, defaultLabel) {
                        buildList {
                            add(AppChoiceOption("", defaultLabel))
                            styles.forEach { add(AppChoiceOption(it, it)) }
                        }.toImmutableList()
                    }
                    val styleTitle = pg.style.ifBlank { defaultLabel }
                    AppChoiceDropdown(
                        options = styleChoices,
                        selectedId = pg.style,
                        onSelect = { style -> update(pg.copy(style = style)) },
                        title = styleTitle,
                        modifier = Modifier.fillMaxWidth(),
                        compact = true,
                        triggerShape = IconButtonDefaults.extraSmallRoundShape,
                    ) {
                        MediumTonalButton(
                            onClick = null,
                            text = styleTitle,
                            modifier = this.modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            // Instruct
            item {
                AppTextField(
                    pg.instruct,
                    { update(pg.copy(instruct = it)) },
                    label = stringResource(R.string.cloud_tts_pg_instruct),
                    minLines = 3,
                    backgroundColor = LegadoTheme.colorScheme.onSheetContent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 语速
            item {
                AppTextField(
                    pg.speed,
                    { update(pg.copy(speed = it)) },
                    label = stringResource(R.string.cloud_tts_speed_multiplier),
                    singleLine = true,
                    backgroundColor = LegadoTheme.colorScheme.onSheetContent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 文本
            item {
                AppTextField(
                    pg.text,
                    { update(pg.copy(text = it)) },
                    label = stringResource(R.string.cloud_tts_pg_text),
                    minLines = 4,
                    backgroundColor = LegadoTheme.colorScheme.onSheetContent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 合成按钮
            item {
                MediumTonalButton(
                    onClick = { onIntent(CloudTtsIntent.SynthesizePlayground) },
                    enabled = !pg.synthesizing,
                    text = stringResource(
                        if (pg.synthesizing) R.string.cloud_tts_pg_synthesizing
                        else R.string.cloud_tts_pg_synthesize
                    ),
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // 历史
            if (pg.history.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AppText(
                            text = stringResource(R.string.cloud_tts_pg_history),
                            style = LegadoTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        MediumTonalButton(
                            onClick = { onIntent(CloudTtsIntent.ClearPlaygroundHistory) },
                            text = stringResource(R.string.cloud_tts_pg_clear),
                        )
                    }
                }
                itemsIndexed(pg.history) { index, diag ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        AppText(
                            text = "${index + 1}. ${diag.summaryLabel()}",
                            style = LegadoTheme.typography.bodyMedium,
                            color = if (diag.success) {
                                LegadoTheme.colorScheme.primary
                            } else {
                                LegadoTheme.colorScheme.error
                            },
                        )
                        AppText(
                            text = diag.requestPreview.ifBlank { "-" },
                            style = LegadoTheme.typography.bodySmall,
                            color = LegadoTheme.colorScheme.onSurfaceVariant,
                        )
                        diag.errorMessage?.let {
                            AppText(
                                text = it,
                                style = LegadoTheme.typography.bodySmall,
                                color = LegadoTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}
