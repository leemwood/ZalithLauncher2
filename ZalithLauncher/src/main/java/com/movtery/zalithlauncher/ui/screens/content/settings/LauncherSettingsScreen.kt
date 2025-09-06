package com.movtery.zalithlauncher.ui.screens.content.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.MirrorSourceType
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.base.FullScreenComponentActivity
import com.movtery.zalithlauncher.ui.components.ColorPickerDialog
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsBackground
import com.movtery.zalithlauncher.ui.theme.ColorThemeType
import com.movtery.zalithlauncher.utils.animation.TransitionAnimationType
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.file.shareFile
import com.movtery.zalithlauncher.utils.file.zipDirectory
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import java.io.File
import androidx.compose.foundation.layout.height
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.EnumSettingsLayout
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.ListSettingsLayout
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.ShareLogLayout
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SliderSettingsLayout
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SwitchSettingsLayout

private sealed interface CustomColorOperation {
    data object None : CustomColorOperation
    /** 展示自定义主题颜色 Dialog */
    data object Dialog: CustomColorOperation
}

@Composable
fun LauncherSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: NavKey?,
    mainScreenKey: NavKey?
) {
    val context = LocalContext.current

    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Launcher, settingsScreenKey, false)
    ) { isVisible ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState())
                .padding(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val yOffset1 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible
            )

            SettingsBackground(
                modifier = Modifier.offset { IntOffset(x = 0, y = yOffset1.roundToPx()) }
            ) {
                var customColorOperation by remember { mutableStateOf<CustomColorOperation>(CustomColorOperation.None) }
                CustomColorOperation(
                    customColorOperation = customColorOperation,
                    updateOperation = { customColorOperation = it }
                )

                EnumSettingsLayout(
                    unit = AllSettings.launcherColorTheme,
                    title = stringResource(R.string.settings_launcher_color_theme_title),
                    summary = stringResource(R.string.settings_launcher_color_theme_summary),
                    entries = ColorThemeType.entries,
                    getRadioEnable = { enum ->
                        if (enum == ColorThemeType.DYNAMIC) Build.VERSION.SDK_INT >= Build.VERSION_CODES.S else true
                    },
                    getRadioText = { enum ->
                        when (enum) {
                            ColorThemeType.DYNAMIC -> stringResource(R.string.theme_color_dynamic)
                            ColorThemeType.EMBERMIRE -> stringResource(R.string.theme_color_embermire)
                            ColorThemeType.VELVET_ROSE -> stringResource(R.string.theme_color_velvet_rose)
                            ColorThemeType.MISTWAVE -> stringResource(R.string.theme_color_mistwave)
                            ColorThemeType.GLACIER -> stringResource(R.string.theme_color_glacier)
                            ColorThemeType.VERDANTFIELD -> stringResource(R.string.theme_color_verdant_field)
                            ColorThemeType.URBAN_ASH -> stringResource(R.string.theme_color_urban_ash)
                            ColorThemeType.VERDANT_DAWN -> stringResource(R.string.theme_color_verdant_dawn)
                            ColorThemeType.CUSTOM -> stringResource(R.string.generic_custom)
                        }
                    },
                    onRadioClick = { enum ->
                        if (enum == ColorThemeType.CUSTOM) customColorOperation = CustomColorOperation.Dialog
                    }
                )

                SwitchSettingsLayout(
                    unit = AllSettings.launcherFullScreen,
                    title = stringResource(R.string.settings_launcher_full_screen_title),
                    summary = stringResource(R.string.settings_launcher_full_screen_summary),
                    onCheckedChange = {
                        val activity = context as? FullScreenComponentActivity
                        activity?.fullScreenViewModel?.triggerRefresh()
                    }
                )
            }

            val yOffset2 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible,
                delayMillis = 50
            )

            //动画设置板块
            SettingsBackground(
                modifier = Modifier.offset { IntOffset(x = 0, y = yOffset2.roundToPx()) }
            ) {
                SliderSettingsLayout(
                    unit = AllSettings.launcherAnimateSpeed,
                    title = stringResource(R.string.settings_launcher_animate_speed_title),
                    summary = stringResource(R.string.settings_launcher_animate_speed_summary),
                    valueRange = 0f..10f,
                    steps = 9,
                    suffix = "x"
                )

                SliderSettingsLayout(
                    unit = AllSettings.launcherAnimateExtent,
                    title = stringResource(R.string.settings_launcher_animate_extent_title),
                    summary = stringResource(R.string.settings_launcher_animate_extent_summary),
                    valueRange = 0f..10f,
                    steps = 9,
                    suffix = "x"
                )

                EnumSettingsLayout(
                    unit = AllSettings.launcherSwapAnimateType,
                    title = stringResource(R.string.settings_launcher_swap_animate_type_title),
                    summary = stringResource(R.string.settings_launcher_swap_animate_type_summary),
                    entries = TransitionAnimationType.entries,
                    getRadioEnable = { true },
                    getRadioText = { enum ->
                        stringResource(enum.textRes)
                    }
                )
            }

            val yOffset3 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible,
                delayMillis = 100
            )

            SettingsBackground(
                modifier = Modifier.offset { IntOffset(x = 0, y = yOffset3.roundToPx()) }
            ) {
                // 添加背景图片设置选项
                BackgroundImageSettings()

                ListSettingsLayout(
                    unit = AllSettings.fetchModLoaderSource,
                    items = MirrorSourceType.entries,
                    title = stringResource(R.string.settings_launcher_mirror_modloader_title),
                    getItemText = { stringResource(it.textRes) }
                )

                ListSettingsLayout(
                    unit = AllSettings.fileDownloadSource,
                    items = MirrorSourceType.entries,
                    title = stringResource(R.string.settings_launcher_mirror_file_download_title),
                    getItemText = { stringResource(it.textRes) }
                )

                SliderSettingsLayout(
                    unit = AllSettings.launcherLogRetentionDays,
                    title = stringResource(R.string.settings_launcher_log_retention_days_title),
                    summary = stringResource(R.string.settings_launcher_log_retention_days_summary),
                    valueRange = 1f..14f,
                    suffix = stringResource(R.string.unit_day)
                )

                ShareLogLayout(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        TaskSystem.submitTask(
                            Task.runTask(
                                id = "ZIP_LOGS",
                                task = { task ->
                                    task.updateProgress(-1f, R.string.settings_launcher_log_share_packing)
                                    val logsFile = File(PathManager.DIR_CACHE, "logs.zip")
                                    zipDirectory(
                                        PathManager.DIR_LAUNCHER_LOGS,
                                        logsFile
                                    )
                                    task.updateProgress(1f, null)
                                    //分享压缩包
                                    shareFile(
                                        context = context,
                                        file = logsFile
                                    )
                                },
                                onError = { e ->
                                    lError("Failed to package log files.", e)
                                }
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun BackgroundImageSettings() {
    val context = LocalContext.current
    var backgroundImage by AllSettings.launcherBackgroundImage.state

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { 
            // 保存图片URI到设置中
            backgroundImage = it.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(22.0.dp))
            .clickable { imagePicker.launch("image/*") }
            .padding(all = 8.dp)
            .padding(bottom = 4.dp)
    ) {
        TitleAndSummary(
            title = stringResource(R.string.settings_launcher_background_image_title),
            summary = stringResource(R.string.settings_launcher_background_image_summary)
        )

        // 显示当前选中的背景图片预览
        if (backgroundImage.isNotEmpty()) {
            AsyncImage(
                model = backgroundImage,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(100.dp)
            )
        }
    }
}

@Composable
private fun CustomColorOperation(
    customColorOperation: CustomColorOperation,
    updateOperation: (CustomColorOperation) -> Unit
) {
    when (customColorOperation) {
        is CustomColorOperation.None -> {}
        is CustomColorOperation.Dialog -> {
            ColorPickerDialog(
                initialColor = Color(AllSettings.launcherCustomColor.getValue()),
                realTimeUpdate = false,
                onDismissRequest = {
                    updateOperation(CustomColorOperation.None)
                },
                onConfirm = { color ->
                    AllSettings.launcherCustomColor.save(color.toArgb())
                    updateOperation(CustomColorOperation.None)
                },
                showAlpha = false,
                showBrightness = false
            )
        }
    }
}

@Composable
private fun ShareLogLayout(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .clip(shape = RoundedCornerShape(22.0.dp))
            .clickable(onClick = onClick)
            .padding(all = 8.dp)
            .padding(bottom = 4.dp)
    ) {
        TitleAndSummary(
            title = stringResource(R.string.settings_launcher_log_share_title),
            summary = stringResource(R.string.settings_launcher_log_share_summary)
        )
    }
}