# GitHub Actions 上编 Android APK（ubuntu runner）

SUMMARY: ubuntu runner **自带 Android SDK**（`/usr/local/lib/android/sdk`，含 cmdline-tools、
platform-tools、licenses 已接受），所以**不要再用 `android-actions/setup-android@v3`**：它默认
`packages: tools platform-tools`，而 `tools` 包已从 SDK 仓库下架，`sdkmanager tools` 报
`Failed to find package 'tools'` 直接把 job 打死。改用自带 sdkmanager 装需要的组件。
READ WHEN: when 改 `.github/workflows/android.yml`，或 CI 报 sdkmanager / NDK / cmake 相关失败时。
RECHECK WHEN: runner 镜像换代（ubuntu-latest 迁移）或 setup-android 出新版本后。

---

## 已验证可用的 job 骨架（2026-09-20 实跑通过）

```yaml
      - name: Install NDK, CMake and platform
        env:
          ANDROID_HOME: /usr/local/lib/android/sdk
          ANDROID_SDK_ROOT: /usr/local/lib/android/sdk
        run: |
          sdkmanager="$(command -v sdkmanager || echo "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager")"
          yes | "$sdkmanager" --licenses > /dev/null || true
          "$sdkmanager" --install "platforms;android-36" "build-tools;36.0.0" \
            "cmake;3.22.1" "ndk;27.0.12077973"
```

- `ndk` 与 `cmake` 版本必须和 `app/build.gradle` 里的 `ndkVersion` / `cmake.version` 一致。
- 没有 `gradlew` 时用 `gradle/actions/setup-gradle@v4` + `gradle-version: '8.13'`，然后直接跑 `gradle`。

## 坑

- `android-actions/setup-android@v3` 会先把预装的 cmdline-tools 换掉，再从 `dl.google.com`
  重下；它失败时**不会**给出有用注解，只能看 job 日志（见
  `knowledge/tooling/github-actions-logs-via-api.md`）。
- actions 报 `Node.js 20 is deprecated`／`被强制跑在 Node.js 24` 只是告警，不影响结果。
- CI 里 `compileSdk` 不够会直接失败：AAR 的 `META-INF/com/android/build/gradle/aar-metadata.properties`
  里 `minCompileSdk` 是硬门槛，本地用 `7z e -so <aar> <该路径>` 就能先查，省一轮 CI。
