# 图标库候选清单（官网 / 许可证 / 怎么进 Android）

SUMMARY: 要给本 App 加「房子」这类图标时，MiuiX 自带的 155 个里**没有 Home / House**，可换的图标集有：
Lucide(ISC)、Tabler(MIT)、Material Symbols / Material Icons(Apache-2.0)、Phosphor(MIT)、Heroicons(MIT)、
Bootstrap Icons(MIT)、Iconoir(MIT)、Feather(MIT)、Iconify(聚合器)、Remix Icon(**自定义许可**，慎用)。
**只有 Google 的 Material icons 有官方 Android 构件**（`androidx.compose.material:material-icons-extended`，
最后一版 1.7.8）；其余都要自己把 SVG 的 `path d` 转成 Compose `ImageVector` 或 vector drawable——
本仓库已有这套手法（见 `miuix-0.8.8.md` 的图标源码解析小节），1~2 个图标**不必引依赖**。
READ WHEN: when 要给界面换/加图标、评估引入某个图标库，或要问「哪个图标库能用在 Compose」时。

---

## 候选（2026-09-24 逐个查证）

官网一律取自 GitHub 仓库的 `homepage` 字段 + `license` 字段，不是凭印象写的：

| 图标集 | 官网 | 许可 | 备注 |
|---|---|---|---|
| Material Symbols / Material Icons（Google） | https://fonts.google.com/icons | Apache-2.0 | 有官方 Android 构件，`Home` 现成 |
| Lucide | https://lucide.dev | **ISC** | 24px 描边；morphicons 的底层就是它 |
| Tabler Icons | https://tabler.io/icons | MIT | 5900+，描边风，更新最勤（2026-09 还在推） |
| Phosphor | https://phosphoricons.com | MIT | 多字重（thin → fill） |
| Heroicons | https://heroicons.com | MIT | Tailwind 出品，outline / solid 各一套 |
| Bootstrap Icons | https://icons.getbootstrap.com | MIT | 2000+ |
| Iconoir | https://iconoir.com | MIT | 1500+，线条感强 |
| Feather | https://feathericons.com | MIT | 老牌，但**已停更**（最后 push 2025-03） |
| Iconify | https://iconify.design | MIT | 不是图标集，是 **150+ 套的聚合器**，可跨库搜、直接取 SVG |
| Remix Icon | https://remixicon.com | ⚠ **Remix Icon License v1.0** | **已不是 Apache-2.0**，商用前读原文 |

## 怎么进 Android（关键结论）

- **只有 Google 的 Material icons 有官方 Android 构件**：
  `androidx.compose.material:material-icons-extended`（Google Maven，**释放到 1.7.8 就冻结了**，142 个版本）
  与 `material-icons-core`。`Home` / `HomeFilled` 这类现成，代价是一整套（R8 可裁未引用的）。
- 其余全都要**自己转**：拿 SVG 的 `path d` → Compose `ImageVector`（Kotlin）或 vector drawable（XML）。
  只换 1~2 个图标时这条路**零新依赖、体积几乎不涨**，比引一整套更合本仓库的体积纪律。
  2026-09-24 已经把「解析图标路径数据」跑通过一遍（MiuiX 155 个图标的全量预览）。

## 坑 / 环境

- **Remix Icon 的许可变了**：仓库里 `License` 首行是 "Remix Icon License v1.0"，GitHub API 报
  `NOASSERTION`。别照旧资料写 Apache-2.0。
- 本机**网络可达性不一致**（2026-09-24 实测）：`lucide.dev`、`phosphoricons.com`、`feathericons.com` 通；
  `tabler.io`、`fonts.google.com`、`heroicons.com`、`icons.getbootstrap.com`、`remixicon.com`、
  `iconify.design` **全部超时**（是网络，不是站点没了）。`api.github.com` 很稳 —— 要「官网 + 许可」
  直接读仓库的 `homepage` / `license` 字段最省事，也别用 marketing 站验证。
- `repo1.maven.org` 的**目录索引与 `maven-metadata.xml` 在本机不稳（超时）**，但下具体 jar 文件可以
  （拉 MiuiX 的 sources jar 成功过）。核 artifact 优先用 Google Maven 的 metadata。
