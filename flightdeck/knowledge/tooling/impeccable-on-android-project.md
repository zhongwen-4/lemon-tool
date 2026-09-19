# impeccable 在本机怎么调（Android 原生项目）

SUMMARY: impeccable skill 装在全局 `C:\Users\admin\.agents\skills\impeccable`，**项目里没有
`.agents/`**，所以脚本一律走全局绝对路径；本机 node 在 `D:\node\node.exe`（已在 PATH）。
本项目平台是 **android 原生**（Compose/MiuiX），因此 `detect.mjs` 与 `live` 命令都不适用
（浏览器覆盖层、HTML 规则引擎只对 web），`audit`/`adapt` 要读 `reference/*.native.md` 变体。
READ WHEN: before 用 impeccable 的 init/critique/audit/polish 等命令改本项目界面时。
RECHECK WHEN: 项目里生成 PRODUCT.md / DESIGN.md 之后，或把 `.agents` 复制进项目之后。

---

## 调用方式（cwd 保持在 `D:\lemon_tool`）

```powershell
node "C:\Users\admin\.agents\skills\impeccable\scripts\context.mjs"
node "C:\Users\admin\.agents\skills\impeccable\scripts\context-signals.mjs"
```

## 2026-09-20 首次探测结果

- `context.mjs` → `NO_PRODUCT_MD`（没有 PRODUCT.md，也就没有 DESIGN.md）。
- signals：`hasProduct=false`、`hasDesign=false`、`hasCode=true`、`register=null`、`platform=null`、
  `critique.latest=null`、`devServer.running=false`、`scan.targets=["app"]`（via source-dir）。
- 结论：register 应为 **product**（工具类 App，设计服务于功能），platform 应按 **android** 处理，
  即读 `reference/product.md` + `reference/android.md`；没有 PRODUCT.md 就轮到 `init` 先补。

## 界面代码在哪

- `app/src/main/java/com/lemon/mrs/ScanUi.kt` —— 主界面（MiuiX 组件）
- `app/src/main/java/com/lemon/mrs/MainActivity.kt` —— 入口与 SAF 选包
- 改界面之前先读 `knowledge/android/miuix-0.8.8.md`（组件签名与硬约束）
