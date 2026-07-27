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

package com.movtery.layer_controller.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.movtery.layer_controller.event.EventHandler
import com.movtery.layer_controller.observable.DefaultObservableJoystickStyle
import com.movtery.layer_controller.observable.ObservableControlLayer
import com.movtery.layer_controller.observable.ObservableJoystickData
import com.movtery.layer_controller.observable.ObservableJoystickData.Companion.toRegion
import com.movtery.layer_controller.observable.ObservableJoystickStyle
import com.movtery.layer_controller.observable.ObservableWidget
import com.movtery.layer_controller.observable.PointerEventBus
import com.movtery.layer_controller.utils.buttonSize
import com.movtery.layer_controller.utils.editMode
import com.movtery.layer_controller.utils.snap.GuideLine
import com.movtery.layer_controller.utils.snap.SnapMode

/**
 * 摇杆控件渲染组件
 * 完整迁移自 StyleableJoystick，支持：
 * - 自定义形状（圆角矩形/圆形）背景层和摇杆
 * - 死区、前进锁、锁定标记
 * - 多种颜色状态（正常/可锁定/已锁定）
 * - Alpha 不透明度处理
 * - 区域命中检测
 */
@Composable
internal fun JoystickWidgetRenderer(
    data: ObservableJoystickData,
    joystickStyles: List<ObservableJoystickStyle>,
    screenSize: IntSize,
    isDark: Boolean,
    visible: Boolean = true,
    pointerEventBus: PointerEventBus? = null,
    eventHandler: EventHandler? = null,
    reversedLayers: List<ObservableControlLayer>? = null,
    onOccupiedPointer: (PointerId) -> Unit = {},
    onReleasePointer: (PointerId) -> Unit = {},
    isEditMode: Boolean = false,
    enableSnap: Boolean = false,
    snapMode: SnapMode = SnapMode.Local,
    localSnapRange: Dp = 20.dp,
    getOtherWidgets: () -> List<ObservableWidget> = { emptyList() },
    snapThresholdValue: Dp = 4.dp,
    drawLine: (ObservableWidget, List<GuideLine>) -> Unit = { _, _ -> },
    onLineCancel: (ObservableWidget) -> Unit = {},
    onTapInEditMode: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // 获取样式
    val joystickStyle = data.joystickStyleId?.let { id ->
        joystickStyles.find { it.uuid == id }
    }

    val themeConfig = if (isDark) {
        joystickStyle?.darkStyle ?: DefaultObservableJoystickStyle.darkStyle
    } else {
        joystickStyle?.lightStyle ?: DefaultObservableJoystickStyle.lightStyle
    }

    //已经经过验证，如果使用Modifier.alpha设置不透明度，会导致摇杆强制裁切超出范围的内容
    //graphicsLayer(alpha = alpha, clip = false)也一样
    //这里暂时只能统一修改颜色的alpha
    val alpha = themeConfig.alpha
    val currentBackgroundColor = remember(themeConfig.backgroundColor, alpha) {
        themeConfig.backgroundColor.applyAlpha(alpha)
    }
    val currentJoystickColor = remember(themeConfig.joystickColor, alpha) {
        themeConfig.joystickColor.applyAlpha(alpha)
    }
    val currentJoystickCanLockColor = remember(themeConfig.joystickCanLockColor, alpha) {
        themeConfig.joystickCanLockColor.applyAlpha(alpha)
    }
    val currentJoystickLockedColor = remember(themeConfig.joystickLockedColor, alpha) {
        themeConfig.joystickLockedColor.applyAlpha(alpha)
    }
    val currentLockMarkColor = remember(themeConfig.lockMarkColor, alpha) {
        themeConfig.lockMarkColor.applyAlpha(alpha)
    }
    val currentBorderColor = remember(themeConfig.borderColor, alpha) {
        themeConfig.borderColor.applyAlpha(alpha)
    }

    // 形状
    val backgroundShape = remember(themeConfig.backgroundShape) {
        if (themeConfig.backgroundShape == 50) CircleShape
        else RoundedCornerShape(percent = themeConfig.backgroundShape)
    }
    val joystickShape = remember(themeConfig.joystickShape) {
        if (themeConfig.joystickShape == 50) CircleShape
        else RoundedCornerShape(percent = themeConfig.joystickShape)
    }

    // 边框宽度比例
    val borderWidthRatio = remember(themeConfig.borderWidthRatio) {
        (themeConfig.borderWidthRatio.toFloat() / 100f).coerceIn(0.0f, 0.5f)
    }

    // 摇杆头大小
    val joystickSizeRatio = remember(themeConfig.joystickSize) {
        themeConfig.joystickSize.coerceIn(0.1f, 1.0f)
    }

    //使用这个标记来判断是否渲染摇杆组件，未完全初始化时，可能导致组件闪烁
    var initialized by remember { mutableStateOf(false) }

    // 当大小变化时重新初始化
    var currentSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(currentSize) {
        initialized = false
        if (currentSize != IntSize.Zero) {
            // 计算并设置背景区域
            val sizePx = Size(currentSize.width.toFloat(), currentSize.height.toFloat())
            data.backgroundRegion = backgroundShape.toRegion(
                size = sizePx,
                density = density,
                layoutDirection = layoutDirection
            )
            data.knobOffset = Offset.Zero
            initialized = true
        }
    }

    // 当形状变化时重新计算区域
    LaunchedEffect(backgroundShape) {
        if (currentSize != IntSize.Zero) {
            val sizePx = Size(currentSize.width.toFloat(), currentSize.height.toFloat())
            data.backgroundRegion = backgroundShape.toRegion(
                size = sizePx,
                density = density,
                layoutDirection = layoutDirection
            )
        }
    }

    if (visible) {
        Box(
            modifier = Modifier
                .onSizeChanged { currentSize = it }
                .buttonSize(data, screenSize)
                .let { modifier ->
                    if (isEditMode) {
                        modifier.editMode(
                            isEditMode = true,
                            data = data,
                            screenSize = screenSize,
                            enableSnap = enableSnap,
                            snapMode = snapMode,
                            localSnapRange = localSnapRange,
                            getOtherWidgets = getOtherWidgets,
                            snapThresholdValue = snapThresholdValue,
                            drawLine = drawLine,
                            onLineCancel = onLineCancel,
                            onTapInEditMode = onTapInEditMode ?: {}
                        )
                    } else if (pointerEventBus != null && eventHandler != null && reversedLayers != null) {
                        modifier.then(
                            data.touchModifier(
                                pointerEventBus = pointerEventBus,
                                eventHandler = eventHandler,
                                allLayers = reversedLayers,
                                screenSize = screenSize,
                                onOccupiedPointer = onOccupiedPointer,
                                onReleasePointer = onReleasePointer
                            )
                        )
                    } else modifier
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (initialized) {
                    val minSide = minOf(size.width, size.height)
                    val bgCenter = Offset(size.width / 2f, size.height / 2f)

                    // 背景层
                    drawBackgroundLayer(
                        layoutDirection = layoutDirection,
                        size = Size(size.width, size.height),
                        shape = backgroundShape,
                        backgroundColor = currentBackgroundColor,
                        borderColor = currentBorderColor,
                        borderWidthPx = (minSide * borderWidthRatio).coerceAtLeast(0f)
                    )

                    // 摇杆头
                    val knobSize = minSide * joystickSizeRatio
                    val knobCenter = Offset(
                        bgCenter.x + data.knobOffset.x,
                        bgCenter.y + data.knobOffset.y
                    )
                    drawJoystick(
                        layoutDirection = layoutDirection,
                        color = when {
                            data.isLocked -> currentJoystickLockedColor
                            data.canLockState -> currentJoystickCanLockColor
                            else -> currentJoystickColor
                        },
                        center = knobCenter,
                        size = knobSize,
                        shape = joystickShape
                    )

                    // 绘制锁定标记
                    if (data.isLocked) {
                        drawCircle(
                            color = currentLockMarkColor,
                            center = Offset(bgCenter.x, 0f),
                            radius = 4f
                        )
                    }
                }
            }

            DisposableEffect(Unit) {
                data.onCompositionStart(eventHandler)
                onDispose {
                    data.onCompositionDispose(eventHandler)
                }
            }
        }
    } else {
        Spacer(
            modifier = Modifier.buttonSize(data, screenSize)
        )
    }
}

/**
 * 绘制背景层
 */
private fun DrawScope.drawBackgroundLayer(
    layoutDirection: LayoutDirection,
    size: Size,
    shape: Shape,
    backgroundColor: Color,
    borderColor: Color,
    borderWidthPx: Float
) {
    val outline = shape.createOutline(
        size = size,
        layoutDirection = layoutDirection,
        density = this
    )

    val clipPath = when (outline) {
        is Outline.Generic -> outline.path
        is Outline.Rounded -> Path().apply {
            addRoundRect(outline.roundRect)
        }
        is Outline.Rectangle -> Path().apply {
            addRect(outline.rect)
        }
    }

    clipPath(clipPath) {
        drawOutline(
            outline = outline,
            color = backgroundColor
        )

        if (borderWidthPx > 0f) {
            drawOutline(
                outline = outline,
                color = borderColor,
                style = Stroke(width = borderWidthPx)
            )
        }
    }
}

/**
 * 绘制摇杆层
 */
private fun DrawScope.drawJoystick(
    layoutDirection: LayoutDirection,
    color: Color,
    center: Offset,
    size: Float,
    shape: Shape
) {
    val halfSize = size / 2
    val topLeftX = center.x - halfSize
    val topLeftY = center.y - halfSize

    val outline = shape.createOutline(
        size = Size(size, size),
        layoutDirection = layoutDirection,
        density = this
    )

    translate(
        left = topLeftX,
        top = topLeftY
    ) {
        drawOutline(
            outline = outline,
            color = color
        )
    }
}

private fun Color.applyAlpha(multiplier: Float): Color {
    return copy(alpha = this.alpha * multiplier)
}
