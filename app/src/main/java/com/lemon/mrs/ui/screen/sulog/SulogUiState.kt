// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/sulog/SulogUiState.kt
// 改动（改动日期：2026-09-28）：
//   ① 删掉 SulogActions.onBack（本项目的日志列表是 Tab 页，没有返回栈）与 SulogFileSelector
//      （日志文件下拉不要），其余字段与上游一字不差。
//   ② 每个字段都给了空默认值，SulogScreenState() 就是一个「数据空着」的骨架状态；
//      每个 action 都给了空实现，接真实数据时再传进来。
package com.lemon.mrs.ui.screen.sulog

import com.lemon.mrs.ui.util.sulog.SulogEntry
import com.lemon.mrs.ui.util.sulog.SulogEventFilter
import com.lemon.mrs.ui.util.sulog.SulogFile

data class SulogScreenState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val sulogStatus: String = "",
    val isSulogEnabled: Boolean = false,
    val searchText: String = "",
    val selectedFilters: Set<SulogEventFilter> = emptySet(),
    val files: List<SulogFile> = emptyList(),
    val selectedFilePath: String? = null,
    val entries: List<SulogEntry> = emptyList(),
    val visibleEntries: List<SulogEntry> = emptyList(),
    val errorMessage: String? = null,
)

data class SulogActions(
    val onRefresh: () -> Unit = {},
    val onEnableSulog: () -> Unit = {},
    val onCleanFile: () -> Unit = {},
    val onSearchTextChange: (String) -> Unit = {},
    val onToggleFilter: (SulogEventFilter) -> Unit = {},
    val onSelectFile: (String) -> Unit = {},
)