// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/sulog/SulogUiState.kt
// 改动清单：docs/sukisu-port-changes.md#suloguistate
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