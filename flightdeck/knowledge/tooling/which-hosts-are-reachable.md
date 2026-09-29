# ⚠ 本机取外网内容：哪些通道通、curl 不走代理、SVG 会被解析成 XmlDocument

SUMMARY: 本机外网通道不齐：**`api.github.com` 稳定可用**（`contents` 端点配
`Accept: application/vnd.github.raw` 能取任意仓库文件原文）；**`raw.githubusercontent.com` 与
`curl.exe` 都不通**（curl 连 443 直接超时约 21 s，它**不走** `Invoke-*` 用的那套系统代理）；
`repo1.maven.org` 下载**具体 jar** 可以，目录索引常超时。坑：`Invoke-RestMethod` 遇到
`image/svg+xml` 会**自动解析成 `XmlDocument`**，取原文要用 `.OuterXml`，此时 `.Length` 是空的，
别据此判断失败——曾经因此误以为 Tabler 图标取不到。要整份源码别逐个 `contents`：`tarball/HEAD` 一个请求就够。

READ WHEN: when 要从外网拉文件（图标 SVG / 源码 / Maven 构件），或 curl、`Invoke-WebRequest`
报连接失败 / 返回空 / `Object reference not set to an instance of an object` 时，
或要一次性拿整份上游源码、或刚加了新依赖要跑 `--offline` 前置时。

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
| `repo.maven.apache.org`（Maven Central 另一个域名，Gradle 默认走它） | ❌ 本机被解析成 `198.18.0.66`（假地址）→ 连接超时；**给 JVM 配上代理即可**（见下） |
| `tabler.io` / `fonts.google.com` / `heroicons.com` / `remixicon.com` / `iconify.design` | ❌ 超时，别试 |

## 代理：为什么 PowerShell 能上、Gradle / curl 不能

- 本机系统代理 = **`127.0.0.1:7890`**（注册表 `HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings`：
  `ProxyEnable=1`、`ProxyServer=127.0.0.1:7890`；`netsh winhttp show proxy` 反而显示 direct，别被它骗了）。
- **只有 PowerShell 的 `Invoke-RestMethod` / `Invoke-WebRequest` 走它**；`curl.exe` 与 JVM（Gradle）
  都不读 WinINET 设置，直连还会拿到 `198.18.0.66` 这种假 DNS 结果。
- 给 Gradle 配代理：写**用户级** `C:\Users\admin\.gradle\gradle.properties`（仓库外，别写进仓库那份）：

  ```
  systemProp.http.proxyHost=127.0.0.1
  systemProp.http.proxyPort=7890
  systemProp.https.proxyHost=127.0.0.1
  systemProp.https.proxyPort=7890
  systemProp.http.nonProxyHosts=localhost|127.*|[::1]
  ```

  配完 `.\gradlew.bat :app:compileDebugKotlin` 就能真正跑通（细节见 `knowledge/build/android-toolchain.md`）。
- 给 curl 配代理：`--proxy http://127.0.0.1:7890`（或设 `HTTPS_PROXY` 环境变量）。
## 取仓库文件的标准姿势

```powershell
$h = @{ 'User-Agent' = 'codex'; 'Accept' = 'application/vnd.github.raw' }
$c = Invoke-RestMethod -Uri "https://api.github.com/repos/<owner>/<repo>/contents/<path>?ref=main" -Headers $h -TimeoutSec 30
```

- 要看目录或统计规模，先走 `git/trees/<branch>?recursive=1`，一次拿到全部 `path` + `size`
  ——比逐个 `contents` 请求便宜得多（用 `Where-Object` 过滤 + `Measure-Object -Sum` 算总量）。
- **XML 都会被自动解析成 `[System.Xml.XmlDocument]`**：`.svg`（content-type `image/svg+xml`）如此，仓库里的 `strings.xml` 也如此 —— 2026-09-29 又栽一次，在 `strings.xml` 里 `Select-String` 找文案一条都搜不到，真因就是它：
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

## 2026-09-30 复测 + 一次拿整份上游源码

- **本地代理 7890 又在跑**（`Test-NetConnection 127.0.0.1 7890` → True），Gradle 靠
  `~/.gradle/gradle.properties` 那四行走代理下构件是通的。上面表里「`repo.maven.apache.org` 被解析成
  `198.18.0.66` 假地址」这次**没复现**（解到 `104.18.19.12`、TLS 握手成功）；`repo1.maven.org` 的
  **目录索引这次也 200**（能列出 `hiddenapibypass-6.1/` 下所有构件）。结论：这两条会随环境变，
  别当成永久不通。
- **一次拿整份上游源码**（比逐个 `contents` 便宜得多）：

  ```powershell
  Invoke-WebRequest -Uri 'https://api.github.com/repos/<owner>/<repo>/tarball/HEAD' `
    -Headers @{'User-Agent'='codex'} -OutFile "$env:TEMP\<repo>.tgz" -TimeoutSec 60
  tar -xzf "$env:TEMP\<repo>.tgz" -C "$env:TEMP\<dir>"     # Windows 自带的 bsdtar
  ```

  坑：① 这个 tar **不支持 `--wildcards`**（`Option --wildcards is not supported`），要么全解、
  要么解完再用 `Select-String` 筛；② 解到符号链接目录会报 `Can't create ... Invalid argument`
  （内核仓库的 `kernel/include/uapi` 就是），**已解出的文件不受影响**，别当成整体失败。
  本次 SukiSU 整仓 20 MB，解完直接 `Select-String -Pattern 'PredictiveBack|OnBackInvoked'` 就能定位。
- **新增 Gradle 依赖后先联网跑一次**（`.\gradlew.bat :app:compileDebugKotlin`，不带 `--offline`），
  把构件灌进 `~/.gradle/caches/modules-2/`；之后那 8 条 `--offline` 前置才过得去 —— `--offline`
  不会去下载，会直接给 `FAILED`。
- `Invoke-WebRequest -TimeoutSec 30` 在本机 PS 5.1 上会直接抛
  `Object reference not set to an instance of an object`（不是超时）：加 `-UseBasicParsing`
  或去掉 `-TimeoutSec` 就好。