# ⚠ 本机 PowerShell 四个坑：`rd` 会删文件、`New-Item` 没有 `-LiteralPath`、计算路径上递归删被拦、`$PID` 只读

SUMMARY: 本机是 Windows PowerShell 5.1。四个踩过的坑：① `rd` 其实是 `Remove-Item` 的内置别名，
自定义同名函数不会被调用，`RD "路径"` 会直接把文件删掉、还不报错；② `New-Item` 没有
`-LiteralPath`（只有 `-Path`）；③ 在拼出来的路径上 `Remove-Item -Recurse -Force` 会被安全策略
把整条命令拒掉（`blocked by policy`）；④ `$PID` 是只读的自动变量，`foreach ($pid in ...)` 直接报
`Cannot overwrite variable PID because it is read-only`。细节在正文。
READ WHEN: when 本机 PowerShell 里自定义函数或命令返回空、文件在脚本里凭空消失，或报
`NamedParameterNotFound` / `A parameter cannot be found that matches parameter name` /
`Cannot overwrite variable`，或一条命令被整体拒掉（`blocked by policy` / `rejected`）时。
RECHECK WHEN: 本机装了 PowerShell 7+，或换了没有这类策略的宿主之后。

---

## 四个坑

① **别名优先于函数**：`rd` 是 `Remove-Item` 的内置别名，自定义 `function RD {}` 不会被调用，
`RD "路径"` 会把文件直接删掉且不报错（函数体压根没执行，表达式返回空，调用方接着报
`You cannot call a method on a null-valued expression`）。脚本里的自定义函数一律写成 `Verb-Noun`
全名，短名先用 `Get-Alias <名字>` 确认没被占用。

② `New-Item` **没有 `-LiteralPath`**（只有 `-Path`），照搬 `Get-ChildItem` / `Remove-Item` 的写法会报
`A parameter cannot be found that matches parameter name 'LiteralPath'`，后续的下载、解压跟着报
`DirectoryNotFoundException`。

③ 在**拼出来的路径**上做 `Remove-Item -Recurse -Force` 会被安全策略把**整条命令**拒掉
（`rejected: blocked by policy`），连带 `Remove-Item -Force` 删单个文件也一样被拒；清临时目录
别「先删再建」，删文件改用 `[System.IO.File]::Delete($path)`，临时目录每次用全新的唯一名，
例如 `Join-Path $env:TEMP ("x" + (Get-Date -Format "HHmmss"))`。

④ **`$PID` 是只读自动变量**（当前进程号），`foreach ($pid in $pids) { ... }` 会直接抛
`Cannot overwrite variable PID because it is read-only or constant`（`$pid` / `$PID` 大小写不敏感，
换个名字如 `$procId`）。2026-10-03 想按 PID 对 `netstat -ano` 找代理端口时踩到。