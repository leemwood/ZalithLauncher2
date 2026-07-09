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

package com.movtery.zalithlauncher.ui.screens.content.download.assets.favorites

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R

/**
 * 收藏操作回调，封装收藏态与切换逻辑
 */
class FavoriteAction(
    val isFavorite: Boolean,
    val onToggle: () -> Unit
)

/**
 * 可复用的收藏切换按钮，用于搜索结果卡、下载详情页版本行、收藏列表卡片
 */
@Composable
fun FavoriteToggleButton(
    modifier: Modifier = Modifier,
    favoriteAction: FavoriteAction
) {
    IconButton(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape),
        onClick = favoriteAction.onToggle
    ) {
        Icon(
            painter = painterResource(
                if (favoriteAction.isFavorite) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_outlined
            ),
            contentDescription = stringResource(
                if (favoriteAction.isFavorite) R.string.favorites_remove
                else R.string.favorites_add
            ),
            tint = if (favoriteAction.isFavorite) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
