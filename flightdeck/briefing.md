# Briefing — lemon_tool

## Conventions

1. 每次代码改动都要提交，不允许留下未提交的改动。
2. 代码保持简洁、易维护。
3. 遇到表述模糊不清的需求，先向我详细提问，不要猜着做。
4. 同一操作失败超过两次（GitHub 访问失败或其他外部／工具报错），停止重试：
   说明问题原因和解决办法，然后立即退出。
5. commit 说明用 `[core]` 或 `[UI]` 加简短改动描述；core 和 UI 都涉及就两个都写，
   例如 `[core][UI] 说明`。
6. 与我的对话、以及 flightdeck 里的文件内容，一律用中文。
7. 如果你认为我说的不对，请直接反驳，并给出你认为正确的做法与理由。
8. 每次更新**只动版本号**（`versionCode` 和 `versionName` 一起动），**不要动包名**——
   `com.lemon.mrs` 的 `namespace` / `applicationId` 保持不变；包名一改就是另一个 app，老用户升级不上。

<!-- 章节标题（Focus / In flight / Next / Open questions / Conventions /
     Subscriptions）保留英文，它们是 flightdeck 协议自己的字段名，正文一律中文。 -->

## Subscriptions

<!-- 每行一个 ~/.flightdeck 相对路径；留空表示不订阅任何全局内容 -->
