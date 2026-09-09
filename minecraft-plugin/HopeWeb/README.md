# HopeWeb v0.1.0

Hope Story 希望物語 Minecraft 1.20.1 伺服器 ↔ 官方網站橋接插件。

## 功能
- `/hopelink` 產生 6 位數網站綁定碼（預設 5 分鐘失效）
- `/hopeweb reload`
- `/hopeweb sync`
- `/hopeweb status`
- 同步 ONLINE、玩家數、TPS、版本、IP
- 同步 UUID、名稱、等級、戰力、金幣、空島等級、遊玩時間
- 可透過 PlaceholderAPI 自訂 RPG / 經濟 / 空島數值
- API Bearer Token

## 適用
Paper / Purpur 1.20.1，Java 17+

## 編譯
`mvn clean package`

輸出：`target/HopeWeb-0.1.0.jar`

## API
- POST `/api/minecraft/server-status`
- POST `/api/minecraft/player-sync`
- POST `/api/minecraft/link-code`

API 還沒完成前，保持 `api.enabled: false` 即可先裝插件測試 `/hopelink`。
