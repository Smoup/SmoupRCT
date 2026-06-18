package hw.smoup.smouprct.client;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModeRegistry {

    public record Mode(String id, String label, List<String> iconKeywords, boolean hasCategories) {}
    public record Category(String id, String label, String nameKeyword, String lorePrefix) { }

    public record ServerListing(Mode mode, String number, Category category) {
        public String categoryId() {
            return ModeRegistry.categoryId(category);
        }
    }

    public static final String FLAT = "flat";
    private static final String JOIN_HINT = "подключени";

    public static final List<Mode> MODES = List.of(
            new Mode("light120", "Лайт 1.20", List.of("лайт", "1.20"), false),
            new Mode("light116", "Лайт 1.16", List.of("лайт", "1.16"), true),
            new Mode("classic116", "Классика 1.16", List.of("классическ", "1.16"), false)
    );

    public static final List<Category> CATEGORIES = List.of(
            new Category("solo", "Соло", "соло", "Соло"),
            new Category("duo", "Дуо", "дуо", "Дуо"),
            new Category("trio", "Трио", "трио", "Трио"),
            new Category("clan", "Клан", "клан", "Клан")
    );

    private static final Pattern SERVER_NUM = Pattern.compile("#\\s*(\\d+)");

    public static Mode matchByScoreboard(String scoreboardModeText) {
        if (scoreboardModeText == null) return null;
        String t = scoreboardModeText.toLowerCase();
        if (t.contains("классик")) return byId("classic116");
        if (t.contains("лайт")) {
            return categoryByText(t) != null ? byId("light116") : byId("light120");
        }
        return null;
    }

    public static Mode byId(String id) {
        for (Mode m : MODES) {
            if (m.id().equals(id)) return m;
        }
        return null;
    }

    public static Category categoryById(String id) {
        for (Category c : CATEGORIES) {
            if (c.id().equals(id)) return c;
        }
        return null;
    }

    public static Optional<ServerListing> parseServer(String text) {
        if (text == null || !text.toLowerCase().contains(JOIN_HINT)) return Optional.empty();
        String number = serverNumber(text);
        Mode mode = matchByScoreboard(text);
        if (number == null || mode == null) return Optional.empty();
        return Optional.of(new ServerListing(mode, number, categoryByText(text)));
    }

    public static String categoryId(Category category) {
        return category != null ? category.id() : FLAT;
    }

    public static Category categoryByText(String text) {
        if (text == null) return null;
        String t = text.toLowerCase();
        for (Category c : CATEGORIES) {
            if (t.contains(c.lorePrefix().toLowerCase())) return c;
        }
        return null;
    }

    public static String serverNumber(String text) {
        if (text == null) return null;
        Matcher m = SERVER_NUM.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    public static boolean allContained(String haystack, List<String> needles) {
        String h = haystack.toLowerCase();
        for (String n : needles) {
            if (!h.contains(n.toLowerCase())) return false;
        }
        return true;
    }
}
