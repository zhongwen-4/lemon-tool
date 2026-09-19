# ⚠ Windows PowerShell 下 apply_patch 的调用限制

SUMMARY: 本机命令行是 PowerShell、没有 bash；apply_patch 必须作为命令的第一个 token 调用，
heredoc 前面不能有任何语句，否则整个脚本解析失败、patch 根本不会执行。
READ WHEN: when 调用 apply_patch 报 `Invalid patch` / `The last line of the patch must be
'*** End Patch'`，或 PowerShell 报 `< operator is reserved for future use` 时。
RECHECK WHEN: 换 shell、换机器，或 apply_patch 升级后行为变化时。

---

## 症状

- `apply_patch << 'PATCH'` 前面接了别的语句（如 `New-Item ...; apply_patch << 'PATCH'`）→
  PowerShell 报 `Missing file specification after redirection operator`、
  `The '<' operator is reserved for future use`，并把 patch 正文当成表达式逐行报错。
  注意：这是**解析期**失败，前面的语句也没执行——白跑。
- 把 patch 当 PowerShell 字符串参数传（`$p = @'...'@; apply_patch $p`）→
  `Invalid patch: The last line of the patch must be '*** End Patch'`；
  用管道 `$p | apply_patch` → `--codex-run-as-apply-patch requires a UTF-8 PATCH argument`。

## 正确做法

- 一条命令只做一件事：先单独跑 `New-Item -ItemType Directory -Force -Path ...` 建目录，
- 再单独跑一条**以 `apply_patch << 'PATCH'` 开头**的命令，patch 结束于 `*** End Patch`。
  该形式是唯一验证可用的调用方式（本机无 bash，`bash`/`sh` 均不存在）。

## 兜底

确实需要在一个脚本里做多步文件操作时，改用 .NET 写文件：
`[System.IO.File]::WriteAllText($path, $text)`——默认 UTF-8 无 BOM，
再按需把行尾统一成 CRLF。中文内容用这种方式写入不会乱码。
