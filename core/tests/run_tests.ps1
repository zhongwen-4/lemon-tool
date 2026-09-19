param([string]$Exe)

$ErrorActionPreference = 'Stop'
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

Write-Host 'good.zip (deflate)'
$goodText = & $Exe $goodZip
Assert-Contains -Label 'module name parsed' -Text $goodText -Needle 'Good Module' -Expected $true
Assert-Contains -Label 'no curl rule' -Text $goodText -Needle 'cmd.curl-sh' -Expected $false
Assert-Contains -Label 'no base64 blob rule' -Text $goodText -Needle 'obf.blob' -Expected $false
Assert-Contains -Label 'no missing module.prop' -Text $goodText -Needle 'module.prop.missing' -Expected $false
Assert-Contains -Label 'subdir rm is not root rm' -Text $goodText -Needle 'cmd.rm-rf-root' -Expected $false

Write-Host 'good_stored.zip (stored entries)'
$storedText = & $Exe $goodStoredZip
Assert-Contains -Label 'module name parsed' -Text $storedText -Needle 'Good Module' -Expected $true
Assert-Contains -Label 'no curl rule' -Text $storedText -Needle 'cmd.curl-sh' -Expected $false

Write-Host 'evil.zip (deflate)'
$evilText = & $Exe $evilZip
foreach ($rule in @('cmd.curl-sh', 'cmd.wget-sh', 'obf.base64', 'obf.eval', 'cmd.rm-rf',
                    'cmd.chmod777', 'cmd.mount', 'cmd.resetprop', 'cmd.iptables', 'cmd.dd',
                    'net.url', 'net.ip', 'obf.blob', 'cmd.setprop', 'cmd.su')) {
    Assert-Contains -Label "rule $rule" -Text $evilText -Needle $rule -Expected $true
}
Assert-Contains -Label 'hook listed' -Text $evilText -Needle 'customize.sh' -Expected $true
Assert-Contains -Label 'system overwrite noted' -Text $evilText -Needle 'system/' -Expected $true

Write-Host 'evil dir'
$evilDirText = & $Exe (Join-Path $fixtures 'evil')
Assert-Contains -Label 'dir scan finds curl' -Text $evilDirText -Needle 'cmd.curl-sh' -Expected $true

Write-Host 'json output'
$json = & $Exe $evilZip --json
Assert-Contains -Label 'json has severity' -Text $json -Needle '"severity":"high"' -Expected $true
Assert-Contains -Label 'json has module id' -Text $json -Needle '"id":"test.evil"' -Expected $true

Write-Host ''
if ($failures -eq 0) {
    Write-Host 'ALL TESTS PASSED'
    exit 0
}
Write-Host "$failures CHECK(S) FAILED"
exit 1
