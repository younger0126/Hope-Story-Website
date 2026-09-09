package tw.hopestory.hopeweb;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;

public final class PlayerDataResolver {
    private final HopeWebPlugin plugin; private boolean placeholderApiPresent; private Method setPlaceholdersMethod;
    public PlayerDataResolver(HopeWebPlugin plugin) { this.plugin = plugin; reload(); }
    public void reload() {
        Plugin papi = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI"); placeholderApiPresent = papi != null && papi.isEnabled(); setPlaceholdersMethod = null;
        if (placeholderApiPresent) try { Class<?> c = Class.forName("me.clip.placeholderapi.PlaceholderAPI"); setPlaceholdersMethod = c.getMethod("setPlaceholders", Player.class, String.class); plugin.getLogger().info("已偵測 PlaceholderAPI。"); } catch (Exception ex) { placeholderApiPresent = false; }
    }
    public PlayerSnapshot snapshot(Player p) {
        long level = resolveNumber(p, "player-data.level", p.getLevel()), power = resolveNumber(p, "player-data.power", 0L), money = resolveNumber(p, "player-data.money", 0L), island = resolveNumber(p, "player-data.island-level", 1L);
        long ticks; try { ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE); } catch (Exception ex) { ticks = 0L; }
        return new PlayerSnapshot(p.getUniqueId().toString(), p.getName(), level, power, money, island, ticks / 20L / 60L);
    }
    private long resolveNumber(Player p, String path, long fallback) {
        String ph = plugin.getConfig().getString(path + ".placeholder", ""), fb = plugin.getConfig().getString(path + ".fallback", "");
        if (placeholderApiPresent && setPlaceholdersMethod != null && ph != null && !ph.isBlank()) try { Long v = parseNumber(String.valueOf(setPlaceholdersMethod.invoke(null, p, ph))); if (v != null) return v; } catch (Exception ignored) {}
        if ("minecraft-exp-level".equalsIgnoreCase(fb)) return p.getLevel(); Long v = parseNumber(fb); return v == null ? fallback : v;
    }
    private static Long parseNumber(String v) { if (v == null) return null; String c = v.replace(",", "").replaceAll("[^0-9.\\-]", ""); if (c.isBlank() || c.equals("-") || c.equals(".")) return null; try { return Math.round(Double.parseDouble(c)); } catch (NumberFormatException ex) { return null; } }
    public record PlayerSnapshot(String uuid, String playerName, long level, long power, long money, long islandLevel, long playtimeMinutes) {}
}
