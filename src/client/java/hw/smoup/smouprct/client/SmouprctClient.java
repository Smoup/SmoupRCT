package hw.smoup.smouprct.client;

import hw.smoup.smouprct.mixin.client.BossHealthOverlayAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;

public class SmouprctClient implements ClientModInitializer {

    private static final String COMMAND = ".rct";
    private static final long PVP_CONFIRM_WINDOW_MS = 5000;

    private RctConfig config;
    private RctController controller;

    private String currentNumber;
    private String currentSidebarLine;
    private long pvpConfirmDeadline;

    @Override
    public void onInitializeClient() {
        config = RctConfig.load();
        controller = new RctController(config);
        RctLog.init(config);
        JoinMemory.init(config);

        ClientSendMessageEvents.ALLOW_CHAT.register(this::onChat);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private boolean onChat(String message) {
        String trimmed = message.trim();
        if (!isRctCommand(trimmed)) return true;

        String argument = trimmed.length() > COMMAND.length()
                ? trimmed.substring(COMMAND.length()).trim()
                : "";

        String number = requestedNumber(argument);
        if (number == null) {
            Chat.info(Minecraft.getInstance(), "Формат: .rct или .rct <номер>");
            return false;
        }
        if (!confirmedDuringPvp()) return false;

        controller.start(number, MenuText.boardKey(currentSidebarLine));
        return false;
    }

    private static boolean isRctCommand(String message) {
        return message.equals(COMMAND) || message.toLowerCase().startsWith(COMMAND + " ");
    }

    private String requestedNumber(String argument) {
        if (argument.isEmpty()) {
            return currentNumber != null ? currentNumber : config.lastNumber;
        }
        return argument.matches("\\d+") ? argument : null;
    }

    private boolean confirmedDuringPvp() {
        if (!isPvpBossbarActive(Minecraft.getInstance())) return true;

        long now = System.currentTimeMillis();
        if (now > pvpConfirmDeadline) {
            pvpConfirmDeadline = now + PVP_CONFIRM_WINDOW_MS;
            warnAboutPvp();
            return false;
        }
        pvpConfirmDeadline = 0;
        return true;
    }

    private static void warnAboutPvp() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Component message = Component.literal("⚔ ").withStyle(ChatFormatting.RED)
                .append(Component.literal("PvP").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal(" — повтори ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(COMMAND).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" в течение ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("5 сек").withStyle(ChatFormatting.RED))
                .append(Component.literal(" для перезахода").withStyle(ChatFormatting.GRAY));

        mc.player.displayClientMessage(message, false);
    }

    private void onTick(Minecraft mc) {
        readSidebar(mc);
        JoinMemory.tick(mc, currentSidebarLine);
        controller.tick(mc);
    }

    private static boolean isPvpBossbarActive(Minecraft mc) {
        BossHealthOverlay overlay = mc.gui.getBossOverlay();
        for (LerpingBossEvent event : ((BossHealthOverlayAccessor) overlay).smouprct$getEvents().values()) {
            String name = event.getName().getString().toLowerCase();
            if (name.contains("pvp") || name.contains("пвп")) return true;
        }
        return false;
    }

    private void readSidebar(Minecraft mc) {
        currentSidebarLine = null;
        currentNumber = null;

        for (String line : Compat.sidebarLines(mc)) {
            String number = MenuText.serverNumber(line);
            if (number == null) continue;

            currentSidebarLine = line;
            currentNumber = number;
            return;
        }
    }

}
