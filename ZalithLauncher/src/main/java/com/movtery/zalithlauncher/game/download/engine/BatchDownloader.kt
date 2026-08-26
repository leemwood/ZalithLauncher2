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

package com.movtery.zalithlauncher.game.download.engine

import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/** 整批下载结束后仍有未成功（且未被 [BatchDownloader.onFailureFilter] 接受）的文件 */
class BatchDownloadException internal constructor(summary: String) : IOException(summary)

/**
 * 批量下载编排器：
 * 所有文件共享一个全局连接信号量；大文件的分块决策基于整批聚合速度，
 * 低速时才追加分块连接，避免对下载源的请求风暴。
 * 每个失败文件在所有候选源耗尽后还会参与下一轮整批重试。
 */
class BatchDownloader(
    private val requests: List<DownloadRequest>,
    private val maxConnections: Int = DEFAULT_MAX_CONNECTIONS,
    private val retryRounds: Int = 1,
    client: OkHttpClient = OkHttpClient()
) {
    val stats = DownloadStats()

    /** 每 100ms 收到一次进度快照；回调运行在调度线程上，只应做轻量转发 */
    var onUpdate: (suspend (BatchProgress) -> Unit)? = null

    var onFileSuccess: (suspend (DownloadRequest) -> Unit)? = null

    /**
     * 文件重试轮次全部结束后仍失败的裁决：返回 true 表示接受现状继续
     * （例如可缺失的附加内容），false 则计入最终失败集合。
     */
    var onFailureFilter: ((DownloadRequest, Throwable) -> Boolean)? = null

    private val connections = Semaphore(maxConnections)
    private val fileClient: OkHttpClient = client

    /** 最近一次 run 结束后的失败清单（目标文件路径 → 异常），供调用方诊断 */
    var lastRunFailures: Map<String, Throwable> = emptyMap()
        private set

    suspend fun run() {
        stats.resetFiles()
        requests.forEach { stats.registerFile(it.expectedSize) }

        val failures = ConcurrentHashMap<String, Throwable>()
        coroutineScope {
            val reporter = launch(Dispatchers.Default) {
                while (isActive) {
                    delay(PROGRESS_INTERVAL_MS.milliseconds)
                    onUpdate?.invoke(stats.snapshotProgress())
                }
            }

            try {
                requests.map { request ->
                    launch(Dispatchers.IO) {
                        runOne(request, failures)
                    }
                }.joinAll()
            } finally {
                reporter.cancelAndJoin()
            }
        }

        if (failures.isNotEmpty()) {
            lastRunFailures = failures.toMap()
            throw BatchDownloadException(
                failures.entries.joinToString(separator = "\n") { (path, error) ->
                    "$path: ${error.message ?: error::class.simpleName}"
                }
            )
        }
    }

    private suspend fun runOne(request: DownloadRequest, failures: MutableMap<String, Throwable>) {
        var lastError: Throwable? = null
        repeat(retryRounds + 1) {
            try {
                FileDownloader(request, connections, stats, ::speedGate, client = fileClient).download()
                onFileSuccess?.invoke(request)
                stats.markFileFinished()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        lastError?.let { error ->
            Logger.error(TAG, "Download failed permanently: ${request.targetFile.absolutePath}", error)
            if (onFailureFilter?.invoke(request, error) == true) {
                stats.markFileFinished()
            } else {
                failures[request.targetFile.absolutePath] = error
            }
        }
    }

    /** 分块扩张的闸门：只有整批速度偏低时才允许新开连接拆段 */
    private fun speedGate(): Boolean = stats.refreshSpeed() < DownloadStats.LOW_SPEED_THRESHOLD_BPS

    companion object {
        private const val TAG = "BatchDownloader"
        const val DEFAULT_MAX_CONNECTIONS = 64
        const val PROGRESS_INTERVAL_MS = 100L
    }
}
