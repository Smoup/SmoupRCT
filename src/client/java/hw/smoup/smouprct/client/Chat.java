package hw.smoup.smouprct.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Chat {

    private static final String PREFIX = "[SmoupRCT] ";

    private Chat() {
    }

    public static void error(Minecraft mc, String text) {
        error(mc, red(text));
    }

    public static MutableComponent red(String text) {
        return Component.literal(text).withStyle(ChatFormatting.RED);
    }

    public static void error(Minecraft mc, Component body) {
        send(mc, body);
    }

    public static void info(Minecraft mc, String text) {
        send(mc, Component.literal(text).withStyle(ChatFormatting.WHITE));
    }

    public static void send(Minecraft mc, Component body) {
        if (mc.player == null) return;
        Component message = Component.literal(PREFIX).withStyle(ChatFormatting.GOLD).append(body);
        mc.player.displayClientMessage(message, false);
    }
}
