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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RctConfig {

    private static final Logger log = LoggerFactory.getLogger("SmoupRCT");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int hubArriveFallbackTicks = 40;
    public int hubSettleTicks = 10;
    public int menuRetryTicks = 20;
    public int menuOpenTimeoutTicks = 100;
    public int menuSettleTicks = 8;
    public int postClickTicks = 30;
    public int iconRetryDelayTicks = 20;
    public int iconNotFoundRetries = 1;
    public int screenChangeTimeoutTicks = 60;
    public int contentStableTicks = 4;
    public int operationTimeoutTicks = 20 * 25;

    public boolean autoTuneTimings = true;
    public boolean debugLog = false;
    public Timings timings = new Timings();
    public int branchClickLimit = 8;

    public List<ModeEntry> modes = new ArrayList<>();
    public Map<String, Map<String, String>> serverBranch = new LinkedHashMap<>();
    public List<String> lastSteps = new ArrayList<>();
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
                    cfg.repair();
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

    private void repair() {
        if (modes == null) modes = new ArrayList<>();
        if (serverBranch == null) serverBranch = new LinkedHashMap<>();
        if (lastSteps == null) lastSteps = new ArrayList<>();
        if (timings == null) timings = new Timings();
        timings.repair();
        modes.removeIf(entry -> entry == null || entry.steps == null || entry.steps.isEmpty());
        for (ModeEntry entry : modes) {
            if (entry.boards == null) entry.boards = new ArrayList<>();
        }
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

    public List<ModeEntry> byBoard(String boardKey) {
        List<ModeEntry> hits = new ArrayList<>();
        if (boardKey == null) return hits;
        for (ModeEntry entry : modes) {
            if (entry.knowsBoard(boardKey)) hits.add(entry);
        }
        return hits;
    }

    public ModeEntry bySteps(List<String> steps) {
        for (ModeEntry entry : modes) {
            if (entry.sameSteps(steps)) return entry;
        }
        return null;
    }

    public void remember(List<String> steps, String boardKey, String number) {
        if (steps == null || steps.isEmpty()) return;

        ModeEntry entry = bySteps(steps);
        if (entry == null) {
            entry = new ModeEntry(steps);
            modes.add(entry);
        }
        entry.addBoard(boardKey);
        dropIncompleteVersionsOf(entry);
        takeBoardOverFromStaleEntries(entry, boardKey);

        lastSteps = new ArrayList<>(steps);
        if (number != null) {
            lastNumber = number;
            putBranch(entry.root(), number, entry.branch());
        }
        save();
    }

    private void dropIncompleteVersionsOf(ModeEntry fresh) {
        modes.removeIf(entry -> entry != fresh && isTailOfTheOther(entry.steps, fresh.steps));
    }

    private static boolean isTailOfTheOther(List<String> first, List<String> second) {
        List<String> shorter = first.size() <= second.size() ? first : second;
        List<String> longer = first.size() <= second.size() ? second : first;
        if (shorter.isEmpty() || shorter.size() == longer.size()) return false;

        int offset = longer.size() - shorter.size();
        for (int i = 0; i < shorter.size(); i++) {
            if (!MenuText.sameName(shorter.get(i), longer.get(offset + i))) return false;
        }
        return true;
    }

    private void takeBoardOverFromStaleEntries(ModeEntry owner, String boardKey) {
        if (boardKey == null) return;
        for (ModeEntry entry : modes) {
            if (entry == owner) continue;
            boolean sameRoute = MenuText.sameName(entry.root(), owner.root())
                    || MenuText.sameName(last(entry.steps), last(owner.steps));
            if (sameRoute) entry.boards.remove(boardKey);
        }
        modes.removeIf(entry -> entry != owner && entry.boards.isEmpty());
    }

    private static String last(List<String> steps) {
        return steps.isEmpty() ? "" : steps.getLast();
    }

    public String branchFor(String root, String number) {
        Map<String, String> branches = serverBranch.get(MenuText.normalize(root));
        return branches == null ? null : branches.get(number);
    }

    public boolean putBranch(String root, String number, String branch) {
        if (number == null || branch == null) return false;
        Map<String, String> branches = serverBranch
                .computeIfAbsent(MenuText.normalize(root), key -> new LinkedHashMap<>());
        return !branch.equals(branches.put(number, branch));
    }

    public List<String> knownBranches(String root) {
        List<String> branches = new ArrayList<>();
        for (ModeEntry entry : modes) {
            String branch = entry.branch();
            if (branch == null || !MenuText.sameName(entry.root(), root)) continue;
            addIfNew(branches, branch);
        }
        Map<String, String> cached = serverBranch.get(MenuText.normalize(root));
        if (cached != null) {
            for (String branch : cached.values()) {
                if (!branch.isEmpty()) addIfNew(branches, branch);
            }
        }
        return branches;
    }

    private static void addIfNew(List<String> names, String name) {
        for (String known : names) {
            if (MenuText.sameName(known, name)) return;
        }
        names.add(name);
    }
}
