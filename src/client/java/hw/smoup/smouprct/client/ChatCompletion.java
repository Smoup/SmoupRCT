package hw.smoup.smouprct.client;

import java.util.List;

public final class ChatCompletion {

    private static final List<String> COMMANDS = List.of(".rct");

    public static List<String> matching(String input) {
        String typed = input.toLowerCase();
        return COMMANDS.stream()
                .filter(command -> command.startsWith(typed) && !command.equals(typed))
                .toList();
    }
}
