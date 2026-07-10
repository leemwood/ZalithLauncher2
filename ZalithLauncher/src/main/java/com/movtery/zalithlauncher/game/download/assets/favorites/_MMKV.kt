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
import com.tencent.mmkv.MMKV

/**
 * 资源收藏 MMKV，多进程模式
 */
fun favoritesMMKV(): MMKV = MMKV.mmkvWithID("FavoriteAssets", MMKV.MULTI_PROCESS_MODE)

/**
 * 构建收藏项的唯一键，按 (platform, projectId, type, downloadUrl) 组合
 */
private const val KEY_SEPARATOR = "|"

fun favoriteKey(
    platform: Platform,
    projectId: String,
    type: FavoriteType,
    downloadUrl: String
): String = "${platform.name}$KEY_SEPARATOR$projectId$KEY_SEPARATOR${type.name}$KEY_SEPARATOR$downloadUrl"

fun FavoriteAsset.key(): String =
    favoriteKey(platform = platform, projectId = projectId, type = type, downloadUrl = downloadUrl)