package tw.hopestory.hopeweb;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.time.Instant;

public final class SyncService {
    private final HopeWebPlugin plugin; private final ApiClient apiClient; private final PlayerDataResolver resolver;
    public SyncService(HopeWebPlugin plugin, ApiClient apiClient, PlayerDataResolver resolver) { this.plugin = plugin; this.apiClient = apiClient; this.resolver = resolver; }
    public void syncNow() {
        if (!apiClient.enabled()) return;
        String serverId = plugin.getConfig().getString("server.id", "hope-story-main"); long now = Instant.now().toEpochMilli(); double tps = 20.0D; try { tps = Math.min(20.0D, Bukkit.getTPS()[0]); } catch (Throwable ignored) {}
        String statusJson = "{" + "\"serverId\":" + ApiClient.quote(serverId) + ",\"serverName\":" + ApiClient.quote(plugin.getConfig().getString("server.name", "Hope Story 希望物語")) + ",\"online\":true,\"onlinePlayers\":" + Bukkit.getOnlinePlayers().size() + ",\"maxPlayers\":" + Bukkit.getMaxPlayers() + ",\"tps\":" + String.format(java.util.Locale.US, "%.2f", tps) + ",\"version\":" + ApiClient.quote(plugin.getConfig().getString("server.display-version", "1.20.1+")) + ",\"ip\":" + ApiClient.quote(plugin.getConfig().getString("server.public-ip", "")) + ",\"timestamp\":" + now + "}";
        apiClient.postJson(plugin.getConfig().getString("api.endpoints.server-status", "/api/minecraft/server-status"), statusJson);
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerDataResolver.PlayerSnapshot s = resolver.snapshot(p);
            String json = "{" + "\"serverId\":" + ApiClient.quote(serverId) + ",\"uuid\":" + ApiClient.quote(s.uuid()) + ",\"playerName\":" + ApiClient.quote(s.playerName()) + ",\"online\":true,\"level\":" + s.level() + ",\"power\":" + s.power() + ",\"money\":" + s.money() + ",\"islandLevel\":" + s.islandLevel() + ",\"playtimeMinutes\":" + s.playtimeMinutes() + ",\"timestamp\":" + now + "}";
            apiClient.postJson(plugin.getConfig().getString("api.endpoints.player-sync", "/api/minecraft/player-sync"), json);
        }
    }
}
