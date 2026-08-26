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

import com.movtery.zalithlauncher.game.download.engine.DownloadStats.Companion.SAMPLE_INTERVAL_NANOS
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
 * 跨线程的字节/文件计数器，内嵌"滑动一秒窗口"的实时测速；
 * 热路径只有一次原子加法，采样与淘汰全部运行在复用的环形缓冲上。
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
     * 以最近约一秒的实际吞吐返回字节数/秒；两次调用间隔不足 [SAMPLE_INTERVAL_NANOS] 时直接返回上次结果。
     * 进度快照会先经过这里刷新，保证 UI 读到的速率始终反映当前瞬间。
     */
    fun refreshSpeed(): Long {
        val now = System.nanoTime()
        synchronized(this) {
            if (now - lastSampleNanos < SAMPLE_INTERVAL_NANOS) return currentSpeed

            lastSampleNanos = now
            val bytes = downloaded.get()

            //写入新样本（环形缓冲，零分配）
            val tail = (windowHead + windowCount).let { if (it >= WINDOW_CAPACITY) it - WINDOW_CAPACITY else it }
            windowStamps[tail] = now
            windowBytes[tail] = bytes
            if (windowCount == WINDOW_CAPACITY) {
                windowHead = if (tail + 1 >= WINDOW_CAPACITY) 0 else tail + 1
            } else {
                windowCount++
            }

            //淘汰比窗口更旧的样本，但始终保留最新一个
            while (windowCount > 1 && now - windowStamps[windowHead] > WINDOW_NANOS) {
                windowHead = if (windowHead + 1 >= WINDOW_CAPACITY) 0 else windowHead + 1
                windowCount--
            }

            val span = now - windowStamps[windowHead]
            if (span >= MIN_SPAN_NANOS) {
                currentSpeed = ((bytes - windowBytes[windowHead]) * NANOS_PER_SEC / span)
            }
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

    private val windowStamps = LongArray(WINDOW_CAPACITY)
    private val windowBytes = LongArray(WINDOW_CAPACITY)
    private var windowHead = 0
    private var windowCount = 0

    private var lastSampleNanos = System.nanoTime()

    private var currentSpeed: Long = 0L

    companion object {
        /** 引擎判定"速度偏低需要补连接"的水位线 */
        const val LOW_SPEED_THRESHOLD_BPS: Long = 256L * 1024L

        private const val WINDOW_CAPACITY = 16
        private const val WINDOW_NANOS = 1_000_000_000L
        private const val SAMPLE_INTERVAL_NANOS = 80_000_000L
        private const val MIN_SPAN_NANOS = 50_000_000L
        private const val NANOS_PER_SEC = 1_000_000_000L
    }
}
