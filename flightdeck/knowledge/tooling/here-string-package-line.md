# 用 here-string 写 Kotlin 文件：丢了换行会把 package 粘进注释，症状却像「编译器缓存坏了」

SUMMARY: 本机 `apply_patch` 是坏的 `.bat` 包装，多行补丁传不进去，所以改文件一律用
`[System.IO.File]::WriteAllText` + here-string（`@'...'@`，字面量，不吃 `$` 插值）。
这条本身没问题，但 here-string **不含末尾换行**，一拼头一拼身就会出事：

1. `$header + $body` 少一个 `"`n"` → 头注释最后一行和正文第一行粘成一行。
   如果正文第一行是 `package xxx`，它就变成注释的一部分，
   **整个文件没有 package 声明 → 落到默认包**。
   症状：同目录同包的别的文件里冒出一堆 `Unresolved reference 'Xxx'`（Xxx 是本文件里的 data class），
   报错位置全在**引用方**，本文件自己一条错都没有。极像 Kotlin 增量编译缓存不认识新文件。
   （2026-09-28 实际被这个骗了一轮：`HomeMiuix.kt` / `HomeUiState.kt` 两行 `package` 都粘在
   `// 改动日期：2026-09-28。package com.lemon.mrs...` 后面；`--rerun-tasks`、`:app:clean`、
   `-Pkotlin.incremental=false` 全都白试。）
2. 给「已经是完整文件」的上游源码加头时，头里**不要**再写一行 `package`，
   否则文件里出现两个 `package`，Kotlin 报的是 `Syntax error: Expecting a top level declaration`
   + `imports are only allowed in the beginning of file`。

**凡是脚本拼出来的 .kt，写完必须验：**

```powershell
Get-ChildItem -Recurse -File -Filter *.kt <dir> | ForEach-Object {
  $ls = ([System.IO.File]::ReadAllText($_.FullName)) -split "`n"
  if (-not ($ls | Where-Object { $_ -match '^package ' })) { "BAD $($_.FullName)" }
}
```
写完这一段立刻编译，别攒着。

另一个相关的坑：本机 `Get-Content` 用默认编码（GBK）读 UTF-8 文件，中文全是乱码，
**看不出到底粘没粘行**（`。package` 这种拼接在乱码里也一样显示）；
判行要用 `-Encoding UTF8` 或上面的按 `\n` 切，别用眼睛看控制台。
READ WHEN: before 用脚本/here-string 往仓库里写 .kt（尤其从上游拷文件加文件头）。
RECHECK WHEN: 本机修好 `apply_patch`、或改用别的写文件通道之后。