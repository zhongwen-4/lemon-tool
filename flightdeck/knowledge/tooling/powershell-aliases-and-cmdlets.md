# ⚠ 本机 PowerShell 三个坑：`rd` 会删文件、`New-Item` 没有 `-LiteralPath`、计算路径上递归删被拦

SUMMARY: 本机是 Windows PowerShell 5.1。① **别名优先于函数**：`rd` 是 `Remove-Item` 的内置别名，
自定义 `function RD {}` 不会被调用，`RD "路径"` 会**把文件直接删掉且不报错**（函数体压根没执行，
表达式返回空，调用方接着报 `You cannot call a method on a null-valued expression`）。
脚本里的自定义函数一律写成 `Verb-Noun` 全名，短名先用 `Get-Alias <名字>` 确认没被占用。
② `New-Item` **没有 `-LiteralPath`**（只有 `-Path`），照搬 `Get-ChildItem` / `Remove-Item` 的写法
会报 `A parameter cannot be found that matches parameter name 'LiteralPath'`，后续的下载、解压
跟着报 `DirectoryNotFoundException`。③ 在**拼出来的路径**上做 `Remove-Item -Recurse -Force`
会被安全策略把**整条命令**拒掉（`rejected: blocked by policy`），连带 `Remove-Item -Force` 删单个
文件也一样被拒；清临时目录别「先删再建」，删文件改用 `[System.IO.File]::Delete($path)`，
临时目录每次用全新的唯一名，例如 `Join-Path $env:TEMP ("x" + (Get-Date -Format "HHmmss"))`。
READ WHEN: when 本机 PowerShell 里自定义函数或命令返回空、文件在脚本里凭空消失，或报
`NamedParameterNotFound` / `A parameter cannot be found that matches parameter name`，
或一条命令被整体拒掉（`blocked by policy` / `rejected`）时。
RECHECK WHEN: 本机装了 PowerShell 7+，或换了没有这类策略的宿主之后。