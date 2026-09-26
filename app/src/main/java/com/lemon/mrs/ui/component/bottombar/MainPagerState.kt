/*
 * 移植自 SukiSU Ultra（GPL-3.0）：
 *   manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBar.kt（只取 MainPagerState 那一段）
 * 改动：① 去掉 UiMode、角标、毛玻璃那些部分；② 翻页动画改用 Compose 自带的 animateScrollToPage
 *       （上游用的是 miuix 的 springAnimateToPage）；③ LocalMainPagerState 从 MainActivity 挪到这里。
 *       其余与上游一致。
 *
 * 改动日期：2026-09-27。
 */

package com.lemon.mrs.ui.component.bottombar

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/** 页面容器：底栏读它决定高亮哪一格，页面读它做「点 tab 翻页」。 */
val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> {
    error("LocalMainPagerState not provided")
}

/**
 * 把 pager 的当前页与底栏选中的那一格对齐：
 * 点/拖底栏走 [animateToPage]（会先亮起目标格再翻页，避免高亮回跳），手指滑 pager 走 [syncPage]。
 */
class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
    private val animatePageChanges: Boolean,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()

        selectedPage = targetIndex
        isNavigating = true

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                if (animatePageChanges) {
                    pagerState.animateScrollToPage(targetIndex)
                } else {
                    pagerState.scrollToPage(targetIndex)
                }
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    animatePageChanges: Boolean = true,
): MainPagerState {
    return remember(pagerState, coroutineScope, animatePageChanges) {
        MainPagerState(pagerState, coroutineScope, animatePageChanges)
    }
}
