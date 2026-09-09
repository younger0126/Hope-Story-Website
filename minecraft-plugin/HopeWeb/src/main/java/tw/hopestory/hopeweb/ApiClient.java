package tw.hopestory.hopeweb;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;

public final class ApiClient {
    private final HopeWebPlugin plugin; private HttpClient client; private String baseUrl; private String token; private int timeoutSeconds;
    public ApiClient(HopeWebPlugin plugin) { this.plugin = plugin; reload(); }
    public void reload() {
        baseUrl = stripTrailingSlash(plugin.getConfig().getString("api.base-url", "")); token = plugin.getConfig().getString("api.token", "");
        timeoutSeconds = Math.max(2, plugin.getConfig().getInt("api.connect-timeout-seconds", 5));
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
    }
    public boolean enabled() { return plugin.getConfig().getBoolean("api.enabled", false) && baseUrl.startsWith("http"); }
    public void postJson(String endpoint, String json) {
        if (!enabled()) return;
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(baseUrl + normalizeEndpoint(endpoint))).timeout(Duration.ofSeconds(timeoutSeconds + 3L)).header("Content-Type", "application/json; charset=utf-8").header("User-Agent", "HopeWeb/" + plugin.getDescription().getVersion()).POST(HttpRequest.BodyPublishers.ofString(json));
        if (token != null && !token.isBlank() && !"CHANGE-ME".equals(token)) b.header("Authorization", "Bearer " + token);
        client.sendAsync(b.build(), HttpResponse.BodyHandlers.ofString()).thenAccept(r -> { if (r.statusCode() < 200 || r.statusCode() >= 300) plugin.getLogger().warning("API 回傳 HTTP " + r.statusCode()); }).exceptionally(ex -> { plugin.getLogger().log(Level.WARNING, "無法連線 Hope Story API：" + ex.getMessage()); return null; });
    }
    public static String quote(String v) { if (v == null) return "null"; return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""; }
    private static String stripTrailingSlash(String v) { if (v == null) return ""; while (v.endsWith("/")) v = v.substring(0, v.length()-1); return v; }
    private static String normalizeEndpoint(String e) { if (e == null || e.isBlank()) return "/"; return e.startsWith("/") ? e : "/" + e; }
}
