package hw.smoup.smouprct.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
//?}

//? if >=1.20.2 {
import net.minecraft.world.scores.DisplaySlot;
//?}

//? if >=1.20.3 {
import net.minecraft.world.scores.PlayerScoreEntry;
//?}

//? if <1.20.3 {
/*import net.minecraft.world.scores.Score;
*///?}

//? if <1.20.5 {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
*///?}

import java.util.ArrayList;
import java.util.List;

public final class Compat {

    private Compat() {
    }

    public static List<String> loreLines(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        //? if >=1.20.5 {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return lines;

        for (Component line : lore.lines()) {
            lines.add(line.getString());
        }
        //?} else {
        /*CompoundTag display = stack.getTagElement("display");
        if (display == null) return lines;

        ListTag lore = display.getList("Lore", Tag.TAG_STRING);
        for (int i = 0; i < lore.size(); i++) {
            Component line = Component.Serializer.fromJson(lore.getString(i));
            if (line != null) lines.add(line.getString());
        }
        *///?}
        return lines;
    }

    public static List<String> sidebarLines(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        if (mc.level == null) return lines;

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective sidebar = sidebarObjective(scoreboard);
        if (sidebar == null) return lines;

        //? if >=1.20.3 {
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
            lines.add(ownerLine(scoreboard, entry.owner()));
        }
        //?} else {
        /*for (Score score : scoreboard.getPlayerScores(sidebar)) {
            lines.add(ownerLine(scoreboard, score.getOwner()));
        }
        *///?}
        return lines;
    }

    private static Objective sidebarObjective(Scoreboard scoreboard) {
        //? if >=1.20.2 {
        return scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        //?} else {
        /*return scoreboard.getDisplayObjective(SIDEBAR_SLOT);
        *///?}
    }

    private static String ownerLine(Scoreboard scoreboard, String owner) {
        PlayerTeam team = scoreboard.getPlayersTeam(owner);
        return PlayerTeam.formatNameForTeam(team, Component.literal(owner)).getString();
    }

    //? if <1.20.2 {
    /*private static final int SIDEBAR_SLOT = 1;
    *///?}
}
