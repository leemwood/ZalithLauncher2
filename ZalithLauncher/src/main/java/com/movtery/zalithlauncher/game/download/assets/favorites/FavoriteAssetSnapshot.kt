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
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import java.time.Instant

/**
 * 由 [FavoriteAsset] 构建的 [PlatformVersion] 快照实现，供收藏列表"直接下载"复用现有对话框。
 *
 * 仅还原下载所需的关键字段（文件名、直链、sha1、大小、游戏版本、加载器等），
 * 不还原依赖项（收藏项不缓存依赖，依赖为空列表）。
 */
private class FavoriteAssetSnapshot(
    private val asset: FavoriteAsset
) : PlatformVersion {
    override suspend fun initFile(currentProjectId: String): Boolean = true

    override fun platform(): Platform = asset.platform

    override fun platformId(): String = asset.projectId

    override fun platformDisplayName(): String = asset.versionName ?: asset.title

    override fun platformFileName(): String = asset.versionFileName ?: asset.title

    override fun platformGameVersion(): Array<String> =
        asset.gameVersionsCsv?.split(",")?.filter { it.isNotBlank() }?.toTypedArray() ?: emptyArray()

    override fun platformLoaders(): List<PlatformDisplayLabel> =
        asset.loadersCsv?.split(",")?.filter { it.isNotBlank() }?.map { SnapshotLabel(it.trim()) } ?: emptyList()

    override fun platformReleaseType(): PlatformReleaseType =
        asset.releaseType ?: PlatformReleaseType.RELEASE

    override fun platformDependencies(): List<PlatformVersion.PlatformDependency> = emptyList()

    override fun platformDownloadCount(): Long = 0L

    override fun platformDownloadUrl(): String = asset.downloadUrl

    override fun platformDatePublished(): Instant = Instant.ofEpochMilli(asset.savedAt)

    override fun platformSha1(): String? = asset.sha1

    override fun platformFileSize(): Long = asset.fileSize

    override fun platformVersion(): String = asset.versionName ?: ""
}

/**
 * 仅展示用的加载器标签，直接使用 displayName 字符串
 */
private class SnapshotLabel(private val displayName: String) : PlatformDisplayLabel {
    override fun getDisplayName(): String = displayName
    override fun index(): Int = 0
}

/**
 * 收藏项是否是一个已绑定具体可下载版本的版本收藏
 */
val FavoriteAsset.hasFixedVersion: Boolean
    get() = type == FavoriteType.VERSION && !versionName.isNullOrBlank() && downloadUrl.isNotBlank() && !versionFileName.isNullOrBlank()

/**
 * 将收藏项转为 [PlatformVersion] 快照，用于直接复用下载对话框与下载任务
 */
fun FavoriteAsset.toPlatformVersion(): PlatformVersion = FavoriteAssetSnapshot(this)