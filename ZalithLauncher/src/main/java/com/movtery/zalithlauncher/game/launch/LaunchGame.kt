/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.launch

import android.content.Context
import com.google.gson.JsonObject
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.auth_server.AuthServerHelper
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.account.isMicrosoftAccount
import com.movtery.zalithlauncher.game.account.isReloginRequired
import com.movtery.zalithlauncher.game.account.microsoft.validateAccessToken
import com.movtery.zalithlauncher.game.account.refreshMicrosoft
import com.movtery.zalithlauncher.game.version.download.DownloadMode
import com.movtery.zalithlauncher.game.version.download.MinecraftDownloader
import com.movtery.zalithlauncher.game.version.installed.GraphicsApi
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.mod.AllModReader
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.activities.runGame
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.file.readText
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.isNetworkAvailable
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipFile

private const val TAG = "LaunchGame"

object LaunchGame {
    var isLaunching: Boolean = false
        private set

    fun launchGame(
        context: Context,
        version: Version,
        exitActivity: () -> Unit,
        waitForVulkanChecker: suspend () -> Unit,
        submitError: (ErrorViewModel.ThrowableMessage) -> Unit,
        onReloginRequired: (Account) -> Unit = {},
        onRefreshFailed: (Account, Throwable) -> Unit = { _, _ -> },
        skipAccountRefresh: Boolean = false
    ) {
        if (isLaunching) return
        val account = AccountsManager.currentAccountFlow.value ?: return
        isLaunching = true

        //检查是否联网，根据这个条件决定是否登录账号
        //以及，没有联网时，让微软账号、外置账号作为离线账号登录
        val hasNetwork = isNetworkAvailable(context)

        val downloadTask = createDownloadTask(
            context = context,
            version = version,
            account = account,
            exitActivity = exitActivity,
            waitForVulkanChecker = waitForVulkanChecker,
            submitError = submitError
        )
        fun startDownloadTask() {
            TaskSystem.submitTask(downloadTask) { isLaunching = false }
        }

        val loginTask = createLoginTask(
            context = context,
            hasNetwork = hasNetwork,
            account = account,
            skipRefresh = skipAccountRefresh,
            onLoginSuccess = { startDownloadTask() },
            onReloginRequired = {
                isLaunching = false
                onReloginRequired(account)
            },
            onRefreshFailed = { th ->
                isLaunching = false
                onRefreshFailed(account, th)
            }
        )

        if (loginTask != null) {
            TaskSystem.submitTask(loginTask)
        } else {
            if (!hasNetwork && !account.isLocalAccount()) {
                //没联网时作为离线账号登录
                version.offlineAccountLogin = true
            }
            startDownloadTask()
        }
    }

    private fun createDownloadTask(
        context: Context,
        version: Version,
        account: Account,
        exitActivity: () -> Unit,
        waitForVulkanChecker: suspend () -> Unit,
        submitError: (ErrorViewModel.ThrowableMessage) -> Unit
    ): Task {
        return MinecraftDownloader(
            context = context,
            version = version.getVersionInfo()?.minecraftVersion ?: version.getVersionName(),
            customName = version.getVersionName(),
            verifyIntegrity = !version.skipGameIntegrityCheck(),
            mode = DownloadMode.VERIFY_AND_REPAIR,
            onCompletion = { task ->
                task.updateProgress(-1f)
                task.updateMessage(null)
                checkEnableTouchProxy(version)
                task.updateMessage(androidText(R.string.game_vulkan_check_title))
                checkVulkanCapabilities(version, waitForVulkanChecker)

                runGame(context, version, account)
                exitActivity()
            },
            onError = { message ->
                submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.minecraft_download_failed),
                        message = androidText(message)
                    )
                )
            }
        ).getDownloadTask()
    }

    /**
     * 检查是否安装了 TouchController，安装后开启控制代理
     */
    private suspend fun checkEnableTouchProxy(version: Version) {
        val modsDir = VersionFolders.MOD.getDir(version.getGameDir())
        val reader = AllModReader(modsDir)
        for (mod in reader.readAllLocals()) {
            if (mod.id == "touchcontroller") {
                version.enableTouchProxy = true
                break
            }
        }
    }

    private suspend fun checkVulkanCapabilities(
        version: Version,
        waitForVulkanChecker: suspend () -> Unit
    ) {
        if (!AllSettings.autoVulkanChecker.getValue()) return

        val api = version.getGraphicsApi()
        if (api == GraphicsApi.OPENGL) return

        //游戏可能使用Vulkan，检查版本是否为 26.2+
        val clientJar = version.getClientJar()
        if (clientJar.exists()) {
            val hasVulkan = runCatching {
                withContext(Dispatchers.IO) {
                    //在客户端中读取数据版本
                    ZipFile(clientJar).use { zip ->
                        zip.getEntry("version.json")
                            ?.readText(zip)
                            ?.let { GSON.fromJson(it, JsonObject::class.java) }
                            ?.let { json ->
                                //https://zh.minecraft.wiki/w/%E7%89%88%E6%9C%AC%E4%BF%A1%E6%81%AF%E6%96%87%E4%BB%B6%E6%A0%BC%E5%BC%8F
                                json.get("world_version")?.asInt
                            }
                    }?.let { worldVersion ->
                        //26.2-snapshot-1
                        worldVersion >= 4883
                    }
                } ?: false
            }.onFailure { e ->
                Logger.warning(TAG, "Unable to determine the data version of this client Jar, possibly due to an outdated version.", e)
            }.getOrDefault(false)

            if (hasVulkan) {
                //等待Vulkan检查完成
                waitForVulkanChecker()
            }
        }
    }

    private fun createLoginTask(
        context: Context,
        hasNetwork: Boolean,
        account: Account,
        skipRefresh: Boolean,
        onLoginSuccess: () -> Unit,
        onReloginRequired: () -> Unit,
        onRefreshFailed: (Throwable) -> Unit
    ): Task? {
        if (!hasNetwork || skipRefresh) return null
        if (!AccountsManager.isLaunchCheckNeeded(account)) return null

        //账号管理页已在刷新该账号时，直接使用现有凭据启动
        val runningTaskId = if (account.isMicrosoftAccount()) account.profileId else account.uniqueUUID
        if (TaskSystem.containsTask(runningTaskId)) return null

        val onCheckFailed: (Throwable) -> Unit = { error ->
            if (error.isReloginRequired()) onReloginRequired()
            else onRefreshFailed(error)
        }

        return if (account.isMicrosoftAccount()) {
            createMicrosoftCheckTask(account, onLoginSuccess, onCheckFailed)
        } else {
            createOtherCheckTask(context, account, onLoginSuccess, onCheckFailed)
        }
    }

    private fun createMicrosoftCheckTask(
        account: Account,
        onLoginSuccess: () -> Unit,
        onCheckFailed: (Throwable) -> Unit
    ): Task = Task.runTask(
        id = account.profileId,
        dispatcher = Dispatchers.IO,
        task = { task ->
            val expired = System.currentTimeMillis() > account.expiresAt - 5 * 60 * 1000
            if (expired || !validateAccessToken(account)) {
                account.refreshMicrosoft(task, coroutineContext)
                AccountsManager.suspendSaveAccount(account)
            }
            AccountsManager.markSessionValidated(account)
            onLoginSuccess()
        },
        onError = { e ->
            if (e !is CancellationException) onCheckFailed(e)
        },
        onCancel = { isLaunching = false }
    )

    private fun createOtherCheckTask(
        context: Context,
        account: Account,
        onLoginSuccess: () -> Unit,
        onCheckFailed: (Throwable) -> Unit
    ): Task = Task.runTask(
        id = account.uniqueUUID,
        dispatcher = Dispatchers.IO,
        task = { task ->
            task.updateMessage(androidText(R.string.account_logging_in, account.username))
            val helper = AuthServerHelper(
                baseUrl = account.otherBaseUrl!!,
                serverName = account.accountType!!,
                email = account.otherAccount!!,
                password = account.otherPassword!!
            )
            if (!helper.validateOrRefresh(context, account)) {
                helper.passwordLogin(context, account)
            }
            AccountsManager.suspendSaveAccount(account)
            AccountsManager.markSessionValidated(account)
            onLoginSuccess()
        },
        onError = { e ->
            if (e !is CancellationException) onCheckFailed(e)
        },
        onCancel = { isLaunching = false }
    )
}
