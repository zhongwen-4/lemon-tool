# ⚠ 改文件的两个坑：here-string 吃掉末尾换行、行尾要按文件类型给（.kt 是 LF，deck 的 .md 是 CRLF）

SUMMARY: 在本机用「读全文 → `String.Replace` → `WriteAllText`」改文件时，栽过两次：
① PowerShell 的 `@'...'@` / `@"..."@` **内容不含末尾那个换行**，拿它当替换文本会把插入块的最后一行
与原有的下一行**粘成一行**（本次因此在 `ScanUi.kt` 造出 `import aimport b`，编译报
`Expecting a top level declaration`，还顺带带出几十条 `Unresolved reference 'Composable'` 之类的假错误）。
拼接时一律显式补 `` `n ``。
② **锚点的行尾必须跟文件实际一致**：仓库里 `.kt` 是 **LF**，`build.gradle` 是 **CRLF**，
`flightdeck/**/*.md` 是 **CRLF**；给错了 `Contains` 直接返回 false，替换**静默 MISS**、文件不变。
动手前先判一次：`$nl = if ($t.Contains("`r`n")) { "`r`n" } else { "`n" }`，再拿 `$nl` 拼锚点。

③ **守卫与拼接本身也会错**（2026-09-29 同一次改版本号时同时撞上）：`@(@($a,$b))` 会被**拍平**成两元素数组
（`$x[0]` 变成首字符，命中数假报 687）；二元 `-join` 的优先级**低于** `+`，`@(...) -join $nl + $nl` 等于
`-join ($nl + $nl)`，插入块里每两行之间会多一个空行、行尾还少一个换行。详见文末「第六次」。

READ WHEN: when 用脚本改文件、Replace 看着成功却没生效、锚点命中数报「不唯一／为 0」、批量替换中途报错、或改完编译报出一整片语法／未解析错误时。

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
## 2026-09-29 第四次：把 here-string 当替换文本用，又是丢末尾换行

给 `SettingsMiuix.kt` 插一行 SwitchPreference 时，把 `$row = @'...'@` 直接当替换文本，
于是插入块的最后一行 `)` 与原有的下一行粘成 `)                    SwitchPreference(` ——
编译报的是 `Unresolved reference 'SwitchPreference' on receiver of type 'Unit'` 加一串
`Expecting ')'`，看着像括号写错，其实是少了一个换行。

规矩：here-string 只用来装「多行正文」，**每次拼接都显式补一个换行**；
`$row + $anchor` 这种写法改成 `$row + "`n" + $anchor`。
## 2026-09-29 第五次：按标记截断文件，标记**之前**的同名函数被留下了

重写 `HomeMiuix.kt` 的「检查模块」那一段时，用 `IndexOf('// ---- 检查模块')` 截断、再拼上新的尾部。
问题是标记**之后的**内容才是要换掉的，而**标记之前**正好还躺着一个同名函数 `CheckEntryCard`（旧版），
于是新尾部又定义了一个同名函数 → 编译报 `Conflicting overloads: ... CheckEntryCard`。

规矩：截断前先确认「这个标记之前的最后一个函数是谁、新尾部有没有同名函数」；
改完先数一遍重名：

```powershell
([regex]::Matches($t, 'private fun CheckEntryCard')).Count   # 期望 1
```

## 2026-09-29 第六次：命中数守卫自己被写坏了——`@(@($a,$b))` 会被拍平成两元素数组

给 `app/build.gradle` 换版本号时，把「锚点 / 替换」写成 `$pair = @(@($o,$n))` 再 `foreach ($x in $pair)` 遍历：
PowerShell 的 `@()` **不是**「数组的数组」，`@(@($o,$n))` 会被**拍平**成两个字符串，于是 `$x[0]` 取到的是
**字符串的首字符**（一个空格），`[regex]::Escape(' ')` 在三千字的文件里命中 **687** 次 —— 守卫于是报
`anchor not unique`。锚点其实完全正常（单独跑 `hits=1`，脚本随后一次就改成了）。

规矩：

- 要成对遍历就写 `$pairs = @( ,@($o,$n) )`（前置逗号防拍平），或者干脆两个变量、两次调用。
- **守卫报的数不合常理时先怀疑守卫自己**：命中数远大于 1、或为 0，先把 pattern 打出来看是什么 ——
  `"$([regex]::Escape($old))"`。一个空格、一个字符的 pattern 几乎必然是这个锅。
- **同一轮还栽了第二个**：`@(...) -join $nl + $nl` —— 二元 `-join` 的优先级**低于** `+`，这一句等于
  `-join ($nl + $nl)`，于是插进去的**每两行之间都多一个空行**，而且行尾的 `+ $nl` 也跟着失效、
  把插入块与原来的下一行粘在一起。**必须加括号**：`(@(...) -join $nl) + $nl`。
  更阴的是：这次的回读断言**没抓住它** —— 断言比的就是用同一句表达式构造出来的期望串，属于自证。
  所以断言要用「手写的正确文本」或行数/结构检查，别用构造期望串的那句表达式。

## 2026-09-29 第七次：批量替换中途抛异常 —— 前面的已经落盘，别从头重跑

把「锚点 / 替换」收集成一批逐条跑时，中间一条 `throw`（锚点打错、`hits=0`）会让脚本停在半路：
**它前面那几条早就写进文件了**，后面的没跑。这时从头重跑整批，前面的锚点已经不存在 → 又是 `hits=0`，
看着像「文件被改坏了」，其实只是半批生效。

规矩：

- 抛异常后先 `Select-String` 看哪几条已经生效，**只重跑剩下那几条**（几秒的事）。
- 更省事：批量前先做一轮**只查不写**的锚点体检（`hits` 全为 1 再开写），写的时候就不用赌中途会不会停。

## 2026-09-30 第八次：锚点手抄漏了一个 `val`；切片按 `)` 收尾会把下一行的 `{` 咬走

一次改 `SulogModels.kt`（加结构化数据类）连中三处，都属于「本可以避免」：

- **锚点别手抄，从文件里切。** 我照着 dump 抄了一行 `    fields: Map<String, String>,`，
  文件里其实是 `    val fields: Map<String, String>,` —— 少一个 `val`，`hits=0`。
  省事的排查法：拿锚点去 `$t.IndexOf($old)` 一看便知；更省事的是**用 `Substring` 从文件里切出锚点**，
  再拿它做替换目标。
- **切片替换的终点要吃掉完整的一行结构。** 我把旧块的结尾定在 `)`，而文件里是 `) {`（后面跟着
  `val searchableText` 那段成员）。替换完，那段成员就落到**下一个 data class** 里了 ——
  编译报一串 `Unresolved reference 'rawLine' / 'timestampText' / 'fields'`，看着像字段丢了，其实是粘连。
  规矩：端点用**整行**标记（`")"+nl` 不够就带上后面的 `{`），或者干脆拿下一个函数的 `/**` / `@Composable`
  行当终点。
- **整段搬代码（从一个类挪到另一个类）就一次重写整个区间**，别用「先删一块、再插一块」两刀：
  两刀的端点很难对齐，这次就是这么出的错。写法：`$s=IndexOf(起点标记)`、`$e=IndexOf(终点标记)`，
  `$t.Substring(0,$s) + 新文本 + $t.Substring($e)`，终点标记选**下一个段落自己的头**（如 `/**` 注释首行）。
- 顺带：`Replace-Once` 的 `tag` 打印出奇怪的片段（比如 `"+\n+)"`）时，说明**实参错位**了 ——
  PowerShell 把带括号的表达式当成了多个参数组。先把要传的字符串**赋给变量**再调用，
  别在实参位置拼字符串。

经验：这轮 30 秒的 `compileDebugKotlin` 比肉眼扫一遍快得多，结构改动后**直接让编译器告诉你**。