param([string]$Exe)

$ErrorActionPreference = 'Stop'
# 扫描器输出是 UTF-8 中文；Windows PowerShell 5.1 默认按 ANSI 解码会乱码，
# 下面有中文断言，先统一解码方式（pwsh 本来就是 UTF-8，无副作用）。
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if (-not $Exe) {
    foreach ($candidate in @('../../build/host/mrs.exe', '../../build/host/mrs')) {
        $probe = Join-Path $PSScriptRoot $candidate
        if (Test-Path -LiteralPath $probe) { $Exe = $probe; break }
    }
    if (-not $Exe) { throw '找不到 mrs 可执行文件，请用 -Exe 指定' }
}
$Exe = (Resolve-Path -LiteralPath $Exe).Path

Add-Type -AssemblyName System.IO.Compression.FileSystem

$fixtures = Join-Path $PSScriptRoot 'fixtures'
$out = Join-Path $PSScriptRoot '../../build/fixtures'
New-Item -ItemType Directory -Force -Path $out | Out-Null

function New-TestZip {
    param([string]$Source, [string]$ZipPath, [System.IO.Compression.CompressionLevel]$Level)
    if (Test-Path -LiteralPath $ZipPath) { Remove-Item -LiteralPath $ZipPath -Force }
    $zip = [System.IO.Compression.ZipFile]::Open($ZipPath, 'Create')
    try {
        Get-ChildItem -LiteralPath $Source -Recurse -File | ForEach-Object {
            $rel = $_.FullName.Substring($Source.Length + 1).Replace('\', '/')
            $entry = $zip.CreateEntry($rel, $Level)
            $stream = $entry.Open()
            $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
            $stream.Write($bytes, 0, $bytes.Length)
            $stream.Close()
        }
    } finally { $zip.Dispose() }
}

$goodZip = Join-Path $out 'good.zip'
$goodStoredZip = Join-Path $out 'good_stored.zip'
$evilZip = Join-Path $out 'evil.zip'
New-TestZip -Source (Join-Path $fixtures 'good') -ZipPath $goodZip -Level Optimal
New-TestZip -Source (Join-Path $fixtures 'good') -ZipPath $goodStoredZip -Level NoCompression
New-TestZip -Source (Join-Path $fixtures 'evil') -ZipPath $evilZip -Level Optimal

$failures = 0

function Assert-Contains {
    param([string]$Label, [string]$Text, [string]$Needle, [bool]$Expected)
    $found = $Text.Contains($Needle)
    if ($found -eq $Expected) {
        Write-Host "  ok   $Label"
    } else {
        $script:failures++
        $want = if ($Expected) { 'expected' } else { 'unexpected' }
        Write-Host "  FAIL $Label ($want '$Needle')"
    }
}

function Assert-Equal {
    param([string]$Label, $Actual, $Expected)
    if ($Actual -eq $Expected) {
        Write-Host "  ok   $Label"
    } else {
        $script:failures++
        Write-Host "  FAIL $Label (got '$Actual', want '$Expected')"
    }
}

Write-Host 'good.zip (deflate)'
$goodText = & $Exe $goodZip
Assert-Contains -Label 'module name parsed' -Text $goodText -Needle 'Good Module' -Expected $true
Assert-Contains -Label 'no curl rule' -Text $goodText -Needle 'cmd.curl' -Expected $false
Assert-Contains -Label 'no base64 blob rule' -Text $goodText -Needle 'obf.blob' -Expected $false
Assert-Contains -Label 'no missing module.prop' -Text $goodText -Needle 'module.prop.missing' -Expected $false
# 误报回归：善意样本一条高危/中危都不许有
Assert-Contains -Label 'benign stays out of high+medium' -Text $goodText -Needle '风险统计：高危 0 · 中危 0' -Expected $true
Assert-Contains -Label 'verdict says benign' -Text $goodText -Needle '未发现高危或中危行为' -Expected $true
Assert-Contains -Label 'rm -rf /data/local/tmp not escalated' -Text $goodText -Needle 'cmd.rm-rf-device' -Expected $false
Assert-Contains -Label 'no /data/adb rm inferred' -Text $goodText -Needle 'cmd.rm-rf-adb' -Expected $false
Assert-Contains -Label 'mount -o bind is not a remount' -Text $goodText -Needle 'cmd.remount-rw' -Expected $false
Assert-Contains -Label 'chmod 755 recorded as info' -Text $goodText -Needle '[信息] cmd.chmod755' -Expected $true
Assert-Contains -Label 'rm -rf still recorded as low' -Text $goodText -Needle '[低危] cmd.rm-rf' -Expected $true

Write-Host 'good_stored.zip (stored entries)'
$storedText = & $Exe $goodStoredZip
Assert-Contains -Label 'module name parsed' -Text $storedText -Needle 'Good Module' -Expected $true
Assert-Contains -Label 'no curl rule' -Text $storedText -Needle 'cmd.curl-sh' -Expected $false

Write-Host 'evil.zip (deflate)'
$evilText = & $Exe $evilZip
foreach ($rule in @('cmd.curl', 'cmd.wget', 'obf.base64', 'obf.eval', 'cmd.rm-rf',
                    'cmd.chmod777', 'cmd.mount', 'cmd.resetprop', 'cmd.iptables', 'cmd.dd',
                    'net.url', 'net.ip', 'obf.blob', 'cmd.setprop', 'cmd.su')) {
    Assert-Contains -Label "rule $rule" -Text $evilText -Needle $rule -Expected $true
}
# 升级判定：真正给出高危的那几种组合与路径
foreach ($rule in @('cmd.rm-rf-device', 'cmd.rm-rf-adb', 'net.exec-download', 'obf.exec-decode',
                    'cmd.dd-block', 'cmd.resetprop-ro', 'obf.eval-dynamic', 'cmd.chmod777-system',
                    'cmd.remount-rw')) {
    Assert-Contains -Label "escalation $rule" -Text $evilText -Needle $rule -Expected $true
}
Assert-Contains -Label 'hook listed' -Text $evilText -Needle 'customize.sh' -Expected $true
Assert-Contains -Label 'system overwrite noted' -Text $evilText -Needle 'system/' -Expected $true

Write-Host 'evil dir'
$evilDirText = & $Exe (Join-Path $fixtures 'evil')
Assert-Contains -Label 'dir scan finds curl' -Text $evilDirText -Needle 'cmd.curl' -Expected $true

Write-Host 'json output'
$json = & $Exe $evilZip --json
Assert-Contains -Label 'json has severity' -Text $json -Needle '"severity":"high"' -Expected $true
Assert-Contains -Label 'json has module id' -Text $json -Needle '"id":"test.evil"' -Expected $true

Write-Host 'json 计数与结论'
$goodReport = ((& $Exe $goodZip --json) -join '') | ConvertFrom-Json
Assert-Equal -Label 'good counts.high' -Actual $goodReport.counts.high -Expected 0
Assert-Equal -Label 'good counts.medium' -Actual $goodReport.counts.medium -Expected 0
Assert-Contains -Label 'good verdict benign' -Text $goodReport.verdict -Needle '未发现高危或中危行为' -Expected $true
$evilReport = ((& $Exe $evilZip --json) -join '') | ConvertFrom-Json
Assert-Equal -Label 'evil has high' -Actual ($evilReport.counts.high -gt 0) -Expected $true
Assert-Contains -Label 'evil verdict warns' -Text $evilReport.verdict -Needle '建议不要安装' -Expected $true

Write-Host ''
if ($failures -eq 0) {
    Write-Host 'ALL TESTS PASSED'
    exit 0
}
Write-Host "$failures CHECK(S) FAILED"
exit 1
