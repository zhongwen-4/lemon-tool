# ⚠ 改文件的两个坑：here-string 吃掉末尾换行、行尾要按文件类型给（.kt 是 LF，deck 的 .md 是 CRLF）

SUMMARY: 在本机用「读全文 → `String.Replace` → `WriteAllText`」改文件时，栽过两次：
① PowerShell 的 `@'...'@` / `@"..."@` **内容不含末尾那个换行**，拿它当替换文本会把插入块的最后一行
与原有的下一行**粘成一行**（本次因此在 `ScanUi.kt` 造出 `import aimport b`，编译报
`Expecting a top level declaration`，还顺带带出几十条 `Unresolved reference 'Composable'` 之类的假错误）。
拼接时一律显式补 `` `n ``。
② **锚点的行尾必须跟文件实际一致**：仓库里 `.kt` 是 **LF**，`build.gradle` 是 **CRLF**，
`flightdeck/**/*.md` 是 **CRLF**；给错了 `Contains` 直接返回 false，替换**静默 MISS**、文件不变。
动手前先判一次：`$nl = if ($t.Contains("`r`n")) { "`r`n" } else { "`n" }`，再拿 `$nl` 拼锚点。

READ WHEN: when 用脚本改文件、Replace 看着成功却没生效、或改完编译报出一整片语法／未解析错误时。

RECHECK WHEN: 仓库改了换行策略、或本机换掉 apply_patch 的包装方式之后（见
`apply-patch-on-windows-powershell.md`）。

---

## 症状与修法

- **粘行**：`import ximport y`。Kotlin 会报 `Expecting a top level declaration` /
  `imports are only allowed in the beginning of file`，并由这一个假错误牵连出后面一大片
  `Unresolved reference 'Composable'`、`@Composable invocations can only happen from ...`。
  **只看第一条错的坐标**，后面的基本都是它的回声。
- **静默 MISS**：锚点写错（尤其行尾）时 `Replace` 什么也不做，脚本照样打印「已写入」。
  所以每一步替换都要打命中结果：
  ```powershell
  if ($t.Contains($old)) { $t = $t.Replace($old, $new); "OK $tag" } else { "MISS $tag" }
  ```
- 写完**立刻回读几行**确认（本次就是回读才发现两行被粘住）。
- 别用 `$script:t` 之类的跨作用域写法玩花活（函数里读到的可能是空串，导致全部 MISS）；
  老老实实在同一作用域里顺序执行、或用数组收集「锚点/替换/标签」再统一跑。

## 第三个改法：按行号切片重拼（2026-09-27 补）

改大段版式时，比 apply_patch 与 Replace 更好用的是「读成数组 → 按行号切片 → 拼回」。
但它有自己的一种失手方式：**切片边界算错会静默删掉整段代码**，而且删的往往是
「看起来不在改动范围里」的那段。本机就栽过一次：重写 `History.kt` 的界面区时把
`A = 行 1..37` 写成 `A = 行 1..133`，于是 `HistoryEntry`、`historyEntryOf`、`HistoryStore`
整段（行 38..133）被抹掉——文件照样写成功，脚本照样打印 OK。

规矩：
- 切之前**先把边界那两行打印出来确认**（`"$($l[132])"` 之类），别只看行数。
- 拼完立刻搜关键符号计数（`data class` / `object` / 关键函数名 各出现几次），**期望值写死**，
  像下面这样，比「能编译」更早发现问题：
  ```powershell
  foreach ($k in 'data class HistoryEntry','object HistoryStore','fun HistoryScreen') {
    "$k = " + (Select-String -LiteralPath $p -Pattern ([regex]::Escape($k)) | Measure-Object).Count
  }
  ```
- 万一真切掉了，`git show HEAD:<仓库内路径>` 能把原文按行取回来（本机实测中文不乱码，
  但先设 `[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)`），
  再把缺的行号区间插回去，比自己重写一遍安全。

## 2026-09-28 又栽一次：切片端点是肉眼从 dump 里读的，容易差一位

同一招（按行号切片重拼）当天第二次失手，形态是**丢一个闭合花括号**：把 `StatusCard(...)` 与
`InfoCard(...)` 两处调用换成新调用时，切片写成了 `$o[99..169]`，而那个 `}`（闭合上面 `if` 块的最后一行）
正好落在 `$o[170]` —— 于是它跟着被"替换"掉。文件照样写成功，脚本照样打印行数，
报错要到很远的 `HomePagerMiuix` 结尾才出现，看起来像"多了个括号"，其实是少了一个。

规矩（在上面的三条之外）：

- 切片端点**不要凭 dump 的肉眼计数**：dump 里的前缀（`"$i|"`）会让人把 0 基/1 基数错。
  用代码取，别用眼睛数：`[regex]::Match($ls[$i],'^\s*').Value` 拿缩进、`"$i|$($ls[$i])"` 拿内容，
  而且**改完立刻再 dump 一次边界两侧各 3 行**复核。
- 结构敏感的文件（Kotlin）拼完先数一次花括号平衡，比"能编译"更早暴露问题：
  ```powershell
  $t = [System.IO.File]::ReadAllText($p)
  "open=" + ([regex]::Matches($t,'\{')).Count + " close=" + ([regex]::Matches($t,'\}')).Count
  ```

## 2026-09-28 第三次：「脚本报 OK、文件没变」出现过两次

两次都是**插入型**改动静默没生效：一次是给 `MainActivity.kt` 插一行 import（`Contains` 为真、
打印 OK，回读却发现没有），一次是给 work index 的 `## Read now` 插一行指针（同样打印 OK、
`Select-String` 却搜不到）。**重跑同一条命令就过了**，两次的锚点事后手工验证都能匹配，
所以不是锚点问题——是这套「读全文 → Replace → WriteAllText」在本机偶发不稳。

结论（比"再确认一次锚点"更实用）：

- **别信脚本自己打印的 OK。** 写完整份文件后立刻回读断言，断言才是唯一的成功判据：
  ```powershell
  $v = [System.IO.File]::ReadAllText($p)
  "ok=$($v.Contains($new))"
  ```
- 断言为 false 就**重跑同一条命令**（别先怀疑锚点、别急着换写法），本机两次都是重跑即中。
- 顺手把 `$nl = if ($t.Contains("`r`n")) { "`r`n" } else { "`n" }` 先算出来再拼锚点，
  别在 `@()` 数组里用 `'...' + "`r`n" + '...'` 这种就地拼接——它是这两次里共有的写法特征。
- 同一轮里改多个位置时，把「锚点/替换/标签」收集成数组再统一跑**并不能**豁免这条：
  上面两次就是这么写的。
