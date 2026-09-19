# ⚠ 无锚点的 `build/` 会连 deck 的 knowledge/build/ 一起忽略

SUMMARY: `.gitignore` 里的 `build/` 没有前导斜杠时会匹配任意层级的 build 目录，
把 `flightdeck/knowledge/build/` 下的知识文件一起忽略——**文件在、却永远进不了提交**，
属于静默丢失。忽略构建目录一律写成锚定形式 `/build/`。
READ WHEN: when 新增 deck 文件却没出现在 `git status` 里，或要动 `.gitignore` 规则时。
RECHECK WHEN: `.gitignore` 改动之后。

---

## 症状

2026-09-20 提交核心实现那次：`flightdeck/knowledge/build/msvc-utf8-source.md` 明明写好了，
`git add -A` 之后却不在暂存列表里，`git commit` 也没报任何错——差点就这么丢了。

## 定位

```
git check-ignore -v flightdeck/knowledge/build/msvc-utf8-source.md
.gitignore:1:build/	flightdeck/knowledge/build/msvc-utf8-source.md
```

## 修法

忽略规则锚到仓库根：`/build/`、`/app/build/`、`/.gradle/`。改完复验：

- `git check-ignore -v flightdeck/knowledge/build/...` → 应无输出（不再被忽略）
- `git check-ignore -v build/host/mrs.exe` → 仍应命中 `/build/`

## 教训

新增 deck 文件后如果 `git status` 里看不到它，先 `git check-ignore -v <路径>`，
别假定是自己写错了目录。
