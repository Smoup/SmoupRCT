package hw.smoup.smouprct.client;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MenuText {

    private static final Pattern SERVER_NUM = Pattern.compile("#\\s*(\\d+)");
    private static final Pattern SECTION_CODE = Pattern.compile("§.");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern WORD_SPLIT = Pattern.compile("[^\\p{L}\\p{N}]+");

    private MenuText() {
    }

    public static String serverNumber(String text) {
        if (text == null) return null;
        Matcher m = SERVER_NUM.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    public static boolean hasNumber(String text, String number) {
        if (text == null || number == null) return false;
        Matcher m = SERVER_NUM.matcher(text);
        while (m.find()) {
            if (m.group(1).equals(number)) return true;
        }
        return false;
    }

    public static String normalize(String text) {
        if (text == null) return "";
        String clean = SECTION_CODE.matcher(text).replaceAll("");
        return SPACES.matcher(clean).replaceAll(" ").trim().toLowerCase();
    }

    public static String boardKey(String sidebarLine) {
        if (sidebarLine == null) return null;
        String withoutNumber = SERVER_NUM.matcher(sidebarLine).replaceAll(" ");
        String key = normalize(withoutNumber);
        return key.isEmpty() ? null : key;
    }

    public static boolean sameName(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    public static boolean matches(String candidate, String wanted) {
        String left = normalize(candidate);
        String right = normalize(wanted);
        if (left.equals(right) || left.contains(right)) return true;

        List<String> tokens = tokens(right);
        if (tokens.isEmpty()) return false;
        for (String token : tokens) {
            if (!left.contains(token)) return false;
        }
        return true;
    }

    public static List<String> tokens(String text) {
        List<String> tokens = new ArrayList<>();
        for (String part : WORD_SPLIT.split(normalize(text))) {
            if (part.length() >= 2) tokens.add(part);
        }
        return tokens;
    }
}
