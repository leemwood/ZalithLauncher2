package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.base.BaseScreenWithBackground
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.VersionIconImage
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.ScreenBackStackViewModel
import com.movtery.zalithlauncher.ui.screens.content.elements.getLocalSkinWarningButton
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.viewmodel.LaunchGameViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import net.kdt.pojavlaunch.PojavLauncherActivity

@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    launchGameViewModel: LaunchGameViewModel
) {
    BaseScreenWithBackground(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreen.currentKey
    ) { isVisible ->
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            ContentMenu(
                isVisible = isVisible,
                modifier = Modifier.weight(7f),
                backStackViewModel = backStackViewModel,
                navigateToVersions = navigateToVersions
            )

            RightMenu(
                isVisible = isVisible,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .padding(top = 12.dp, end = 12.dp, bottom = 12.dp),
                launchGameViewModel = launchGameViewModel,
                toAccountManageScreen = {
                    backStackViewModel.mainScreen.navigateTo(NormalNavKey.AccountManager)
                },
                toVersionManageScreen = {
                    backStackViewModel.mainScreen.removeAndNavigateTo(
                        remove = NestedNavKey.VersionSettings::class,
                        screenKey = NormalNavKey.VersionsManager
                    )
                },
                toVersionSettingsScreen = {
                    VersionsManager.currentVersion?.let { version ->
                        navigateToVersions(version)
                    }
                },
                toDownloadScreen = { projectId, platform, classes ->
                    backStackViewModel.navigateToDownload(
                        targetScreen = backStackViewModel.downloadModScreen.apply {
                            navigateTo(
                                NormalNavKey.DownloadAssets(platform, projectId, classes)
                            )
                        }
                    )
                }
            )
        }
    }
}


@Composable
private fun RightMenu(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    launchGameViewModel: LaunchGameViewModel,
    toAccountManageScreen: () -> Unit,
    toVersionManageScreen: () -> Unit,
    toVersionSettingsScreen: () -> Unit,
    toDownloadScreen: (String, Platform, PlatformClasses) -> Unit
) {
    val yOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible
    )

    Card(
        modifier = modifier.offset { IntOffset(x = xOffset.roundToPx(), y = 0) },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        val account by AccountsManager.currentAccountFlow.collectAsState()
        val version = VersionsManager.currentVersion

        ConstraintLayout(
            modifier = Modifier.fillMaxSize()
        ) {
            val (accountAvatar, skinWarningLayout, versionManagerLayout, launchButton) = createRefs()

            AccountAvatar(
                modifier = Modifier
                    .constrainAs(accountAvatar) {
                        top.linkTo(parent.top)
                        bottom.linkTo(launchButton.top, margin = 32.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                account = account,
                onClick = toAccountManageScreen
            )

            val skinWarning: (@Composable () -> Unit)? = version?.getVersionInfo()?.let { versionInfo ->
                account?.let { acc1 ->
                    //离线账号皮肤相关的警告
                    getLocalSkinWarningButton(
                        account = acc1,
                        versionInfo = versionInfo,
                        swapToAccountScreen = toAccountManageScreen,
                        swapToDownloadScreen = toDownloadScreen
                    )
                }
            }

            skinWarning?.let { button ->
                Column(
                    modifier = Modifier
                        .constrainAs(skinWarningLayout) {
                            top.linkTo(accountAvatar.top, margin = 50.dp)
                            start.linkTo(accountAvatar.start, margin = 50.dp)
                        }
                ) {
                    button()
                }
            }

            Row(
                modifier = Modifier.constrainAs(versionManagerLayout) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(launchButton.top)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                VersionManagerLayout(
                    version = version,
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f)
                        .padding(8.dp),
                    swapToVersionManage = toVersionManageScreen
                )
                version?.takeIf { it.isValid() }?.let {
                    IconButton(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = toVersionSettingsScreen
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.versions_manage_settings)
                        )
                    }
                }
            }

            ScalingActionButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(launchButton) {
                        bottom.linkTo(parent.bottom, margin = 8.dp)
                    }
                    .padding(PaddingValues(horizontal = 12.dp)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                onClick = {
                    launchGameViewModel.tryLaunch(
                        VersionsManager.currentVersion
                    )
                },
            ) {
                MarqueeText(text = stringResource(R.string.main_launch_game))
            }
        }
    }
}

@Composable
private fun VersionManagerLayout(
    version: Version?,
    modifier: Modifier = Modifier,
    swapToVersionManage: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(onClick = swapToVersionManage)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 8.dp, vertical = 4.dp))
        ) {
            if (VersionsManager.isRefreshing) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center))
                }
            } else {
                VersionIconImage(
                    version = version,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(8.dp))

                if (version == null) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .basicMarquee(iterations = Int.MAX_VALUE),
                        text = stringResource(R.string.versions_manage_no_versions),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                    ) {
                        Text(
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                            text = version.getVersionName(),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                        if (version.isValid()) {
                            Text(
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                                text = version.getVersionSummary(),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}