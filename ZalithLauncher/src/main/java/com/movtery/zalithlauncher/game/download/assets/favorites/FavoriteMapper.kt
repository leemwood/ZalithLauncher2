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

import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformFilterCode
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchData
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion

/**
 * 由 [PlatformVersion]（已 initFile）+ [PlatformProject] 构建一个完整的资源收藏项。
 * @param previousSavedAt 若为更新操作，传入原收藏项的 savedAt 以保留收藏时间；否则使用当前时间
 */
fun PlatformVersion.toFavorite(
    project: PlatformProject,
    classes: PlatformClasses,
    previousSavedAt: Long? = null
): FavoriteAsset = FavoriteAsset(
    id = 0,
    platform = project.platform(),
    classes = classes,
    projectId = project.platformId(),
    slug = project.platformSlug(),
    title = project.platformTitle(),
    author = project.platformAuthor(),
    description = project.platformSummary(),
    iconUrl = project.platformIconUrl(),
    categoriesCsv = project.platformCategories(classes)?.joinToString(",") { it.filterCodeName() },
    versionName = platformVersion(),
    versionFileName = platformFileName(),
    downloadUrl = platformDownloadUrl(),
    sha1 = platformSha1(),
    fileSize = platformFileSize(),
    gameVersionsCsv = platformGameVersion().joinToString(","),
    loadersCsv = platformLoaders().joinToString(",") { it.getDisplayName() },
    releaseType = platformReleaseType(),
    savedAt = previousSavedAt ?: System.currentTimeMillis()
)

/**
 * 由 [PlatformSearchData] 构建一个仅含项目元信息的资源收藏项（未指定版本）。
 * 调用方应在用户进入下载页选版后，再通过 [toFavorite] 回写完整版本链接。
 */
fun PlatformSearchData.toFavorite(
    classes: PlatformClasses,
    previousSavedAt: Long? = null
): FavoriteAsset = FavoriteAsset(
    id = 0,
    platform = platform(),
    classes = classes,
    projectId = platformId(),
    slug = null,
    title = platformTitle(),
    author = platformAuthor(),
    description = platformDescription(),
    iconUrl = platformIconUrl(),
    categoriesCsv = platformCategories(classes)?.joinToString(",") { it.filterCodeName() },
    versionName = null,
    versionFileName = null,
    downloadUrl = "",
    sha1 = null,
    fileSize = 0L,
    gameVersionsCsv = null,
    loadersCsv = platformModLoaders()?.joinToString(",") { it.getDisplayName() },
    releaseType = null,
    savedAt = previousSavedAt ?: System.currentTimeMillis()
)

private fun PlatformFilterCode.filterCodeName(): String =
    this::class.simpleName ?: this.toString()
