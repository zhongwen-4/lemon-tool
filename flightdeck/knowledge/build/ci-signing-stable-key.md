# ⚠ 「从最新 action 下的包，装上去还是旧界面」：分支构建签名不一致

SUMMARY: 用户 2026-09-28 连着反馈「关于页返回退桌面 / 关于页背景没实现 / 模块检查没实现」——
这三条症状合起来**正好是 0.6.0（code7）的功能集**（BackHandler 是 0.8.0 加的、背景是 0.8.0 加的、
模块检查是 0.7.0 加的），所以根本不是功能缺失，而是**手机上装的就不是新包**。
根因在 CI：`android.yml` 里「从 secret 取正式钥匙」那一步带 `if: startsWith(github.ref, 'refs/tags/v')`，
分支构建走的是另一条 `keytool -genkeypair` **每次现生成一把随机钥匙**的分支 ——
**相邻两次构建签名不同**，从最新 action 下回来的 APK 覆盖安装必然失败
（`INSTALL_FAILED_UPDATE_INCOMPATIBLE`，系统只给一句「应用未安装」，很容易被当成别的问题），
于是手机上留着的还是老版本。修法（2026-09-29，版本 0.9.0/code10 起）：
仓库里放一把公开的固定 CI 钥匙 `ci-signing/mrs-ci.jks`（没配 `MRS_KEYSTORE_BASE64` 时就用它），
分支构建与 tag 发布走同一条取钥匙的路；产物同时改名成 `mrs-<versionName>-c<versionCode>.apk`，
解压后一眼能看出是哪个构建，不会再跟旧包搞混。

要点：

- **诊断口诀**：用户说「我从最新 action 下的」+ 症状像某个旧版本 → 先让他在**主页看「应用版本」**，
  对不上就是没装上去，别急着改代码。
- 换钥匙（第一次从随机钥匙切到固定钥匙、或切到正式 secret）**必须让用户先卸载再装**一次。
- `ci-signing/mrs-ci.jks` 是公开的（就躺在仓库里），只适合内部自测；对外发布请在仓库里配上
  `MRS_KEYSTORE_BASE64` / `MRS_STORE_PASSWORD` / `MRS_KEY_ALIAS` / `MRS_KEY_PASSWORD` 四个 secret。
- 顺带记一笔：**artifact 名带版本号**之后，`publish` job 的 `download-artifact` 只留 `path`、不按名字挑，
  以后改产物名不必再改那里。

READ WHEN: when 用户反馈「装了你给的最新包，界面 / 功能还是老的」，或要改 `.github/workflows/android.yml` 的签名 / 产物步骤时。