package tw.hopestory.hopeweb;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.logging.Level;

public final class LinkCodeManager {
    private final HopeWebPlugin plugin; private final ApiClient apiClient; private final SecureRandom random = new SecureRandom(); private final File file; private final YamlConfiguration data;
    public LinkCodeManager(HopeWebPlugin plugin, ApiClient apiClient) { this.plugin = plugin; this.apiClient = apiClient; file = new File(plugin.getDataFolder(), "link-codes.yml"); data = YamlConfiguration.loadConfiguration(file); }
    public LinkCode create(Player player) {
        int length = Math.max(4, Math.min(9, plugin.getConfig().getInt("link.code-length", 6))); int minutes = Math.max(1, plugin.getConfig().getInt("link.expire-minutes", 5));
        String code = generateNumericCode(length); long expiresAt = Instant.now().plusSeconds(minutes * 60L).toEpochMilli(); String key = "codes." + code;
        data.set(key + ".uuid", player.getUniqueId().toString()); data.set(key + ".player", player.getName()); data.set(key + ".expires-at", expiresAt); data.set(key + ".used", false); save();
        if (apiClient.enabled()) {
            String endpoint = plugin.getConfig().getString("api.endpoints.link-code", "/api/minecraft/link-code");
            String json = "{" + "\"serverId\":" + ApiClient.quote(plugin.getConfig().getString("server.id", "hope-story-main")) + ",\"uuid\":" + ApiClient.quote(player.getUniqueId().toString()) + ",\"playerName\":" + ApiClient.quote(player.getName()) + ",\"code\":" + ApiClient.quote(code) + ",\"expiresAt\":" + expiresAt + "}";
            apiClient.postJson(endpoint, json);
        }
        return new LinkCode(code, minutes, expiresAt);
    }
    private String generateNumericCode(int length) { int bound = (int)Math.pow(10, length); int floor = (int)Math.pow(10, length - 1); return String.valueOf(floor + random.nextInt(bound - floor)); }
    private void save() { try { if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs(); data.save(file); } catch (IOException ex) { plugin.getLogger().log(Level.SEVERE, "無法儲存 link-codes.yml", ex); } }
    public record LinkCode(String code, int minutes, long expiresAt) {}
}
