package hw.smoup.smouprct.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public final class RctMessages {

    private RctMessages() {
    }

    public static Component unknownMode() {
        return red("Не знаю, на каком ты режиме.")
                .append(learnHint());
    }

    public static Component ambiguousMode(List<ModeEntry> candidates) {
        MutableComponent message = red("Не понял, на каком ты режиме — подходит несколько:");
        for (ModeEntry candidate : candidates) {
            message.append(grey("\n• "))
                    .append(gold(String.join(" → ", candidate.steps)));
        }
        return message
                .append(grey("\nЗайди сам через "))
                .append(menuCommand())
                .append(grey(" — запомню, какой из них твой."));
    }

    public static Component serverNotFound(String number, String mode) {
        return red("Не нашёл сервер ")
                .append(gold("#" + number))
                .append(red(" в "))
                .append(gold(mode))
                .append(red("."))
                .append(learnHint());
    }

    public static Component modeIconNotFound(String mode) {
        return red("Не нашёл иконку режима ")
                .append(gold(mode))
                .append(red(" в "))
                .append(menuCommand())
                .append(red("."))
                .append(grey("\nЕсли меню просто не успевает прогрузиться, мод сам увеличит выдержку"))
                .append(grey(" — попробуй ещё раз."));
    }

    private static MutableComponent learnHint() {
        return grey("\nЗайди на него сам через ")
                .append(menuCommand())
                .append(grey(" — я запомню дорогу и дальше буду ходить сам."));
    }

    private static MutableComponent red(String text) {
        return Component.literal(text).withStyle(ChatFormatting.RED);
    }

    private static MutableComponent grey(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    private static MutableComponent gold(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD);
    }

    private static MutableComponent menuCommand() {
        return Component.literal("/menu").withStyle(ChatFormatting.YELLOW);
    }
}
