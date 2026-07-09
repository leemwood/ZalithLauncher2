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

package com.movtery.zalithlauncher.database

import androidx.room.TypeConverter
import com.movtery.zalithlauncher.game.account.wardrobe.SkinModelType
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType

class Converters {
    @TypeConverter
    fun fromSkinModelType(type: SkinModelType): String = type.name

    @TypeConverter
    fun toSkinModelType(value: String): SkinModelType =
        enumValueOf(value)

    @TypeConverter
    fun fromPlatform(value: Platform): String = value.name

    @TypeConverter
    fun toPlatform(value: String): Platform = enumValueOf(value)

    @TypeConverter
    fun fromPlatformClasses(value: PlatformClasses): String = value.name

    @TypeConverter
    fun toPlatformClasses(value: String): PlatformClasses = enumValueOf(value)

    @TypeConverter
    fun fromPlatformReleaseType(value: PlatformReleaseType?): String? = value?.name

    @TypeConverter
    fun toPlatformReleaseType(value: String?): PlatformReleaseType? =
        value?.let { enumValueOf<PlatformReleaseType>(it) }
}