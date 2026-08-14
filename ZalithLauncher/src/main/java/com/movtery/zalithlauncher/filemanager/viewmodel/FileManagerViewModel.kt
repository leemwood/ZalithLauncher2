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

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movtery.zalithlauncher.filemanager.config.FmConfig
import com.movtery.zalithlauncher.filemanager.logic.AccessScope
import com.movtery.zalithlauncher.filemanager.logic.FileManagerLogic
import com.movtery.zalithlauncher.filemanager.logic.compress.CompressOptions
import com.movtery.zalithlauncher.filemanager.logic.entry.FmEntry
import com.movtery.zalithlauncher.filemanager.logic.ops.ConflictResolution
import com.movtery.zalithlauncher.filemanager.logic.task.TaskManager
import com.movtery.zalithlauncher.filemanager.logic.task.TaskState
import com.movtery.zalithlauncher.filemanager.logic.trash.TrashItem
import com.movtery.zalithlauncher.filemanager.viewmodel.controllers.BrowseController
import com.movtery.zalithlauncher.filemanager.viewmodel.controllers.CompressController
import com.movtery.zalithlauncher.filemanager.viewmodel.controllers.DirectoryScanController
import com.movtery.zalithlauncher.filemanager.viewmodel.controllers.EntryController
import com.movtery.zalithlauncher.filemanager.viewmodel.controllers.ExtractController
import com.movtery.zalithlauncher.filemanager.viewmodel.controllers.ImportController
import com.movtery.zalithlauncher.filemanager.viewmodel.controllers.PasteController
import com.movtery.zalithlauncher.filemanager.viewmodel.controllers.SearchController
import com.movtery.zalithlauncher.filemanager.viewmodel.controllers.SelectionController
import com.movtery.zalithlauncher.filemanager.viewmodel.controllers.TrashController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject

/**
 * 文件管理器数据控制层门面：负责装配各功能控制器并转发 UI 调用。
 * 具体业务逻辑与协程启动由各控制器自行管理。
 */
@HiltViewModel
class FileManagerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 可访问范围目录的绝对路径。 */
    private val rootPathStr: String = savedStateHandle[KEY_ROOT_PATH]
        ?: throw IllegalStateException("Missing required argument: $KEY_ROOT_PATH")
    /** 可选的初始当前目录；无效时回退根目录。 */
    private val currentPathStr: String? = savedStateHandle[KEY_CURRENT_PATH]

    private val taskManager = TaskManager()

    private val scope: AccessScope = AccessScope(Paths.get(rootPathStr).normalize().toAbsolutePath())
    private val logic: FileManagerLogic = FileManagerLogic(
        scope = scope,
        trashRoot = context.cacheDir.toPath().resolve(TRASH_SUBDIR).normalize().toAbsolutePath(),
        cacheRoot = context.cacheDir.toPath().normalize().toAbsolutePath(),
        taskManager = taskManager
    )

    private val store = FmStateStore(context)

    private val browseCtr = BrowseController(logic, scope, store, viewModelScope)
    private val selectionCtr = SelectionController(store)
    private val pasteCtr = PasteController(logic, store, browseCtr, viewModelScope)
    private val compressCtr = CompressController(context, logic, store, browseCtr, viewModelScope)
    private val extractCtr = ExtractController(context, logic, scope, store, browseCtr, viewModelScope)
    private val importCtr = ImportController(context, logic, taskManager, store, pasteCtr, browseCtr, viewModelScope)
    private val searchCtr = SearchController(logic, taskManager, store, viewModelScope)
    private val directoryScanCtr = DirectoryScanController(store, viewModelScope)
    private val entryCtr = EntryController(context, logic, store, browseCtr, viewModelScope)
    private val trashCtr = TrashController(logic, store, browseCtr, viewModelScope)

    val state: StateFlow<FileManagerUiState> get() = store.state
    val searchUi: StateFlow<SearchUiState> get() = store.searchUi
    val dirScan: StateFlow<DirScanUiState?> get() = store.dirScan
    val errorEvents: SharedFlow<String> get() = store.errorEvents

    init {
        store.history = NavHistory(logic.resolveInitialCurrent(currentPathStr?.let { Paths.get(it) }))
        store.updateState {
            it.copy(
                currentDir = store.history.currentPath,
                sortConfig = SortConfig.load().also { c -> c.persist() },
                showHidden = FmConfig.showHidden(),
                trashSortConfig = TrashSortConfig.load().also { c -> c.persist() },
                taskState = TaskState.Idle,
                trashView = TrashViewState.Idle,
                canNavigateBack = store.history.canBack,
                canNavigateForward = store.history.canForward
            )
        }
        browseCtr.refreshDir(store.history.currentPath)
    }

    // ---------------- 浏览 / 导航 ----------------

    fun refresh() {
        browseCtr.refreshDir()
    }
    fun navigateTo(path: Path) = browseCtr.navigateTo(path)
    fun enterDirectory(entry: FmEntry) = browseCtr.enterDirectory(entry)
    fun back(): Boolean = browseCtr.back()
    fun forward(): Boolean = browseCtr.forward()
    fun goParent(): Boolean = browseCtr.goParent()
    fun submitJump(targetInput: String): Boolean = browseCtr.submitJump(targetInput)
    fun navigateToSearchHit(hit: SearchHitView) = browseCtr.navigateToSearchHit(hit.path)
    fun setSortConfig(config: SortConfig) = browseCtr.setSortConfig(config)
    fun toggleHidden() = browseCtr.toggleHidden()

    // ---------------- 选择 / 多选 ----------------
    fun toggleSelection(entry: FmEntry) = selectionCtr.toggleSelection(entry)
    fun swipeRangeSelect(entry: FmEntry) = selectionCtr.swipeRangeSelect(entry)
    fun selectAll() = selectionCtr.selectAll()
    fun clearSelection() = selectionCtr.clearSelection()

    /**
     * 系统返回键处理
     * 多选模式下先退出多选，否则后退
     * @return 位于根目录时返回 false
     */
    fun consumeBack(): Boolean {
        if (store.stateValue().multiSelect) {
            store.clearSelectionAndExitMulti()
            return true
        }
        // 系统返回键视为返回上一层目录，而非后退到上一个访问的目录
        if (browseCtr.goParent()) return true
        // 根目录
        return false
    }

    // ---------------- 目录属性扫描 ----------------

    fun startDirectoryScan(path: Path) = directoryScanCtr.startDirectoryScan(path)
    fun stopDirectoryScan() = directoryScanCtr.stopDirectoryScan()

    // ---------------- 搜索 ----------------

    fun showSearchDialog() = searchCtr.showSearchDialog()
    fun submitSearch(keyword: String, caseSensitive: Boolean) = searchCtr.submitSearch(keyword, caseSensitive)
    fun clearSearch() = searchCtr.clearSearch()
    fun backToSearchSetup() = searchCtr.backToSearchSetup()

    // ---------------- 剪贴板 / 粘贴 ----------------
    fun copyEntry(entry: FmEntry) {
        store.setClipboard(FmClipboard(listOf(entry.path), false))
    }
    fun cutEntry(entry: FmEntry) {
        store.setClipboard(FmClipboard(listOf(entry.path), true))
    }
    fun requestPaste() = pasteCtr.requestPaste()
    fun resolvePasteConflict(resolution: ConflictResolution) = pasteCtr.resolvePasteConflict(resolution)

    /** 复制 / 剪贴登记剪贴板后，关闭批量操作对话框 */
    fun bulkCopy() {
        val src = store.selectedEntries().map { it.path }
        if (!src.isEmpty()) {
            store.setClipboard(FmClipboard(src, false))
        }
        store.dismissDialog()
    }
    fun bulkCut() {
        val src = store.selectedEntries().map { it.path }
        if (!src.isEmpty()) {
            store.setClipboard(FmClipboard(src, true))
        }
        store.dismissDialog()
    }

    // ---------------- 压缩 ----------------

    fun bulkCompress() = compressCtr.bulkCompress()
    fun compressEntry(entry: FmEntry) = compressCtr.compressEntry(entry)
    fun onCompressSetupConfirmed(name: String, sources: List<Path>, options: CompressOptions) =
        compressCtr.onCompressSetupConfirmed(name, sources, options)
    fun onCompressOutputChoiceCurrent() = compressCtr.onCompressOutputChoiceCurrent()
    fun onCompressOutputChoiceSaf() = compressCtr.onCompressOutputChoiceSaf()
    fun onCompressOutputPickedCancelled() = compressCtr.onCompressOutputPickedCancelled()
    fun onCompressOutputPicked(treeUri: Uri) = compressCtr.onCompressOutputPicked(treeUri)
    fun resolveCompressConflict(resolution: ConflictResolution) = compressCtr.resolveCompressConflict(resolution)

    // ---------------- 解压 ----------------

    fun showExtract(entry: FmEntry) = extractCtr.showExtract(entry)
    fun onExtractSetupConfirmed(independentFolder: Boolean) = extractCtr.onExtractSetupConfirmed(independentFolder)
    fun onExtractPasswordConfirmed(password: String) = extractCtr.onExtractPasswordConfirmed(password)
    fun onExtractOutputChoiceCurrent() = extractCtr.onExtractOutputChoiceCurrent()
    fun onExtractOutputChoiceSaf() = extractCtr.onExtractOutputChoiceSaf()
    fun onExtractOutputPicked(treeUri: Uri) = extractCtr.onExtractOutputPicked(treeUri)
    fun onExtractOutputPickedCancelled() = extractCtr.onExtractOutputPickedCancelled()
    fun resolveExtractConflict(resolution: ConflictResolution) = extractCtr.resolveExtractConflict(resolution)

    // ---------------- 导入 ----------------

    fun showImportFilesDialog() {
        store.dismissDialog()
        store.updateState {
            it.copy(dialogIntent = DialogIntent.ImportFiles)
        }
    }
    fun showImportDirDialog() {
        store.dismissDialog()
        store.updateState {
            it.copy(dialogIntent = DialogIntent.ImportDir)
        }
    }
    fun onImportFiles(uris: List<Uri>) = importCtr.onImportFiles(uris)
    fun onImportDir(treeUri: Uri) = importCtr.onImportDir(treeUri)
    fun onImportCancelled() {
        store.dismissDialog()
    }

    // ---------------- 删除 / 重命名 / 新建 / 分享 ----------------

    fun stageSingleDelete(entry: FmEntry) = entryCtr.stageSingleDelete(entry)
    fun cancelStagedDelete() = entryCtr.cancelStagedDelete()
    fun deleteSelected(toTrash: Boolean) = entryCtr.deleteSelected(toTrash)
    fun rename(entry: FmEntry, newName: String, onSuccess: () -> Unit) = entryCtr.rename(entry, newName, onSuccess)
    fun submitCreate(name: String, isFolder: Boolean, onDone: (Boolean) -> Unit) = entryCtr.submitCreate(name, isFolder, onDone)
    fun submitRename(entry: FmEntry, newName: String, onSuccess: () -> Unit) = entryCtr.submitRename(entry, newName, onSuccess)
    fun validateRename(entry: FmEntry, newName: String): String? = entryCtr.validateRename(entry, newName)
    fun showShare(entry: FmEntry) = entryCtr.showShare(entry)

    // ---------------- 回收站 ----------------

    fun loadTrashList() = trashCtr.loadTrashList()
    fun refreshTrashList() = trashCtr.refreshTrashList()
    fun closeTrash() = trashCtr.closeTrash()
    fun setTrashSortConfig(config: TrashSortConfig) = trashCtr.setTrashSortConfig(config)
    fun trashRestore(items: List<TrashItem>, resolutions: Map<String, ConflictResolution>) = trashCtr.trashRestore(items, resolutions)
    fun beginTrashRestore(items: List<TrashItem>) = trashCtr.beginTrashRestore(items)
    fun resolveTrashRestoreConflict(resolution: ConflictResolution) = trashCtr.resolveTrashRestoreConflict(resolution)
    fun restoreTrashItem(item: TrashItem) = trashCtr.restoreTrashItem(item)
    fun trashRestoreAll() = trashCtr.trashRestoreAll()
    fun purgeTrashItem(item: TrashItem) = trashCtr.purgeTrashItem(item)
    fun trashPurge(items: List<TrashItem>) = trashCtr.trashPurge(items)
    fun trashClear() = trashCtr.trashClear()
    fun toggleTrashSelection(uuid: String) = trashCtr.toggleTrashSelection(uuid)
    fun selectAllTrash() = trashCtr.selectAllTrash()
    fun clearTrashSelection() = trashCtr.clearTrashSelection()
    fun trashRangeSelect(swipeItem: TrashItem) = trashCtr.trashRangeSelect(swipeItem)
    fun selectedTrashItems(): List<TrashItem> = trashCtr.selectedTrashItems()

    // ---------------- 对话框 / Snackbar ----------------

    fun dismissDialog() = store.dismissDialog()
    fun consumeSnackbar() = store.updateState { it.copy(snackbar = null) }
    fun consumeLocateHighlight() = store.updateState { it.copy(locateHighlightPath = null) }

    // ---------------- 任务进度同步 ----------------

    /** 订阅 [TaskManager] 的任务状态与进度，同步到状态集合。 */
    fun observeTasks() {
        viewModelScope.launch {
            taskManager.state.collect { st ->
                store.updateState { it.copy(taskState = st) }
            }
        }
        viewModelScope.launch {
            taskManager.progress.collect { p ->
                store.updateState { it.copy(taskProgress = p) }
            }
        }
    }

    /** 取消当前任务 */
    fun cancelCurrentTask() {
        taskManager.cancel()
    }

    /** 返回应用上下文 */
    fun appContext(): Context = context

    companion object {
        private const val TRASH_SUBDIR = "fileManagerTrash"

        /** [SavedStateHandle] 键：可访问范围目录绝对路径。 */
        const val KEY_ROOT_PATH = "fm.rootPath"

        /** [SavedStateHandle] 键：可选初始当前目录。 */
        const val KEY_CURRENT_PATH = "fm.currentPath"
    }
}
