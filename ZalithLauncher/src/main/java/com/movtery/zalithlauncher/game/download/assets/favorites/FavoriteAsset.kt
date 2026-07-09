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

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import kotlinx.parcelize.Parcelize

/**
 * 资源收藏项，按平台项目去重，并保存用户选中的某个版本的下载链接快照
 */
@Keep
@Parcelize
@Entity(
    tableName = "favoriteAssets",
    indices = [Index(value = ["platform", "projectId"], unique = true)]
)
data class FavoriteAsset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val platform: Platform,
    val classes: PlatformClasses,
    val projectId: String,
    val slug: String?,
    val title: String,
    val author: String?,
    val description: String?,
    val iconUrl: String?,
    val categoriesCsv: String?,
    val versionName: String?,
    val versionFileName: String?,
    val downloadUrl: String,
    val sha1: String?,
    val fileSize: Long,
    val gameVersionsCsv: String?,
    val loadersCsv: String?,
    val releaseType: PlatformReleaseType?,
    val savedAt: Long
) : Parcelable
