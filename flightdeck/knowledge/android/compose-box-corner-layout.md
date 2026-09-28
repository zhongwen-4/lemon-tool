# ⚠ Box 里给多块内容都用 fillMaxSize 做四角布局：文字会互相压住（装饰层要用 matchParentSize）

SUMMARY: 照 SukiSU 主页「工作中」那张卡做结论卡时，把三块内容（右下大图标、左上大字、左下模块名）
都写成 `Box(Modifier.fillMaxSize()...)` 叠在同一个 `Box` 里 —— 真机上**文字压在文字上**。
真因：`fillMaxSize` 的每一层都参与定尺寸、又都被撑满同一格；`Row(height(IntrinsicSize.Min))`
只决定「有多高」，并不阻止三块重叠。正确做法：**装饰层用 `matchParentSize()`**（它不参与定尺寸，
只跟随内容层量出来的大小），内容层用一个普通 `Column(fillMaxWidth)` 自己决定卡片高度，靠 `Spacer` 拉开。
READ WHEN: before 用「四角 + 溢出被裁的大图标」那种卡片版式（上游 StatusCard / 英雄卡），
或用户反馈卡片里「文字重合 / 文字压在一起」时。
RECHECK WHEN: MiuiX 的 Card 换实现，或上游改了那张卡。

---

## 写法对照

```kotlin
// ✗ 三层 fillMaxSize 叠在一起：三块内容共享同一格，文字互相压
Card(...) {
    Box {
        Box(Modifier.fillMaxSize().offset(27.dp, 31.dp), contentAlignment = Alignment.BottomEnd) { Icon(...) }
        Box(Modifier.fillMaxSize().padding(16.dp, 14.dp), contentAlignment = Alignment.TopStart) { Column { 大字; 计数 } }
        Box(Modifier.fillMaxSize().padding(16.dp, 10.dp), contentAlignment = Alignment.BottomStart) { Text(模块名) }
    }
}

// ✓ 装饰层 matchParentSize（不参与定尺寸），内容层单列自定高
Card(...) {
    Box {
        Box(Modifier.matchParentSize().offset(27.dp, 31.dp), contentAlignment = Alignment.BottomEnd) { Icon(...) }
        Column(Modifier.fillMaxWidth().padding(...)) { 大字; Spacer(4.dp); 计数; Spacer(18.dp); 模块名 }
    }
}
```

## 三个附带结论（本项目实测）

- `matchParentSize()` 是 `BoxScope` 的**成员扩展**，**不用 import**：写了
  `import androidx.compose.foundation.layout.matchParentSize` 会直接 `Unresolved reference`。
- 右下角那枚大图标实际盖住右侧约 83dp（110dp 图标、`offset(27.dp, 31.dp)`、被卡片裁掉右下一角）：
  **左下角那行文字要留右内边距**（本项目给了 `96.dp` + `maxLines = 1` + `TextOverflow.Ellipsis`），
  否则长模块名会跑进图标里 —— 看起来又是「文字重合」。上游没这个问题，是因为它左下角只有
  `LKM` / `Built-in` 这种三个字母的短词，我们的模块名可以很长。
- 卡片标题直接写风险等级（高危 / 中危 / 低危）而不是核心给的 verdict 句子；只有**一档都没有**时
  才落到「未发现风险」—— 把「没发现问题」叫成「低危」是假话。