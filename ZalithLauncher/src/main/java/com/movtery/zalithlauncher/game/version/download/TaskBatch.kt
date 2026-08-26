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

package com.movtery.zalithlauncher.game.version.download

import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.game.download.engine.BatchDownloader
import com.movtery.zalithlauncher.game.download.engine.BatchProgress
import kotlinx.coroutines.CancellationException

/**
 * 把一批 [DownloadTask] 交给下载引擎执行的统一入口：
 * 先挑出本地已可复用的文件（直接计入完成并执行挂后任务），
 * 剩余的交给引擎分块并发下载；仍有失败时抛出 [DownloadFailedException]，
 * message 内含失败文件路径。
 *
 * @param onSnapshot 每 100ms 收到一次引擎统计快照
 */
suspend fun Task.runBatchDownloads(
    tasks: List<DownloadTask>,
    maxConnections: Int,
    retryRounds: Int = 1,
    onSnapshot: suspend (BatchProgress) -> Unit = {},
    acceptFailure: ((task: DownloadTask, error: Throwable) -> Boolean)? = null
) {
    val (reusable, pending) = tasks.partition { it.existingFileValid() }

    val batch = BatchDownloader(
        requests = pending.map { it.toRequest() },
        maxConnections = maxConnections,
        retryRounds = retryRounds
    )

    reusable.forEach {
        batch.stats.registerFile(it.size)
        batch.stats.markFileFinished()
        it.runFileDownloadedTask()
    }

    batch.onUpdate = { snapshot ->
        val totalBytes = if (snapshot.totalBytes < snapshot.downloadedBytes) snapshot.downloadedBytes else snapshot.totalBytes
        updateProgress(
            if (totalBytes > 0) (snapshot.downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else -1f
        )
        onSnapshot(snapshot.copy(totalBytes = totalBytes))
    }
    batch.onFileSuccess = { request ->
        (request.tag as? DownloadTask)?.runFileDownloadedTask()
    }
    acceptFailure?.let { judge ->
        batch.onFailureFilter = { request, error ->
            (request.tag as? DownloadTask)?.let { judge(it, error) } ?: false
        }
    }

    try {
        batch.run()
        updateProgress(1f)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val failedDetail = batch.lastRunFailures.keys.joinToString("\n") { path -> path }
        throw DownloadFailedException("Failed downloads:\n$failedDetail", e)
    }
}

