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
 *
 * 收藏分两类：
 * - 项目收藏（[FavoriteType.PROJECT]）：同一 (platform, projectId) 最多一条
 * - 版本收藏（[FavoriteType.VERSION]）：同一 (platform, projectId, downloadUrl) 唯一，可多条
 *
 * 数据通过 MMKV 持久化，每条收藏项以 (platform, projectId, type, downloadUrl) 为键。
 */
object FavoriteManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()

    private val _assets = MutableStateFlow<List<FavoriteAsset>>(emptyList())
    val assets = _assets.asStateFlow()

    fun initialize() {
        reload()
    }

    fun reload() {
        scope.launch {
            mutex.withLock {
                runCatching { loadAll() }
                    .onSuccess { list -> _assets.update { list } }
                    .onFailure { e -> Logger.error(TAG, "Failed to load favorites", e) }
            }
        }
    }

    /** 是否已收藏项目本身 */
    fun isProjectFavorite(platform: Platform, projectId: String): Boolean =
        _assets.value.any { it.platform == platform && it.projectId == projectId && it.type == FavoriteType.PROJECT }

    /** 是否已收藏指定版本 */
    fun isVersionFavorite(platform: Platform, projectId: String, downloadUrl: String): Boolean =
        _assets.value.any {
            it.platform == platform && it.projectId == projectId &&
                it.type == FavoriteType.VERSION && it.downloadUrl == downloadUrl
        }

    /** 取项目收藏记录 */
    fun findProject(platform: Platform, projectId: String): FavoriteAsset? =
        _assets.value.firstOrNull { it.platform == platform && it.projectId == projectId && it.type == FavoriteType.PROJECT }

    /** 取某项目下所有版本收藏记录 */
    fun findVersions(platform: Platform, projectId: String): List<FavoriteAsset> =
        _assets.value.filter {
            it.platform == platform && it.projectId == projectId && it.type == FavoriteType.VERSION
        }

    fun getByClasses(classes: PlatformClasses): List<FavoriteAsset> =
        _assets.value.filter { it.classes == classes }

    fun save(asset: FavoriteAsset) {
        scope.launch {
            runCatching {
                favoritesMMKV().encode(asset.key(), asset)
            }.onFailure { e -> Logger.error(TAG, "Failed to save favorite", e) }
            reload()
        }
    }

    fun removeProject(platform: Platform, projectId: String) {
        scope.launch {
            runCatching {
                val mmkv = favoritesMMKV()
                val key = favoriteKey(platform, projectId, FavoriteType.PROJECT, "")
                mmkv.removeValueForKey(key)
            }.onFailure { e -> Logger.error(TAG, "Failed to remove project favorite", e) }
            reload()
        }
    }

    fun removeVersion(platform: Platform, projectId: String, downloadUrl: String) {
        scope.launch {
            runCatching {
                val mmkv = favoritesMMKV()
                val key = favoriteKey(platform, projectId, FavoriteType.VERSION, downloadUrl)
                mmkv.removeValueForKey(key)
            }.onFailure { e -> Logger.error(TAG, "Failed to remove version favorite", e) }
            reload()
        }
    }

    /** 删除某项目下的全部收藏（项目收藏 + 所有版本收藏） */
    fun removeAllByProject(platform: Platform, projectId: String) {
        scope.launch {
            runCatching {
                val mmkv = favoritesMMKV()
                val keys = _assets.value
                    .filter { it.platform == platform && it.projectId == projectId }
                    .map { it.key() }
                keys.forEach { mmkv.removeValueForKey(it) }
            }.onFailure { e -> Logger.error(TAG, "Failed to remove all favorites by project", e) }
            reload()
        }
    }

    /** 切换项目收藏 */
    fun toggleProject(asset: FavoriteAsset) {
        require(asset.type == FavoriteType.PROJECT)
        if (isProjectFavorite(asset.platform, asset.projectId)) {
            removeProject(asset.platform, asset.projectId)
        } else {
            save(asset)
        }
    }

    /** 切换版本收藏 */
    fun toggleVersion(asset: FavoriteAsset) {
        require(asset.type == FavoriteType.VERSION)
        if (isVersionFavorite(asset.platform, asset.projectId, asset.downloadUrl)) {
            removeVersion(asset.platform, asset.projectId, asset.downloadUrl)
        } else {
            save(asset)
        }
    }

    private fun loadAll(): List<FavoriteAsset> {
        val mmkv = favoritesMMKV()
        val keys = mmkv.allKeys() ?: return emptyList()
        return keys.mapNotNull { key ->
            runCatching { mmkv.decodeParcelable(key, FavoriteAsset::class.java) }
                .getOrNull()
        }.sortedByDescending { it.savedAt }
    }
}