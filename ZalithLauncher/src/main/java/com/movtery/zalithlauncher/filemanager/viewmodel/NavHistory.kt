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

package com.movtery.zalithlauncher.filemanager.viewmodel

import java.nio.file.Path

/** 当前目录的后退/前进历史 */
class NavHistory(initialCurrent: Path) {

    private val backStack = ArrayDeque<Path>()
    private val forwardStack = ArrayDeque<Path>()
    private var current: Path = initialCurrent

    val canBack: Boolean get() = backStack.isNotEmpty()
    val canForward: Boolean get() = forwardStack.isNotEmpty()
    val currentPath: Path get() = current

    /** 进入一个新目录，清空前进栈 */
    fun navigate(to: Path) {
        if (to == current) return
        backStack.addLast(current)
        forwardStack.clear()
        current = to
    }

    /**
     * 后退
     * @return 上一个目录，或 null 表示无历史
     */
    fun back(): Path? {
        if (backStack.isEmpty()) return null
        val prev = backStack.removeLast()
        forwardStack.addLast(current)
        current = prev
        return prev
    }

    /**
     * 前进
     * @return 撤销上一次后退，或 null 表示无可前进
     */
    fun forward(): Path? {
        if (forwardStack.isEmpty()) return null
        val next = forwardStack.removeLast()
        backStack.addLast(current)
        current = next
        return next
    }

    /**
     * 目录被删除后清理历史：移除位于被删目录内（含自身）的历史条目，
     * 并截断后退栈中该条目及其以前（更早）的历史，避免导航进入已删除的目录。
     */
    fun pruneDeleted(deleted: Path) {
        fun inside(p: Path): Boolean = p == deleted || p.startsWith(deleted)

        val back = backStack.toList()
        val firstInvalid = back.indexOfFirst { inside(it) }
        if (firstInvalid >= 0) {
            backStack.clear()
            back.drop(firstInvalid + 1).filterNot { inside(it) }.forEach { backStack.addLast(it) }
        }

        if (forwardStack.any { inside(it) }) {
            forwardStack.clear()
        }
    }
}