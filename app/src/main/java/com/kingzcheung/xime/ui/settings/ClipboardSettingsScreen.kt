package com.kingzcheung.xime.ui.settings

import com.kingzcheung.xime.R

import com.kingzcheung.xime.i18n.tr

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.twotone.DataUsage
import androidx.compose.material.icons.twotone.Image
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material.icons.twotone.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingzcheung.xime.clipboard.ClipboardManager
import com.kingzcheung.xime.clipboard.ImageUsage
import com.kingzcheung.xime.settings.SettingsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 剪贴板设置（设置 → 数据与同步 → 剪贴板）。
 *
 * 页面只保留"需要用户决定/只有此处能看到"的东西：图片采集开关、敏感内容开关、单张上限，
 * 以及调上限时作为参照的图片占用；**清理不在这里**——剪贴板历史（文字 + 图片 + 残留）
 * 统一在 关于 → 存储空间 → 「剪贴板历史」清理，避免两个入口做同一件事。
 * 其余限制（最长边 / 保留数量 / 总容量）是内置常量，不暴露也不罗列。
 * 「剪贴板同步」的**总开关就在本页**（就地可切），整行点击进入内页做「选服务 + 配置」——
 * 同步是剪贴板的一种流向，不另立设置分组；开关上提后无需为开关专门进一层页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardSettingsContent(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = remember { ClipboardManager.getInstance(context) }

    var captureEnabled by remember {
        mutableStateOf(SettingsPreferences.isClipboardImageCaptureEnabled(context))
    }
    var sensitiveCaptureEnabled by remember {
        mutableStateOf(SettingsPreferences.isClipboardSensitiveCaptureEnabled(context))
    }
    var maxMb by remember {
        mutableFloatStateOf(SettingsPreferences.getClipboardImageMaxMb(context).toFloat())
    }
    var usage by remember { mutableStateOf<ImageUsage?>(null) }

    // 占用统计读的是数据库 + 图片目录，放到 IO 线程
    LaunchedEffect(Unit) {
        usage = withContext(Dispatchers.IO) { clipboardManager.imageUsage() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(tr(R.string.s_jiantieban)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr(R.string.s_fanhui)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(
                title = tr(R.string.s_tupianjilu),
                content = {
                    SettingsToggleItem(
                        icon = Icons.TwoTone.Image,
                        title = tr(R.string.s_shoujifuzhidetupian),
                        subtitle = tr(R.string.s_fuzhitupianshibaocundaojiantie_6d667),
                        checked = captureEnabled,
                        onCheckedChange = { checked ->
                            captureEnabled = checked
                            SettingsPreferences.setClipboardImageCaptureEnabled(context, checked)
                        }
                    )
                    if (!captureEnabled) {
                        Text(
                            text = tr(R.string.s_yiguanbi_zhishoujiwenzi_fuzhit_8bf4d),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        )
                    }
                }
            )

            SettingsSection(
                title = tr(R.string.s_minganneirong),
                content = {
                    SettingsToggleItem(
                        icon = Icons.TwoTone.Lock,
                        title = tr(R.string.s_jilumimadengminganneirong),
                        subtitle = tr(R.string.s_morenbubaocunmimaguanliqibiaoj_9d5df),
                        checked = sensitiveCaptureEnabled,
                        onCheckedChange = { checked ->
                            sensitiveCaptureEnabled = checked
                            SettingsPreferences.setClipboardSensitiveCaptureEnabled(context, checked)
                        }
                    )
                    if (sensitiveCaptureEnabled) {
                        Text(
                            text = tr(R.string.s_yikaiqi_minganneironghuibaocun_3f987),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        )
                    }
                }
            )

            SettingsSection(
                title = tr(R.string.s_tupianshangxian),
                content = {
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 12.dp
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tr(R.string.s_danzhangzuidatiji),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${maxMb.toInt()} MB",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = maxMb,
                            onValueChange = { maxMb = it },
                            onValueChangeFinished = {
                                SettingsPreferences.setClipboardImageMaxMb(context, maxMb.toInt())
                            },
                            valueRange = SettingsPreferences.CLIPBOARD_IMAGE_MAX_MB_RANGE.first.toFloat()..
                                SettingsPreferences.CLIPBOARD_IMAGE_MAX_MB_RANGE.last.toFloat(),
                            steps = 18
                        )
                        Text(
                            text = tr(R.string.s_chaoguoshangxiandetupianbuhuib_f2e2d),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.DataUsage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tr(R.string.s_tupianzhanyong),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = usage?.let {
                                tr(R.string.s_zhang, it.count, Formatter.formatFileSize(context, it.fileBytes))
                            } ?: tr(R.string.s_tongjizhong),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

        }
    }
}