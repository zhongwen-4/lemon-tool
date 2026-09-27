# 推不动 GitHub 时：多半是 DNS 挑了个被墙的 IP，本地开 CONNECT 隧道绕过去

SUMMARY: 2026-09-28 本机 `git push lemon-tool main`（HTTPS）连不上：
`Failed to connect to github.com port 443 ... Could not connect to server` /
`Recv failure: Connection was reset`。
排查下来**不是墙整个 GitHub**，是 **DNS 把 github.com 解到了一个被墙的 IP**：

- `Resolve-DnsName github.com` → `20.205.243.166`（走不通）
- 手工 TCP 试同域的其它 IP：`140.82.112.4 / .113.4 / .114.4 / .121.4`、`20.27.177.113`、
  `4.237.22.34` **全部 443 通**；`api.github.com`、`codeload.github.com`、`ssh.github.com:443`
  也都通（所以查 CI 一直没问题，只有 push 挂）。
- 顺带排除的：本机没配 git proxy、没设 `HTTP(S)_PROXY`、没监听 7890/1080 之类的本地代理；
  系统 hosts 里没有 github 条目；有 `%USERPROFILE%\.ssh\id_ed25519`，但推到
  `ssh://git@ssh.github.com:443/...` 是 `Permission denied (publickey)`（这把钥匙没在 GitHub 上登记）。
  也**不是**没权限：凭据在 Windows 凭据管理器里（`cmdkey /list` 能看到 `git:https://github.com`，用户 zhongwen-4）。

解法：本地起一个**只监听 127.0.0.1 的 TCP 隧道**，把 `CONNECT github.com:443` 直接怼到一个通的 IP，
让 git 把它当 HTTP 代理用：

```powershell
# 关键点：C# 编译进 PowerShell 进程，AcceptTcpClient 每个连接开一条线程；TLS 是端到端透传，
# 证书校验照旧，所以 git 不会抗议。用 Start-Process -WindowStyle Hidden 起，用完 Stop-Process 杀掉。
git -c http.proxy=http://127.0.0.1:18080 -c https.proxy=http://127.0.0.1:18080 push lemon-tool main
```
实测一次就过：`9919af2..8517d3f main -> main`。

- 隧道要**并发**（git 会同时开几条连接），单线程接力会把 git 卡死。
- 隧道日志里 `err: 无法从传输连接中读取数据 ... WSACancelBlockingCall` 是正常收尾噪音，不用管。
- 用完记得 `Stop-Process`（按 `CommandLine -like '*gh_tunnel*'` 找），并删掉临时 ps1 与日志。
- 下次直接 push 如果是 `Connection was reset`，先按上面这套查一遍 IP，
  别急着怀疑凭据/权限，也别反复重试同一条命令。
READ WHEN: before 在 git push / clone / 取 raw 文件时遇到连接被重置或连不上 github.com。
RECHECK WHEN: 网络环境变了、或 DNS 恢复正常之后（通了就还是直接 push）。