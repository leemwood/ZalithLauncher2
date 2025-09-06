package com.movtery.zalithlauncher.ui.base

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.navigation3.runtime.NavKey
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.setting.AllSettings

/**
 * 带有自定义背景的基础屏幕组件
 * @param screenKey 当前屏幕的Key
 * @param currentKey 当前屏幕正在展示的Key
 * @param useClassEquality 是否使用类相等判断
 */
@Composable
fun BaseScreenWithBackground(
    screenKey: NavKey,
    currentKey: NavKey?,
    useClassEquality: Boolean = false,
    content: @Composable (isVisible: Boolean) -> Unit,
) {
    val targetVisible = isTagVisible(screenKey, currentKey, useClassEquality)
    
    //初始不可见，用于触发首次的 false -> true 动画
    val visibleState = remember { mutableStateOf(false) }
    
    //仅在 composition 完成后，才允许更新可见状态
    LaunchedEffect(targetVisible) {
        visibleState.value = targetVisible
    }
    
    BaseScreenWithBackground(
        content = content,
        visible = visibleState.value
    )
}

/**
 * 带有自定义背景的多层级基础屏幕组件
 */
@Composable
fun BaseScreenWithBackground(
    vararg levels: Triple<NavKey, NavKey?, Boolean>,
    content: @Composable (isVisible: Boolean) -> Unit,
) {
    val targetVisible = remember(levels) {
        levels.all { (tag, currentKey, useReferenceEquality) ->
            isTagVisible(tag, currentKey, useReferenceEquality)
        }
    }

    //初始不可见，用于触发首次的 false -> true 动画
    val visibleState = remember { mutableStateOf(false) }
    
    //仅在 composition 完成后，才允许更新可见状态
    LaunchedEffect(targetVisible) {
        visibleState.value = targetVisible
    }

    BaseScreenWithBackground(
        content = content,
        visible = visibleState.value
    )
}

@Composable
private fun BaseScreenWithBackground(
    content: @Composable (isVisible: Boolean) -> Unit,
    visible: Boolean
) {
    val backgroundImage = AllSettings.launcherBackgroundImage.getValue()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 显示背景图片（如果已设置）
        if (backgroundImage.isNotEmpty()) {
            AsyncImage(
                model = backgroundImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.3f // 降低背景图片的透明度，以确保内容可读性
            )
            
            // 添加半透明遮罩以提高内容可读性
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color.Black.copy(alpha = 0.5f))
            }
        }
        
        // 内容显示在背景之上
        BaseScreen(
            content = content,
            visible = visible
        )
    }
}