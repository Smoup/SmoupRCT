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

public final class Menus {

    private static final int PLAYER_INVENTORY_SLOTS = 36;

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

    public static ItemStack itemAt(AbstractContainerMenu menu, int slotId) {
        if (slotId < 0 || slotId >= menu.slots.size()) return ItemStack.EMPTY;
        return menu.slots.get(slotId).getItem();
    }

    public static String textOf(ItemStack stack) {
        StringBuilder sb = new StringBuilder(stack.getHoverName().getString());
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                sb.append('\n').append(line.getString());
            }
        }
        return sb.toString();
    }
}
