package tw.hopestory.hopeweb;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class HopeWebCommand implements CommandExecutor, TabCompleter {
    private final HopeWebPlugin plugin; private final LinkCodeManager linkCodeManager; private final SyncService syncService;
    public HopeWebCommand(HopeWebPlugin plugin, LinkCodeManager linkCodeManager, SyncService syncService) { this.plugin = plugin; this.linkCodeManager = linkCodeManager; this.syncService = syncService; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("hopelink")) {
            if (!(sender instanceof Player p)) { sender.sendMessage(plugin.color(plugin.getConfig().getString("messages.player-only", ""))); return true; }
            if (!p.hasPermission("hopeweb.link")) { plugin.message(p, "messages.no-permission"); return true; }
            if (!plugin.getConfig().getBoolean("link.enabled", true)) { plugin.message(p, "messages.link-disabled"); return true; }
            LinkCodeManager.LinkCode r = linkCodeManager.create(p); String prefix = plugin.color(plugin.getConfig().getString("messages.prefix", ""));
            for (String line : plugin.getConfig().getStringList("messages.link-created")) p.sendMessage(prefix + plugin.color(line.replace("{code}", r.code()).replace("{minutes}", String.valueOf(r.minutes()))));
            return true;
        }
        if (!sender.hasPermission("hopeweb.admin")) { sender.sendMessage(plugin.color("&c你沒有權限。")); return true; }
        if (args.length == 0) { sender.sendMessage(plugin.color("&6HopeWeb &fv" + plugin.getDescription().getVersion() + " &7| /hopeweb reload | sync | status")); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> { plugin.reloadHopeWeb(); sender.sendMessage(plugin.color("&aHopeWeb 設定已重新載入。")); }
            case "sync" -> { syncService.syncNow(); sender.sendMessage(plugin.color("&a已執行同步。")); }
            case "status" -> { sender.sendMessage(plugin.color("&6HopeWeb &7API：" + (plugin.getConfig().getBoolean("api.enabled", false) ? "&a啟用" : "&c停用"))); }
            default -> sender.sendMessage(plugin.color("&c未知子指令。"));
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) { if (!command.getName().equalsIgnoreCase("hopeweb") || args.length != 1) return List.of(); String i = args[0].toLowerCase(Locale.ROOT); List<String> r = new ArrayList<>(); for (String o : List.of("reload", "sync", "status")) if (o.startsWith(i)) r.add(o); return r; }
}
