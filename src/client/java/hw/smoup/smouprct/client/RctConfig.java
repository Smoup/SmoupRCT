package hw.smoup.smouprct.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class RctConfig {

    private static final Logger log = LoggerFactory.getLogger("SmoupRCT");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int hubArriveFallbackTicks = 40;
    public int menuRetryTicks = 4;
    public int menuOpenTimeoutTicks = 100;
    public int menuSettleTicks = 8;
    public int postClickTicks = 30;
    public int iconRetryDelayTicks = 20;
    public int iconNotFoundRetries = 1;

    public Map<String, Map<String, String>> serverCategory = new HashMap<>();

    public String lastModeId = null;
    public String lastNumber = null;

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("smouprct.json");
    }

    public static RctConfig load() {
        Path path = configPath();
        try {
            if (Files.exists(path)) {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                RctConfig cfg = GSON.fromJson(json, RctConfig.class);
                if (cfg != null) {
                    if (cfg.serverCategory == null) cfg.serverCategory = new HashMap<>();
                    return cfg;
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось прочитать конфиг, использую значения по умолчанию", e);
        }
        RctConfig cfg = new RctConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Не удалось сохранить конфиг", e);
        }
    }

    public String getCategory(String modeId, String number) {
        Map<String, String> m = serverCategory.get(modeId);
        return m == null ? null : m.get(number);
    }

    public boolean putCategory(String modeId, String number, String categoryId) {
        Map<String, String> m = serverCategory.computeIfAbsent(modeId, k -> new HashMap<>());
        return !categoryId.equals(m.put(number, categoryId));
    }

    public void setLastJoined(String modeId, String number) {
        if (modeId.equals(lastModeId) && number.equals(lastNumber)) return;
        lastModeId = modeId;
        lastNumber = number;
        save();
    }
}
