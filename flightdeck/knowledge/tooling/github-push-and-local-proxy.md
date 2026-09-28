# ⚠ 推不动 GitHub：先查本机 127.0.0.1:7890 那个系统代理（PowerShell 走它、git 不走）

SUMMARY: 2026-09-28 `git push` 反复失败（`Could not connect to server` / `Proxy CONNECT aborted`），
而同一台机器上 `Invoke-RestMethod` 调 `api.github.com` 一直正常。真凶**不是** DNS 挑被墙 IP
（那是同一天早些时候的错误结论），是**本机装着系统代理**：
`HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings` 里 `ProxyEnable=1`、
`ProxyServer=127.0.0.1:7890`（且真有进程在听）。**PowerShell / `Invoke-*` 尊重这个 WinINET 代理，
而 git 用的 libcurl 完全不理它** —— 所以同一个网络下浏览器与 PowerShell 通、git 不通。
修法一句话：`git -c http.proxy=http://127.0.0.1:7890 -c https.proxy=http://127.0.0.1:7890 push <remote> <branch>`
（实测 3 秒推完 `9a08f10..c14ecbb main -> main`）。
READ WHEN: before 怀疑凭据 / 权限 / DNS —— 只要 `git push`、`git clone`、取 raw 文件连不上 github 就先看这条。
RECHECK WHEN: 代理软件关掉或换端口之后、换机器之后、或哪天 git push 忽然直接就能过。

---

## 排查顺序（按这个来，别跳）

1. **看系统代理**（成本最低、命中率最高）：
   ```powershell
   Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings' -Name ProxyEnable,ProxyServer
   Get-NetTCPConnection -State Listen -LocalPort 7890
   ```
   `ProxyEnable=1` 且端口在听 → 直接拿它当 git 的代理，见上面那条命令。
   顺手看一眼 `Get-NetTCPConnection -State Listen -LocalPort 7890,1080,10809` 找端口。
2. 再看环境变量与 git 自己的配置：`$env:HTTPS_PROXY`、`git config --list | Select-String proxy`。
3. **最后**才怀疑 DNS / IP 被墙（也就是本文后面那套隧道），或者凭据。
   `netsh winhttp show proxy` 显示 Direct 并不能说明没代理 —— WinINET（PowerShell）与 WinHTTP 是两套。

## 为什么同一天会误判成「DNS 挑到了被墙 IP」

因为当时测到：`Resolve-DnsName github.com` → `20.205.243.166`（确实不通），
手工 TCP 连同域其它 IP（`140.82.112.4/113.4/114.4/121.4`、`20.27.177.113`、`4.237.22.34`）
**部分能通**，于是推断成「DNS 解析结果不好」。实际上：

- 那些手工 TCP 测试**有时通有时不通**（同一批 IP 隔十几分钟通断就反了：`140.82.121.4` 先通后不通、
  `140.82.114.4` 反之）——这种抖动本身就是"没走代理"的症状，不是墙的行为。
- PowerShell 的 `Invoke-*` 一直能用（查 CI 从来没断过），这一点当时被当成了"网是好的"的证据，
  其实是"PowerShell 走了代理"的证据。

## 代理软件是随时会开关的：两边都试一次（2026-09-28 当天就来回变了）

同一天里这个代理**先开、后关**：上午 `ProxyEnable=1` + 7890 在听（push 必须带 `-c http.proxy`），
下午用户把它关了（`ProxyEnable=0`、7890 没人听）→ 这时**直连 `git push` 就通了**，
再带 `-c http.proxy=http://127.0.0.1:7890` 反而报
`Failed to connect to github.com port 443 via 127.0.0.1 after 2112 ms`。

所以正确姿势是**先看当前状态再决定**，不要记住「必须带代理」这个结论：

```powershell
Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings' -Name ProxyEnable,ProxyServer
Get-NetTCPConnection -State Listen -LocalPort 7890 -ErrorAction SilentlyContinue
# 端口在听 -> 带 -c http.proxy=...；没人听 -> 直接 git push
```

判断依据是「**那个端口此刻有没有进程在听**」，不是 `ProxyEnable` 的值本身
（这次 `ProxyEnable=1` 时确实在听，但两者理论上可以不同步）。

## 备选：本地 CONNECT 隧道（没有代理时才用）

思路：起一个只监听 `127.0.0.1` 的 TCP 隧道，把 `CONNECT host:443` 转给一个手工测通的 IP。
**两个坑都踩过：**

- **解析 CONNECT 行会把端口解析炸**：请求行是 `CONNECT github.com:443 HTTP/1.1`，
  `line.Substring(8).Trim().Split(':')[1]` 得到的是 `"443 HTTP/1.1"`，`int.Parse` 直接抛
  `FormatException`；异常被 catch 后隧道**默默关掉客户端**，git 那边只报
  `fatal: unable to access ...: Proxy CONNECT aborted`（而且 0.2 秒就返回，像是被立刻拒绝，
  极易误判成"端口没起""防火墙拦了"）。正确写法：`int.Parse(parts[1].Split(' ')[0])`。
- **目标 IP 会漂**：写死一个 IP 的隧道过一会儿就不通了，`TcpClient.Connect` 又没有超时，
  会卡到 TCP 超时（约 21 s/个）。要么把候选 IP 写成列表逐个试，要么别用隧道。

隧道本体（C# 编进 PowerShell，`AcceptTcpClient` **每个连接一条线程**，TLS 端到端透传所以 git 不会抗议证书）：

```powershell
# Add-Type 里定义 GhTunnel.Start(port, string[] hosts) / Handle / Pump
Start-Process powershell.exe -ArgumentList '-NoProfile','-ExecutionPolicy','Bypass','-File',"$env:TEMP\gh_tunnel.ps1" -WindowStyle Hidden
git -c http.proxy=http://127.0.0.1:18081 -c https.proxy=http://127.0.0.1:18081 push <remote> <branch>
# 用完：Get-CimInstance Win32_Process -Filter "Name='powershell.exe'" |
#   Where-Object { $_.CommandLine -like '*gh_tunnel*' -and $_.ProcessId -ne $PID } |
#   ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
```

- `-ne $PID` **不能省**：跑这条命令的 PowerShell 自己命令行里也含 `gh_tunnel` 字样，
  不排除自己就会把当前 shell 杀掉。
- 隧道要**并发**（git 同时开几条连接），单线程接力会把 git 卡死。
- SSH 那条（`ssh://git@ssh.github.com:443`）能连但 `Permission denied (publickey)`，别走 SSH。
