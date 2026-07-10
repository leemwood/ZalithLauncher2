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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses

@Dao
interface FavoriteAssetDao {
    @Query("SELECT * FROM favoriteAssets ORDER BY savedAt DESC")
    suspend fun getAll(): List<FavoriteAsset>

    @Query("SELECT * FROM favoriteAssets WHERE classes = :classes ORDER BY savedAt DESC")
    suspend fun getByClasses(classes: PlatformClasses): List<FavoriteAsset>

    @Query("SELECT * FROM favoriteAssets WHERE platform = :platform AND projectId = :projectId AND type = 'PROJECT' LIMIT 1")
    suspend fun getProjectFavorite(platform: Platform, projectId: String): FavoriteAsset?

    @Query("SELECT * FROM favoriteAssets WHERE platform = :platform AND projectId = :projectId AND type = 'VERSION' ORDER BY savedAt DESC")
    suspend fun getVersionFavorites(platform: Platform, projectId: String): List<FavoriteAsset>

    @Query("SELECT * FROM favoriteAssets WHERE platform = :platform AND projectId = :projectId AND type = 'VERSION' AND downloadUrl = :downloadUrl LIMIT 1")
    suspend fun getVersionFavorite(platform: Platform, projectId: String, downloadUrl: String): FavoriteAsset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(asset: FavoriteAsset): Long

    @Query("DELETE FROM favoriteAssets WHERE platform = :platform AND projectId = :projectId AND type = 'PROJECT'")
    suspend fun deleteProject(platform: Platform, projectId: String): Int

    @Query("DELETE FROM favoriteAssets WHERE platform = :platform AND projectId = :projectId AND type = 'VERSION' AND downloadUrl = :downloadUrl")
    suspend fun deleteVersion(platform: Platform, projectId: String, downloadUrl: String): Int

    @Query("DELETE FROM favoriteAssets WHERE platform = :platform AND projectId = :projectId")
    suspend fun deleteAllByProject(platform: Platform, projectId: String): Int
}