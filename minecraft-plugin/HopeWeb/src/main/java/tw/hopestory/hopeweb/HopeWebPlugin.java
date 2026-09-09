package tw.hopestory.hopeweb;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;
import java.util.logging.Level;

public final class HopeWebPlugin extends JavaPlugin {
    private ApiClient apiClient;
    private LinkCodeManager linkCodeManager;
    private PlayerDataResolver dataResolver;
    private SyncService syncService;
    private int syncTaskId = -1;

    @Override public void onEnable() {
        saveDefaultConfig();
        apiClient = new ApiClient(this);
        linkCodeManager = new LinkCodeManager(this, apiClient);
        dataResolver = new PlayerDataResolver(this);
        syncService = new SyncService(this, apiClient, dataResolver);
        HopeWebCommand command = new HopeWebCommand(this, linkCodeManager, syncService);
        PluginCommand hopeLink = Objects.requireNonNull(getCommand("hopelink")); hopeLink.setExecutor(command);
        PluginCommand hopeWeb = Objects.requireNonNull(getCommand("hopeweb")); hopeWeb.setExecutor(command); hopeWeb.setTabCompleter(command);
        startSyncTask();
        getLogger().info("HopeWeb v" + getDescription().getVersion() + " 已啟用。");
        if (!getConfig().getBoolean("api.enabled", false)) getLogger().warning("API 同步目前為停用狀態。設定好網站 API 後，將 api.enabled 改為 true。");
    }

    @Override public void onDisable() { if (syncTaskId != -1) Bukkit.getScheduler().cancelTask(syncTaskId); }

    public void reloadHopeWeb() {
        reloadConfig(); apiClient.reload(); dataResolver.reload();
        if (syncTaskId != -1) Bukkit.getScheduler().cancelTask(syncTaskId);
        startSyncTask();
    }

    private void startSyncTask() {
        long seconds = Math.max(10L, getConfig().getLong("api.sync-interval-seconds", 30L));
        syncTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            try { syncService.syncNow(); } catch (Exception ex) { getLogger().log(Level.WARNING, "同步排程發生錯誤", ex); }
        }, 60L, seconds * 20L);
    }

    public String color(String input) { return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input); }
    public void message(Player player, String path) { player.sendMessage(color(getConfig().getString("messages.prefix", "")) + color(getConfig().getString(path, ""))); }
}
