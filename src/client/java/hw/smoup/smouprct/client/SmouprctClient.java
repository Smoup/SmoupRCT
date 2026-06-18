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
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;

public class SmouprctClient implements ClientModInitializer {

    private RctConfig config;
    private RctController controller;

    private String detectedCurrent = null;
    private String detectedMode = null;

    private static final long PVP_CONFIRM_WINDOW_MS = 5000;
    private long pvpConfirmDeadline = 0;

    @Override
    public void onInitializeClient() {
        config = RctConfig.load();
        controller = new RctController(config);
        JoinMemory.init(config);

        ClientSendMessageEvents.ALLOW_CHAT.register(this::onChat);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private boolean onChat(String message) {
        String trimmed = message.trim();
        if (!trimmed.equals(".rct") && !trimmed.toLowerCase().startsWith(".rct ")) {
            return true;
        }

        String arg = trimmed.length() > 4 ? trimmed.substring(4).trim() : "";
        String number;

        if (arg.isEmpty()) {
            number = detectedCurrent != null ? detectedCurrent : config.lastNumber;
        } else if (arg.matches("\\d+")) {
            number = arg;
        } else {
            chat("Формат: .rct или .rct <номер>");
            return false;
        }

        if (!confirmPvp()) {
            return false;
        }

        controller.start(number, detectedMode);
        return false;
    }

    private boolean confirmPvp() {
        if (!isPvpBossbarActive(Minecraft.getInstance())) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now > pvpConfirmDeadline) {
            pvpConfirmDeadline = now + PVP_CONFIRM_WINDOW_MS;
            warnPvp();
            return false;
        }
        pvpConfirmDeadline = 0;
        return true;
    }

    private static void warnPvp() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Component message = Component.literal("⚔ ").withStyle(ChatFormatting.RED)
                .append(Component.literal("PvP").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal(" — повтори ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(".rct").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" в течение ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("5 сек").withStyle(ChatFormatting.RED))
                .append(Component.literal(" для перезахода").withStyle(ChatFormatting.GRAY));

        mc.player.displayClientMessage(message, false);
    }

    private static void chat(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component
                            .literal("[SmoupRCT] ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(
                                    Component
                                            .literal(text)
                                            .withStyle(ChatFormatting.WHITE)
                            ), false);
        }
    }

    private void onTick(Minecraft mc) {
        detectFromScoreboard(mc);
        controller.tick(mc);
    }

    private static boolean isPvpBossbarActive(Minecraft mc) {
        if (mc.gui == null) return false;
        BossHealthOverlay overlay = mc.gui.getBossOverlay();
        if (overlay == null) return false;
        for (LerpingBossEvent event : ((BossHealthOverlayAccessor) overlay).smouprct$getEvents().values()) {
            String name = event.getName().getString().toLowerCase();
            if (name.contains("pvp") || name.contains("пвп")) return true;
        }
        return false;
    }

    private void detectFromScoreboard(Minecraft mc) {
        String foundLine = null;
        String foundNumber = null;

        for (String line : readSidebarLines(mc)) {
            String number = ModeRegistry.serverNumber(line);
            if (number == null) continue;
            ModeRegistry.Mode mode = ModeRegistry.matchByScoreboard(line);
            if (mode == null) continue;

            foundLine = line;
            foundNumber = number;

            // Засеваем кеш: текущий сервер -> его категория (из той же строки).
            ModeRegistry.Category cat = ModeRegistry.categoryByText(line);
            if (cat != null) {
                if (config.putCategory(mode.id(), number, cat.id())) {
                    config.save();
                }
            }
            break;
        }

        detectedMode = foundLine;
        detectedCurrent = foundNumber;
    }

    private static List<String> readSidebarLines(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        if (mc.level == null) return lines;
        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return lines;
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            String owner = entry.owner();
            PlayerTeam team = scoreboard.getPlayersTeam(owner);
            Component formatted = PlayerTeam.formatNameForTeam(team, Component.literal(owner));
            lines.add(formatted.getString());
        }
        return lines;
    }
}
