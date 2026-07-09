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

package com.movtery.zalithlauncher.game.download.assets.favorites

import android.content.Context
import com.movtery.zalithlauncher.database.AppDatabase
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "FavoriteManager"

/**
 * 资源收藏管理器，维护 [FavoriteAsset] 列表的 StateFlow，并对外暴露收藏/取消收藏/查询能力
 */
object FavoriteManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private val _assets = MutableStateFlow<List<FavoriteAsset>>(emptyList())
    val assets = _assets.asStateFlow()

    private lateinit var favoriteAssetDao: FavoriteAssetDao

    fun initialize(context: Context) {
        favoriteAssetDao = AppDatabase.getInstance(context).favoriteAssetDao()
    }

    fun reload() {
        scope.launch {
            mutex.withLock {
                runCatching { favoriteAssetDao.getAll() }
                    .onSuccess { list -> _assets.update { list } }
                    .onFailure { e -> Logger.error(TAG, "Failed to load favorites", e) }
            }
        }
    }

    fun isFavorite(platform: Platform, projectId: String): Boolean =
        _assets.value.any { it.platform == platform && it.projectId == projectId }

    fun find(platform: Platform, projectId: String): FavoriteAsset? =
        _assets.value.firstOrNull { it.platform == platform && it.projectId == projectId }

    fun getByClasses(classes: PlatformClasses): List<FavoriteAsset> =
        _assets.value.filter { it.classes == classes }

    fun save(asset: FavoriteAsset) {
        scope.launch {
            runCatching { favoriteAssetDao.save(asset) }
                .onFailure { e -> Logger.error(TAG, "Failed to save favorite", e) }
            reload()
        }
    }

    fun remove(platform: Platform, projectId: String) {
        scope.launch {
            runCatching { favoriteAssetDao.delete(platform, projectId) }
                .onFailure { e -> Logger.error(TAG, "Failed to remove favorite", e) }
            reload()
        }
    }

    fun toggle(asset: FavoriteAsset) {
        if (isFavorite(asset.platform, asset.projectId)) {
            remove(asset.platform, asset.projectId)
        } else {
            save(asset)
        }
    }
}
