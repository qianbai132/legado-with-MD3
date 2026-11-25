package io.legado.app.ui.config.ai

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.AppTextField
import io.legado.app.ui.widget.components.SplicedColumnGroup
import io.legado.app.ui.widget.components.button.series.MediumTonalButton
import io.legado.app.ui.widget.components.settingItem.ClickableSettingItem
import io.legado.app.ui.widget.components.text.AppText
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.GlassTopAppBarDefaults
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton

/**
 * 书源工坊：围绕内置技能「书源驯兽师」的使用入口与技能管理。
 *
 * - 使用：填站点名称 / 地址，选新建或改造，点开始后把一段结构化简报交给驯兽师。
 * - 管理：启停技能、查看 / 编辑正文、从剪贴板导入新版 SKILL.md。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSourceWorkshopScreen(
    skill: AiSkillListItemUi?,
    onClose: () -> Unit,
    onEditSkill: () -> Unit,
    onImportContent: (String) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onLaunch: (String) -> Unit,
) {
    val scrollBehavior = GlassTopAppBarDefaults.defaultScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var siteName by rememberSaveable { mutableStateOf("") }
    var siteUrl by rememberSaveable { mutableStateOf("") }
    var editExisting by rememberSaveable { mutableStateOf(false) }
    var extra by rememberSaveable { mutableStateOf("") }

    AppScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = stringResource(R.string.ai_book_source_workshop),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    TopBarNavigationButton(onClick = onClose)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 120.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppText(
                text = stringResource(R.string.ai_book_source_workshop_desc),
                style = LegadoTheme.typography.bodyMedium,
                color = LegadoTheme.colorScheme.onSurfaceVariant,
            )

            // ---- 技能状态 ----
            SplicedColumnGroup(title = stringResource(R.string.ai_workshop_skill)) {
                ClickableSettingItem(
                    title = skill?.name ?: stringResource(R.string.ai_book_source_tamer),
                    description = stringResource(
                        R.string.ai_workshop_skill_desc,
                        skill?.content?.length ?: 0
                    ),
                    trailingContent = {
                        Switch(
                            checked = skill?.enabled ?: true,
                            onCheckedChange = onToggleEnabled
                        )
                    },
                    onClick = onEditSkill
                )
                ClickableSettingItem(
                    title = stringResource(R.string.ai_workshop_view_content),
                    description = stringResource(R.string.ai_workshop_view_content_desc),
                    onClick = onEditSkill
                )
                ClickableSettingItem(
                    title = stringResource(R.string.ai_workshop_import),
                    description = stringResource(R.string.ai_workshop_import_desc),
                    onClick = {
                        val text = readClipboard(context)
                        if (text.isBlank()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.ai_workshop_clipboard_empty),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            onImportContent(text)
                        }
                    }
                )
            }

            // ---- 用它做一个书源 ----
            SplicedColumnGroup(title = stringResource(R.string.ai_workshop_make)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    AppTextField(
                        value = siteName,
                        onValueChange = { siteName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.ai_workshop_site_name),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    AppTextField(
                        value = siteUrl,
                        onValueChange = { siteUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.ai_workshop_site_url),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    AppText(
                        text = stringResource(R.string.ai_workshop_mode),
                        style = LegadoTheme.typography.labelMediumEmphasized,
                        color = LegadoTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MediumTonalButton(
                            onClick = { editExisting = false },
                            text = stringResource(R.string.ai_workshop_mode_new),
                            selected = !editExisting,
                        )
                        MediumTonalButton(
                            onClick = { editExisting = true },
                            text = stringResource(R.string.ai_workshop_mode_edit),
                            selected = editExisting,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = extra,
                        onValueChange = { extra = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.ai_workshop_extra),
                        minLines = 3,
                    )
                    Spacer(Modifier.height(16.dp))
                    MediumTonalButton(
                        onClick = {
                            onLaunch(
                                buildWorkshopBrief(
                                    siteName = siteName,
                                    siteUrl = siteUrl,
                                    editExisting = editExisting,
                                    extra = extra,
                                )
                            )
                        },
                        text = stringResource(R.string.ai_workshop_start),
                        enabled = siteUrl.isNotBlank(),
                    )
                    Spacer(Modifier.height(8.dp))
                    AppText(
                        text = stringResource(R.string.ai_workshop_start_hint),
                        style = LegadoTheme.typography.bodySmall,
                        color = LegadoTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun readClipboard(context: Context): String {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = manager?.primaryClip ?: return ""
    if (clip.itemCount == 0) return ""
    return clip.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
}

/**
 * 交给书源驯兽师的结构化简报：站点信息 + 模式 + 交付要求。
 * 不做任何规则拼装，避免和技能自己的流程冲突。
 */
private fun buildWorkshopBrief(
    siteName: String,
    siteUrl: String,
    editExisting: Boolean,
    extra: String,
): String = buildString {
    append("请使用「书源驯兽师」技能，")
    append(if (editExisting) "改造一个已有的书源。" else "从一个目标站点制作新书源。")
    append("\n\n")
    append("站点名称：").append(siteName.ifBlank { "未填写" }).append('\n')
    append("站点地址：").append(siteUrl.trim()).append('\n')
    append("任务模式：").append(if (editExisting) "改造已有书源" else "新建书源").append('\n')
    append("补充要求：").append(extra.ifBlank { "无" }).append("\n\n")
    append("请按技能流程执行：分析站点结构与可用接口 → 生成或修改书源规则 → ")
    append("用真实关键词验证搜索与详情 → 给出已知限制。")
    append("最后把书源 JSON 完整贴出来。")
}
