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

package com.movtery.zalithlauncher.filemanager.config

import com.tencent.mmkv.MMKV

private const val KEY_SHOW_HIDDEN = "show_hidden"
private const val KEY_SORT_FIELD = "sort_field"
private const val KEY_SORT_ASC = "sort_ascending"
private const val KEY_FOLDER_FIRST = "folder_first"
private const val KEY_TRASH_SORT_FIELD = "trash_sort_field"
private const val KEY_TRASH_SORT_ASC = "trash_sort_ascending"
private const val KEY_TRASH_FOLDER_FIRST = "trash_folder_first"

/** 文件管理器配置存储 */
object FmConfig {
    private const val MMKV_ID = "zalith_file_manager"

    private fun mmkv(): MMKV = MMKV.mmkvWithID(MMKV_ID, MMKV.SINGLE_PROCESS_MODE)

    /** 设置是否显示隐藏文件 */
    fun setShowHidden(value: Boolean) {
        mmkv().putBoolean(KEY_SHOW_HIDDEN, value)
    }

    /** 是否显示隐藏文件 */
    fun showHidden(): Boolean = mmkv().decodeBool(KEY_SHOW_HIDDEN, true)

    /** 设置主列表的排序字段 */
    fun setSortField(value: String) {
        mmkv().putString(KEY_SORT_FIELD, value)
    }

    /** 主列表排序字段 */
    fun sortField(): String = mmkv().decodeString(KEY_SORT_FIELD) ?: SortField.NAME.name

    /** 设置主列表是否升序排序 */
    fun setSortAscending(value: Boolean) {
        mmkv().putBoolean(KEY_SORT_ASC, value)
    }

    /** 主列表是否升序排序 */
    fun sortAscending(): Boolean = mmkv().decodeBool(KEY_SORT_ASC, true)

    /** 设置主列表是否目录优先 */
    fun setFolderFirst(value: Boolean) {
        mmkv().putBoolean(KEY_FOLDER_FIRST, value)
    }

    /** 主列表是否目录优先 */
    fun folderFirst(): Boolean = mmkv().decodeBool(KEY_FOLDER_FIRST, true)

    /** 设置回收站列表的排序字段 */
    fun setTrashSortField(value: String) {
        mmkv().putString(KEY_TRASH_SORT_FIELD, value)
    }

    /** 回收站列表排序字段 */
    fun trashSortField(): String = mmkv().decodeString(KEY_TRASH_SORT_FIELD) ?: TrashSortField.DELETED.name

    /** 设置回收站列表是否升序排序 */
    fun setTrashSortAscending(value: Boolean) {
        mmkv().putBoolean(KEY_TRASH_SORT_ASC, value)
    }

    /** 回收站列表是否升序排序 */
    fun trashSortAscending(): Boolean = mmkv().decodeBool(KEY_TRASH_SORT_ASC, false)

    /** 设置回收站列表是否目录优先 */
    fun setTrashFolderFirst(value: Boolean) {
        mmkv().putBoolean(KEY_TRASH_FOLDER_FIRST, value)
    }

    /** 回收站列表是否目录优先 */
    fun trashFolderFirst(): Boolean = mmkv().decodeBool(KEY_TRASH_FOLDER_FIRST, true)

    enum class SortField { NAME, SIZE, MODIFIED }

    enum class TrashSortField { NAME, DELETED }
}