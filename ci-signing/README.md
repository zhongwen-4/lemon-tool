# CI 签名钥匙

`mrs-ci.jks` 是 **CI 专用**的固定签名钥匙（alias `mrs`，storepass / keypass 都是 `android`），
2026-09-28 生成，目的只有一个：让「每次 action 出的包」用**同一把钥匙**签名。

## 为什么要有它

分支构建以前是每次 `keytool -genkeypair` 现生成一把随机钥匙，于是**相邻两次构建的签名不一致**：
从最新 action 下回来的 APK 覆盖安装必然失败（`INSTALL_FAILED_UPDATE_INCOMPATIBLE`），只能先卸载。
用户看到的表象就是「装了最新包，界面还是旧的」。

## 用哪一把

`.github/workflows/android.yml` 的选择顺序：

1. 配了 `MRS_KEYSTORE_BASE64`（以及 `MRS_STORE_PASSWORD` / `MRS_KEY_ALIAS` / `MRS_KEY_PASSWORD`）
   → 一律用仓库配置的正式钥匙，分支构建与 tag 发布都用它；
2. 没配 → 用本目录这把 CI 钥匙。

## 注意

这把钥匙是公开的（就在仓库里），任何人都能拿它签一个「能覆盖安装」的 APK，只适合内部自测。
要对外发布，请在仓库里配上上面那四个 secret。**换钥匙的那一次，用户必须先卸载再安装**（签名变了）。