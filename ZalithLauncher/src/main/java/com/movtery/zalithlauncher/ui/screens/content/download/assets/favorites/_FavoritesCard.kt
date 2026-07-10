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

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.assets.favorites.FavoriteAsset
import com.movtery.zalithlauncher.game.download.assets.favorites.FavoriteManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.components.LittleTextLabel
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.AssetsIcon
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.ClassesIdentifier
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.PlatformIdentifier
import com.movtery.zalithlauncher.ui.screens.content.elements.backgroundGlass
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.movtery.zalithlauncher.ui.theme.onCardColor
import com.movtery.zalithlauncher.utils.animation.getAnimateTween

/**
 * 收藏列表中同一项目分组合并后的数据
 * @param project 项目收藏记录（若仅收藏了版本没收藏项目本身则为 null）
 * @param versions 该项目下已收藏的版本列表
 */
data class FavoriteGroup(
    val project: FavoriteAsset?,
    val versions: List<FavoriteAsset>
) {
    val platform get() = (project ?: versions.first()).platform
    val classes get() = (project ?: versions.first()).classes
    val projectId get() = (project ?: versions.first()).projectId
    val title get() = (project ?: versions.first()).title
    val author get() = (project ?: versions.first()).author
    val description get() = (project ?: versions.first()).description
    val iconUrl get() = (project ?: versions.first()).iconUrl
}

@Composable
fun FavoriteCard(
    modifier: Modifier = Modifier,
    group: FavoriteGroup,
    onClick: () -> Unit = {}
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }

    val favoriteAction = remember(group.platform, group.projectId) {
        FavoriteAction(
            isFavorite = true,
            onToggle = { FavoriteManager.removeAllByProject(group.platform, group.projectId) }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(scaleY = scale.value, scaleX = scale.value),
        shape = MaterialTheme.shapes.large,
        color = cardColor(true),
        contentColor = onCardColor(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .backgroundGlass(AllSettings.backgroundBlur.state, cardColor(true), true)
                .padding(all = 8.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssetsIcon(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(10.dp))
                    .align(Alignment.CenterVertically),
                size = 72.dp,
                iconUrl = group.iconUrl
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .weight(0.6f, fill = false)
                                .basicMarquee(iterations = Int.MAX_VALUE),
                            text = group.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1
                        )
                        group.author?.let { author ->
                            VerticalDivider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Text(
                                modifier = Modifier
                                    .weight(0.4f, fill = false)
                                    .alpha(0.7f),
                                text = stringResource(R.string.download_assets_result_authors, author),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                    FavoriteToggleButton(favoriteAction = favoriteAction)
                    PlatformIdentifier(platform = group.platform)
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = group.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Column(
                        modifier = Modifier.alpha(0.7f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (group.versions.isNotEmpty()) {
                            //展示已收藏的版本数
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    painter = painterResource(R.drawable.ic_package_2_outlined),
                                    contentDescription = null
                                )
                                Text(
                                    text = if (group.versions.size == 1) {
                                        group.versions.first().versionName ?: ""
                                    } else {
                                        stringResource(R.string.favorites_version_count, group.versions.size)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        } else if (group.project != null) {
                            //仅收藏了项目本身
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier.size(14.dp),
                                    painter = painterResource(R.drawable.ic_favorite_outlined),
                                    contentDescription = null
                                )
                                Text(
                                    text = stringResource(R.string.favorites_no_version),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    group.versions.joinToString(", ") { it.loadersCsv ?: "" }
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct().joinToString(", ").takeIf { it.isNotBlank() }?.let { loaders ->
                            Text(
                                text = loaders,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.alpha(0.7f)
                            )
                        }
                    Row(modifier = Modifier.weight(1f)) {}
                    ClassesIdentifier(classes = group.classes)
                }
            }
        }
    }
}