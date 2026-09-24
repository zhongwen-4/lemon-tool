# app 内更新检查：以 GitHub Release 为数据源

SUMMARY: 版本号比 `releases/latest` 的 `tag_name`（本地逐段比数字）；CI 在推 `v*` tag 时用 `gh` 自动发
Release 并挂上 APK —— 用 ubuntu runner 自带的 `gh`，不引第三方 action。
READ WHEN: when 要动版本号/发版、要改「检查更新」、或要查「为什么 app 说检查失败」时。

---

## 数据源与判定

- app 侧：`GET https://api.github.com/repos/zhongwen-4/lemon-tool/releases/latest`，取 `tag_name`（去掉 `v`）
  与 `html_url`；已安装版本用 `PackageManager.getPackageInfo().versionName`（不是 BuildConfig，
  本项目 `buildConfig false`）。逐段比数字，非数字段当 0：`0.2.0 > 0.1.0`。
- 只用 `HttpURLConnection` + `org.json`，不引 OkHttp/Retrofit：这套依赖已经在了，省 APK 体积。
  超时各 8 秒，跑在 `Dispatchers.IO`。
- 401/403/404 一律落到 `UpdateResult.Failed`，界面直接显示「检查失败：HTTP xxx」。
- 仓库**没有 Release 时 API 返回 404** → 界面会说检查失败。所以这个功能上线前必须先发一个版本。

## 发版流程（CI 已接好）

1. 改 `app/build.gradle` 的 `versionCode`/`versionName`（两个都动，Android 只认 versionCode）。
2. 推 main，等 CI 绿。
3. `git tag -a v0.2.0 -m "..."` + `git push lemon-tool v0.2.0`。
4. tag 触发同一个 workflow：`apk` job 里 `permissions: contents: write` + 最后一步
   `if: startsWith(github.ref, 'refs/tags/v')` 时 `gh release create "$GITHUB_REF_NAME" <apk> --title ... --notes ...`。
   runner 自带 `gh`，`GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}` 就够。

## 坑

- **`git tag v0.2.0` 不带 `-m` 会直接失败**：本机没有 EDITOR，git 报 `Terminal is dumb, but EDITOR unset`，
  于是 tag 根本没建（`git push` 紧接着报 `src refspec v0.2.0 does not match any`）。用 `-a -m` 建注释 tag。
- 别把 `permissions: contents: write` 写在 workflow 顶层：那会顺带改写其它 job 的权限；写在需要的 job 上。
- app 的「关于」页原文写着「不联网」，加了更新检查后必须改成「扫描全程离线，只有检查更新联网」——
  否则是对用户的假陈述。
- 国内网络访问 `api.github.com` 常常不通，界面只能报「检查失败」。要真给国内用户用，得考虑镜像回退
  （比如 jsDelivr 读仓库里的 `version.json`），目前**没做**。
- **CI 每次跑都新生成签名密钥**：workflow 里 `keytool -genkeypair -keystore "$RUNNER_TEMP/release.jks"`，
  而 runner 的临时目录每个 run 都是新的（没有任何缓存）→ **同一个 app，不同 run 产出的 APK 签名不同，
  覆盖安装会 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`**，只能卸掉再装。也就是说「更新」这件事上，
  **光提版本号不够，密钥必须先固定**（把 keystore 存成 repo secret，base64 解到 runner 再用；
  本地则固定一份 `.jks`）。顺带：Release note 里那句「签名与 CI 一致」是**假陈述**，要一起改掉。
- 与上一条配套的约定（用户 2026-09-24 立的，已记入 `flightdeck/briefing.md`）：更新时**只提版本号，
  不动包名**。包名（`namespace` / `applicationId`）一改就是另一个 app。

## 首次落地（2026-09-21）

- 0.1.0 → 0.2.0（versionCode 1 → 2）；tag `v0.2.0` 触发 run `35539547477`，发布 Release
  `https://github.com/zhongwen-4/lemon-tool/releases/tag/v0.2.0`，附 `app-release.apk`（1.24 MiB）。
