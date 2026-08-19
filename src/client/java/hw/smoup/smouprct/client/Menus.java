package hw.smoup.smouprct.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public final class Menus {

    private static final int PLAYER_INVENTORY_SLOTS = 36;
    private static final int LORE_LINES_IN_LABEL = 2;

    private Menus() {
    }

    public static AbstractContainerScreen<?> openContainer(Minecraft mc) {
        Screen screen = mc.screen;
        boolean serverMenu = screen instanceof AbstractContainerScreen<?>
                && !(screen instanceof InventoryScreen);
        return serverMenu ? (AbstractContainerScreen<?>) screen : null;
    }

    public static int contentSlotCount(AbstractContainerMenu menu) {
        int total = menu.slots.size();
        return total > PLAYER_INVENTORY_SLOTS ? total - PLAYER_INVENTORY_SLOTS : total;
    }

    public static ItemStack itemAt(AbstractContainerMenu menu, int slot) {
        if (slot < 0 || slot >= menu.slots.size()) return ItemStack.EMPTY;
        return menu.slots.get(slot).getItem();
    }

    public static boolean hasContent(AbstractContainerMenu menu) {
        int slots = contentSlotCount(menu);
        for (int slot = 0; slot < slots; slot++) {
            if (!menu.slots.get(slot).getItem().isEmpty()) return true;
        }
        return false;
    }

    public static String snapshot(AbstractContainerScreen<?> screen) {
        if (screen == null) return "";
        AbstractContainerMenu menu = screen.getMenu();
        StringBuilder snapshot = new StringBuilder()
                .append(menu.containerId).append('|')
                .append(screen.getTitle().getString()).append('|');

        int slots = contentSlotCount(menu);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = menu.slots.get(slot).getItem();
            snapshot.append(stack.isEmpty() ? "-" : label(stack)).append(';');
        }
        return snapshot.toString();
    }

    public static String describe(AbstractContainerScreen<?> screen) {
        if (screen == null) return "экрана нет";
        AbstractContainerMenu menu = screen.getMenu();
        StringBuilder description = new StringBuilder("\"")
                .append(screen.getTitle().getString())
                .append("\" (id=").append(menu.containerId).append(") слоты:");

        int slots = contentSlotCount(menu);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = menu.slots.get(slot).getItem();
            if (stack.isEmpty()) continue;
            description.append("\n  ").append(slot)
                    .append(": label=\"").append(label(stack))
                    .append("\" text=\"").append(textOf(stack).replace('\n', '|')).append('"');
        }
        return description.toString();
    }

    public static String label(ItemStack stack) {
        String name = stack.getHoverName().getString().trim();
        return name.isEmpty() ? firstMeaningfulLoreLines(stack) : name;
    }

    public static String textOf(ItemStack stack) {
        StringBuilder text = new StringBuilder(stack.getHoverName().getString());
        for (String line : loreLines(stack)) {
            text.append('\n').append(line);
        }
        return text.toString();
    }

    private static String firstMeaningfulLoreLines(ItemStack stack) {
        List<String> meaningful = new ArrayList<>();
        for (String line : loreLines(stack)) {
            String trimmed = line.trim();
            if (MenuText.tokens(trimmed).isEmpty()) continue;
            meaningful.add(trimmed);
            if (meaningful.size() == LORE_LINES_IN_LABEL) break;
        }
        return String.join(" ", meaningful);
    }

    private static List<String> loreLines(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return lines;

        for (Component line : lore.lines()) {
            lines.add(line.getString());
        }
        return lines;
    }
}
