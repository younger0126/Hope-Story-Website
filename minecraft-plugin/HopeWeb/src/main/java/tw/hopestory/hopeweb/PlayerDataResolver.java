package tw.hopestory.hopeweb;

import org.bukkit.Statistic;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

public final class PlayerDataResolver {
    private final HopeWebPlugin plugin;
    private boolean placeholderApiPresent;
    private Method setPlaceholdersMethod;

    public PlayerDataResolver(HopeWebPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        Plugin papi = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");
        placeholderApiPresent = papi != null && papi.isEnabled();
        setPlaceholdersMethod = null;
        if (placeholderApiPresent) {
            try {
                Class<?> type = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                setPlaceholdersMethod = type.getMethod("setPlaceholders", Player.class, String.class);
                plugin.getLogger().info("已偵測 PlaceholderAPI。");
            } catch (Exception ex) {
                placeholderApiPresent = false;
            }
        }
    }

    public PlayerSnapshot snapshot(Player player) {
        long level = resolveNumber(player, "player-data.level", 1L);
        long power = resolveNumber(player, "player-data.power", 0L);
        long money = resolveNumber(player, "player-data.money", 0L);
        long island = resolveNumber(player, "player-data.island-level", 1L);
        long ticks;
        try {
            ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        } catch (Exception ex) {
            ticks = 0L;
        }
        return new PlayerSnapshot(player.getUniqueId().toString(), player.getName(), level, power, money, island, ticks / 20L / 60L);
    }

    private long resolveNumber(Player player, String path, long fallback) {
        String placeholder = plugin.getConfig().getString(path + ".placeholder", "");
        if (placeholderApiPresent && setPlaceholdersMethod != null && placeholder != null && !placeholder.isBlank()) {
            try {
                Long value = parseNumber(String.valueOf(setPlaceholdersMethod.invoke(null, player, placeholder)));
                if (value != null) return value;
            } catch (Exception ignored) {
            }
        }

        String skyField = switch (path) {
            case "player-data.level" -> "level";
            case "player-data.power" -> "power";
            case "player-data.money" -> "gold";
            case "player-data.island-level" -> "island";
            default -> null;
        };
        if (skyField != null) {
            Long value = readSkyRpgValue(player, skyField);
            if (value != null) return value;
        }

        String configuredFallback = plugin.getConfig().getString(path + ".fallback", "");
        // RPG 等級絕不再退回 Minecraft 經驗等級，避免網站出現 Lv.3736 之類的錯誤數字。
        if ("minecraft-exp-level".equalsIgnoreCase(configuredFallback)) return fallback;
        Long value = parseNumber(configuredFallback);
        return value == null ? fallback : value;
    }

    private Long readSkyRpgValue(Player player, String fieldName) {
        Plugin skyRpg = plugin.getServer().getPluginManager().getPlugin("SkyRPG");
        if (skyRpg == null || !skyRpg.isEnabled()) return null;

        try {
            if ("island".equals(fieldName)) {
                Method method = skyRpg.getClass().getDeclaredMethod("islandForPlayer", UUID.class);
                method.setAccessible(true);
                return readNumberField(method.invoke(skyRpg, player.getUniqueId()), "level");
            }
            if ("power".equals(fieldName)) {
                Method method = skyRpg.getClass().getDeclaredMethod("stats", Player.class);
                method.setAccessible(true);
                return readNumberField(method.invoke(skyRpg, player), "power");
            }
            Field profilesField = skyRpg.getClass().getDeclaredField("profiles");
            profilesField.setAccessible(true);
            Object profilesObject = profilesField.get(skyRpg);
            if (profilesObject instanceof Map<?, ?> profiles) {
                return readNumberField(profiles.get(player.getUniqueId()), fieldName);
            }
        } catch (ReflectiveOperationException ignored) {
            // 不同 SkyRPG 版本欄位改名時，再從持久化資料讀取。
        }
        return readSkyRpgYaml(skyRpg, player.getUniqueId(), fieldName);
    }

    private Long readSkyRpgYaml(Plugin skyRpg, UUID uuid, String fieldName) {
        try {
            if ("island".equals(fieldName)) {
                File file = new File(skyRpg.getDataFolder(), "data/islands.yml");
                if (!file.isFile()) return null;
                YamlConfiguration islands = YamlConfiguration.loadConfiguration(file);
                String direct = "islands." + uuid + ".level";
                if (islands.contains(direct)) return islands.getLong(direct);
                if (islands.isConfigurationSection("islands")) {
                    for (String owner : islands.getConfigurationSection("islands").getKeys(false)) {
                        if (islands.getStringList("islands." + owner + ".members").contains(uuid.toString())) {
                            return islands.getLong("islands." + owner + ".level", 1L);
                        }
                    }
                }
                return null;
            }

            File file = new File(skyRpg.getDataFolder(), "data/data.yml");
            if (!file.isFile()) file = new File(skyRpg.getDataFolder(), "data.yml");
            if (!file.isFile()) return null;
            YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
            String key = switch (fieldName) {
                case "power" -> "combat-power";
                case "gold" -> "gold";
                default -> "level";
            };
            String path = "players." + uuid + "." + key;
            return data.contains(path) ? data.getLong(path) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Long readNumberField(Object target, String name) throws ReflectiveOperationException {
        if (target == null) return null;
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        Object value = field.get(target);
        return value instanceof Number number ? number.longValue() : null;
    }

    private static Long parseNumber(String value) {
        if (value == null) return null;
        String cleaned = value.replace(",", "").replaceAll("[^0-9.\\-]", "");
        if (cleaned.isBlank() || cleaned.equals("-") || cleaned.equals(".")) return null;
        try {
            return Math.round(Double.parseDouble(cleaned));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public record PlayerSnapshot(String uuid, String playerName, long level, long power, long money, long islandLevel, long playtimeMinutes) {
    }
}
