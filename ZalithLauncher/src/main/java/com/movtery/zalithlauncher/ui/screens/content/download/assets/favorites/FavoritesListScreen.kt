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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.assets.favorites.FavoriteManager
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.TitledNavKey
import com.movtery.zalithlauncher.utils.string.isEmptyOrBlank

private data class FavoritesTabItem(
    val label: Int,
    val icon: Int,
    val classes: PlatformClasses?
)

private val FAVORITES_TABS = listOf(
    FavoritesTabItem(R.string.favorites_tab_all, R.drawable.ic_favorite_outlined, null),
    FavoritesTabItem(R.string.download_category_modpack, R.drawable.ic_package_2_outlined, PlatformClasses.MOD_PACK),
    FavoritesTabItem(R.string.download_category_mod, R.drawable.ic_extension_outlined, PlatformClasses.MOD),
    FavoritesTabItem(R.string.download_category_resource_pack, R.drawable.ic_format_paint_outlined, PlatformClasses.RESOURCE_PACK),
    FavoritesTabItem(R.string.download_category_saves, R.drawable.ic_public, PlatformClasses.SAVES),
    FavoritesTabItem(R.string.download_category_shaders, R.drawable.ic_lightbulb, PlatformClasses.SHADERS)
)

@Composable
fun FavoritesListScreen(
    mainScreenKey: TitledNavKey?,
    downloadScreenKey: TitledNavKey?,
    downloadFavoritesScreenKey: TitledNavKey,
    downloadFavoritesScreenCurrentKey: TitledNavKey?,
    swapToDownload: (platform: Platform, projectId: String, classes: PlatformClasses, iconUrl: String?) -> Unit = { _, _, _, _ -> }
) {
    val favorites by FavoriteManager.assets.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredFavorites = remember(favorites, selectedTab, searchQuery) {
        val tab = FAVORITES_TABS[selectedTab]
        val byClasses = if (tab.classes == null) favorites else favorites.filter { it.classes == tab.classes }
        if (searchQuery.isEmptyOrBlank()) byClasses
        else byClasses.filter { asset ->
            asset.title.contains(searchQuery, true) ||
                    asset.slug?.contains(searchQuery, true) == true ||
                    asset.author?.contains(searchQuery, true) == true ||
                    asset.description?.contains(searchQuery, true) == true ||
                    asset.versionName?.contains(searchQuery, true) == true
        }
    }

    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.Download::class.java, mainScreenKey)
        ),
        Triple(downloadFavoritesScreenKey, downloadScreenKey, false),
        Triple(NormalNavKey.FavoritesList, downloadFavoritesScreenCurrentKey, false)
    ) { _ ->
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SimpleTextInputField(
                    modifier = Modifier.weight(1f),
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    hint = {
                        Text(
                            text = stringResource(R.string.favorites_search_hint),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpacerFixed(width = 4.dp)
                FAVORITES_TABS.forEachIndexed { index, tab ->
                    FilterChip(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = {
                            Text(
                                text = stringResource(tab.label),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(tab.icon),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    )
                }
                SpacerFixed(width = 4.dp)
            }

            if (filteredFavorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(all = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            painter = painterResource(R.drawable.ic_favorite_outlined),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(R.string.favorites_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredFavorites) { asset ->
                        FavoriteCard(
                            modifier = Modifier.fillMaxWidth(),
                            asset = asset,
                            onClick = {
                                swapToDownload(
                                    asset.platform,
                                    asset.projectId,
                                    asset.classes,
                                    asset.iconUrl
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpacerFixed(width: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(width))
}
