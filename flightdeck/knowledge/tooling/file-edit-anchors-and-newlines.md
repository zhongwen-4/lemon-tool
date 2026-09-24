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