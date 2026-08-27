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

import java.util.concurrent.atomic.AtomicLong

/** 一份批量下载的只读进度快照，供 UI 层消费 */
data class BatchProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val downloadedFiles: Int,
    val totalFiles: Int,
    val speedBytesPerSec: Long
)

/**
 * 跨线程的字节/文件计数器，内嵌"逐秒分块"测速：
 * 每个报表值就是刚刚完整过去的一秒里真实落盘的字节数，每秒更新一次，
 * 不做任何跨窗口平滑或外推——报出来的数字永远有对应的实际流量。
 * 热路径只有一次原子加法。
 */
class DownloadStats internal constructor() {
    private val downloaded = AtomicLong(0L)

    @Volatile
    var expectedTotalBytes: Long = -1L
        private set

    @Volatile
    var totalFiles: Int = 0
        private set

    @Volatile
    var downloadedFiles: Int = 0
        private set

    fun addBytes(count: Long) {
        downloaded.addAndGet(count)
    }

    internal fun registerFile(expectedSize: Long) {
        totalFiles += 1
        if (expectedSize > 0) {
            val previous = expectedTotalBytes
            expectedTotalBytes = if (previous < 0) expectedSize else previous + expectedSize
        }
    }

    internal fun markFileFinished() {
        downloadedFiles += 1
    }

    /** 重置文件维度计数（整批重试时使用），字节计数保持累计 */
    internal fun resetFiles() {
        downloadedFiles = 0
        totalFiles = 0
        expectedTotalBytes = -1L
    }

    val downloadedBytes: Long get() = downloaded.get()

    /**
     * 返回上一个完整采样秒的真实平均吞吐；距上次采样不足一秒时返回原值。
     * 进度快照内部会先经过这里，UI 以任意频率轮询都只会看到每秒一格的变化。
     */
    fun refreshSpeed(): Long {
        val now = System.nanoTime()
        synchronized(this) {
            val elapsed = now - blockStartNanos
            if (elapsed < SAMPLE_INTERVAL_NANOS) return currentSpeed

            val bytes = downloaded.get()
            currentSpeed = ((bytes - blockStartBytes) * NANOS_PER_SEC / elapsed)
                .coerceAtLeast(0L)
            blockStartNanos = now
            blockStartBytes = bytes
            return currentSpeed
        }
    }

    /** 先刷新测速再产出快照，调用方无需单独触发采样 */
    fun snapshotProgress(): BatchProgress = BatchProgress(
        downloadedBytes = downloaded.get(),
        totalBytes = expectedTotalBytes,
        downloadedFiles = downloadedFiles,
        totalFiles = totalFiles,
        speedBytesPerSec = refreshSpeed()
    )

    private var blockStartNanos = System.nanoTime()
    private var blockStartBytes = 0L

    private var currentSpeed: Long = 0L

    companion object {
        /** 引擎判定"速度偏低需要补连接"的水位线 */
        const val LOW_SPEED_THRESHOLD_BPS: Long = 256L * 1024L

        /** 速率采样周期：每个报表值统计刚刚完整过去的一秒 */
        private const val SAMPLE_INTERVAL_NANOS = 1_000_000_000L
        private const val NANOS_PER_SEC = 1_000_000_000L
    }
}
