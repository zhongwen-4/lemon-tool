# ⚠ 本机取外网内容：哪些通道通、curl 不走代理、SVG 会被解析成 XmlDocument

SUMMARY: 本机外网通道不齐：**`api.github.com` 稳定可用**（`contents` 端点配
`Accept: application/vnd.github.raw` 能取任意仓库文件原文）；**`raw.githubusercontent.com` 与
`curl.exe` 都不通**（curl 连 443 直接超时约 21 s，它**不走** `Invoke-*` 用的那套系统代理）；
`repo1.maven.org` 下载**具体 jar** 可以，目录索引常超时。坑：`Invoke-RestMethod` 遇到
`image/svg+xml` 会**自动解析成 `XmlDocument`**，取原文要用 `.OuterXml`，此时 `.Length` 是空的，
别据此判断失败——曾经因此误以为 Tabler 图标取不到。

READ WHEN: when 要从外网拉文件（图标 SVG / 源码 / Maven 构件），或 curl、`Invoke-WebRequest`
报连接失败 / 返回空 / `Object reference not set to an instance of an object` 时。

RECHECK WHEN: 换网络环境、换机器、或给 curl 配了代理之后。

---

## 通道现状（2026-09-25 实测）

| 目标 | 结果 |
| --- | --- |
| `api.github.com`（REST） | ✅ 稳：`repos/<o>/<r>`、`git/trees/<b>?recursive=1`、`contents/<path>?ref=<b>` 全通 |
| `raw.githubusercontent.com` | ❌ 取不到内容（要么 404，要么空体） |
| `curl.exe` 直连任意 https | ❌ `Failed to connect to api.github.com port 443 after 21051 ms` —— curl 不用系统代理 |
| `repo1.maven.org` 的具体 jar | ✅ 可下（`miuix-android-0.8.8-sources.jar` 199 KB 秒下） |
| `repo1.maven.org` 目录索引 | ⚠ 常超时 |
| `tabler.io` / `fonts.google.com` / `heroicons.com` / `remixicon.com` / `iconify.design` | ❌ 超时，别试 |

## 取仓库文件的标准姿势

```powershell
$h = @{ 'User-Agent' = 'codex'; 'Accept' = 'application/vnd.github.raw' }
$c = Invoke-RestMethod -Uri "https://api.github.com/repos/<owner>/<repo>/contents/<path>?ref=main" -Headers $h -TimeoutSec 30
```

- 要看目录或统计规模，先走 `git/trees/<branch>?recursive=1`，一次拿到全部 `path` + `size`
  ——比逐个 `contents` 请求便宜得多（用 `Where-Object` 过滤 + `Measure-Object -Sum` 算总量）。
- **`.svg` 返回的是 `[System.Xml.XmlDocument]`**（响应 content-type = `image/svg+xml`）：
  用 `$c.OuterXml` 取原文。`$c.Length` 为空是正常的，`.GetType().FullName` 一看便知。
- 文本文件（`.kt` / `.toml` / `LICENSE`）用上面这段直接拿到字符串。
- 二进制（png / jar）走 `Invoke-WebRequest -Uri ... -OutFile <每次新建的唯一路径>`；别用 curl，
  也别在拼出来的路径上做 `Remove-Item`（见 `powershell-aliases-and-cmdlets.md`）。

## 推论

- 批量抓 Tabler / Lucide 的图标 SVG 完全可行，例如
  `api.github.com/repos/tabler/tabler-icons/contents/icons/outline/<name>.svg?ref=main`。
- 想核对库的真实签名 / 是否含某 API：下 sources jar →
  `[System.IO.Compression.ZipFile]::OpenRead($jar)` → 遍历 `$z.Entries` 并按名字过滤，
  比翻文档页可靠。