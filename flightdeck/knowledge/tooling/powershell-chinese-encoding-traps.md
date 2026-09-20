# ⚠ PowerShell 里的中文：两个编码陷阱

SUMMARY: 本机只有 Windows PowerShell 5.1（**没装 pwsh**）。它把**无 BOM 的 UTF-8 `.ps1`**
当 ANSI（本机 GBK）读，脚本里的中文字面量会变成乱码、断言必然失败；而且用 `& exe` 捕获原生
程序 stdout 时按 `[Console]::OutputEncoding` 解码，程序吐出的 UTF-8 中文同样会乱。带中文的
脚本一律存成 **UTF-8 with BOM**，并在开头设 `[Console]::OutputEncoding = UTF8`。
READ WHEN: when 脚本里的中文比较/输出对不上，或断言在 CI（pwsh）全过而本机（5.1）不过时。
RECHECK WHEN: 本机装了 PowerShell 7+，或脚本改成不含中文之后。

---

## 症状

- `core/tests/run_tests.ps1` 里的中文 needle 永远匹配不上，`[信息] cmd.chmod755` 这种断言必挂。
- 用探针脚本验证过：写成 UTF-8 无 BOM 的 `Write-Output '风险统计：高危 0'`，5.1 打印出
  `椋庨櫓缁熻锛氶珮鍗?0`（按 GBK 解码 UTF-8 字节的典型乱码）。
- 反过来，`& $Exe` 捕获 `mrs.exe` 的 UTF-8 中文输出时，也会因为控制台输出编码不是 UTF-8 而乱。

## 正确做法

1. 脚本存成 UTF-8 **with BOM**（BOM 是 5.1 识别 UTF-8 的唯一可靠信号）：

```powershell
$p = 'D:\lemon_tool\core\tests\run_tests.ps1'
$text = [System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8)
[System.IO.File]::WriteAllText($p, $text, (New-Object System.Text.UTF8Encoding($true)))
```

2. 脚本开头统一输出解码（pwsh 本来就是 UTF-8，无副作用）：

```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

## 相关事实

- CI 用 `shell: pwsh`，默认按 UTF-8 读脚本、输出也是 UTF-8，所以**同一个脚本会在 CI 全绿、
  本机全红** —— 看到这种分裂先怀疑编码，别怀疑逻辑。
- `apply_patch` 改过的文件会丢掉 BOM：补完 BOM 之后如果还要再 patch 该文件，记得复查首三字节
  是不是 `ef bb bf`。
- 同样的坑适用于**任何重写动作**：`[IO.File]::WriteAllText($p, $text, (New-Object Text.UTF8Encoding($false)))`   也会把 BOM 抹掉（2026-09-21 改 `run_tests.ps1` 时因此本机断言满天红、CI 却全绿）。重写完带中文的   `.ps1` 之后，养成 `[IO.File]::ReadAllBytes($p)[0..2]` 看一眼是不是 `239 187 191` 的习惯。
