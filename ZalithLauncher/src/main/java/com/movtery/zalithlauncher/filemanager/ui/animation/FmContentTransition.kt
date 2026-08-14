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

package com.movtery.zalithlauncher.filemanager.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.AndroidUiDispatcher
import com.movtery.zalithlauncher.filemanager.ui.theme.FmAnimations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 列表内容切换动画生命周期
 */
enum class FmContentPhase {
    /** 内容完全可见 */
    VISIBLE,
    /** 正在淡出旧内容 */
    FADING_OUT,
    /** 旧内容已淡出 */
    WAITING,
    /** 新内容就绪，正在淡入 */
    FADING_IN,
}

class FmContentTransition {
    private val animatable = Animatable(0f)

    private val _progress = MutableStateFlow(0f)
    private val _phase = MutableStateFlow(FmContentPhase.VISIBLE)
    private val _inputBlocked = MutableStateFlow(false)

    /** 进度状态 */
    val progress: StateFlow<Float> = _progress.asStateFlow()
    /** 动画生命周期状态 */
    val phase: StateFlow<FmContentPhase> = _phase.asStateFlow()
    /** 是否启用透明输入拦截层 */
    val inputBlocked: StateFlow<Boolean> = _inputBlocked.asStateFlow()

    /**
     * 淡出旧内容并等待动画完成
     */
    suspend fun fadeOut() {
        if (animatable.value >= 1f) return

        _phase.value = FmContentPhase.FADING_OUT
        _inputBlocked.value = true
        animateSafely {
            animatable.animateTo(1f, tween(FmAnimations.FADE_OUT_MS)) {
                _progress.value = value
            }
        }
        _progress.value = animatable.value
        _phase.value = FmContentPhase.WAITING
    }

    /**
     * 淡入新内容
     */
    suspend fun fadeIn() {
        if (animatable.value <= 0f) return
        _inputBlocked.value = false
        _phase.value = FmContentPhase.FADING_IN
        animateSafely {
            animatable.animateTo(0f, tween(FmAnimations.FADE_IN_MS)) {
                _progress.value = value
            }
        }
        _progress.value = animatable.value
        _phase.value = FmContentPhase.VISIBLE
    }

    /** 预见性返回手势进度（0..1）直接接管淡出 */
    suspend fun followGesture(p: Float) {
        animateSafely { animatable.snapTo(p) }
        _progress.value = animatable.value
    }

    /** 完成淡出并启用拦截；淡入由刷新流程的 [fadeIn] 接管 */
    suspend fun commitGesture() {
        _phase.value = FmContentPhase.FADING_OUT
        _inputBlocked.value = true
        animateSafely {
            animatable.animateTo(1f, tween(FmAnimations.FADE_OUT_MS)) {
                _progress.value = value
            }
        }
        _progress.value = animatable.value
        _phase.value = FmContentPhase.WAITING
    }

    /** 手势取消：撤除拦截并回弹到可见 */
    suspend fun cancelGesture() {
        _inputBlocked.value = false
        animateSafely {
            animatable.animateTo(0f, spring()) {
                _progress.value = value
            }
        }
        _progress.value = animatable.value
        _phase.value = FmContentPhase.VISIBLE
    }

    private suspend fun animateSafely(block: suspend () -> Unit) {
        try {
            withContext(AndroidUiDispatcher.Main) {
                block()
            }
        } catch (_: CancellationException) {
            // 动画被打断：忽略，继续推进生命周期
        }
    }
}
