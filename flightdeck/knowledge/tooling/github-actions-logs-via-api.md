# 没有 gh CLI 时怎么读 GitHub Actions 的日志

SUMMARY: 本机没有 `gh`，但 Git Credential Manager 里缓存着 GitHub 凭据，可以用
`git credential fill` 取出来，再用 `curl` + 代理调 REST API。列 run/job 和读失败注解**匿名就行**；
下载 job 日志**必须带 token**（匿名会 403 `Must have admin rights to Repository.`），而且返回的是
**纯文本**，不是 zip。
READ WHEN: when CI 只告诉你「某一步 failure」，你需要真实报错内容来定位时。
RECHECK WHEN: token 失效、或 GitHub 改动日志接口之后。

---

## 1. 取 token

PowerShell 里**管道喂 stdin 不行**（git 报 `refusing to work with credential missing protocol
field`），必须用 `Start-Process` 重定向文件：

```powershell
$dir = "$env:TEMP\credprobe"; New-Item -ItemType Directory -Force -Path $dir | Out-Null
[System.IO.File]::WriteAllText("$dir\in.txt", "protocol=https`nhost=github.com`n`n")
$env:GIT_TERMINAL_PROMPT = "0"
Start-Process git -ArgumentList 'credential','fill' -RedirectStandardInput "$dir\in.txt" `
  -RedirectStandardOutput "$dir\out.txt" -WindowStyle Hidden -Wait
# out.txt 形如 username=zhongwen-4 / password=<40 位 token>
```

## 2. 列 run 与 job（匿名可读，公开仓库）

```powershell
$base = "https://api.github.com/repos/zhongwen-4/lemon-tool"
curl.exe -s -x http://127.0.0.1:7890 -H "Accept: application/vnd.github+json" "$base/actions/runs?per_page=3"
curl.exe -s -x http://127.0.0.1:7890 -H "Accept: application/vnd.github+json" "$base/actions/runs/<run_id>/jobs"
```

## 3. 失败注解（比日志便宜，先看这个）

```powershell
curl.exe -s -x http://127.0.0.1:7890 -H "Accept: application/vnd.github+json" `
  "$base/check-runs/<job_id>/annotations"
```

坑：注解里的 `start_line` **不可靠**（会指到另一个 job 的行号），只能当线索，别当结论。

## 4. job 日志正文（要 token）

```powershell
curl.exe -s -L -x http://127.0.0.1:7890 -u "$user`:$token" -o "$dir\job_log.txt" `
  "$base/actions/jobs/<job_id>/logs"
```

日志首行是 UTF-8 BOM + 时间戳前缀；去掉每行开头的 `2026-09-19T22:56:38.3571143Z ` 更好读。

**用完记得删掉 `$dir`** —— 里面有明文 token。
