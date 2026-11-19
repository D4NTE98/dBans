package pl.d4nte.bans.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DiscordWebhook {

    private final String url;

    public DiscordWebhook(JavaPlugin plugin) {
        FileConfiguration cfg = plugin.getConfig();
        this.url = cfg.getString("discord.webhook_url", "");
    }

    public void sendEmbed(String title, String description, int color, Map<String,String> fields) {
        if (url == null || url.isEmpty()) return;
        try {
            URL u = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) u.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            StringBuilder fieldsJson = new StringBuilder("[");
            boolean first = true;
            for (Map.Entry<String,String> e : fields.entrySet()) {
                if (!first) fieldsJson.append(",");
                first = false;
                fieldsJson.append("{\"name\":\"").append(escape(e.getKey())).append("\",\"value\":\"")
                        .append(escape(e.getValue())).append("\",\"inline\":true}");
            }
            fieldsJson.append("]");

            String json = "{"
                    + "\"embeds\":[{"
                    + "\"title\":\"" + escape(title) + "\","
                    + "\"description\":\"" + escape(description) + "\","
                    + "\"color\":" + color + ","
                    + "\"fields\":" + fieldsJson.toString()
                    + "}]"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            conn.getInputStream().close();
            conn.disconnect();
        } catch (Exception ignored) {}
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    public static Map<String,String> map(String... kv) {
        Map<String,String> m = new HashMap<>();
        for (int i = 0; i+1 < kv.length; i+=2) m.put(kv[i], kv[i+1]);
        return m;
    }
}