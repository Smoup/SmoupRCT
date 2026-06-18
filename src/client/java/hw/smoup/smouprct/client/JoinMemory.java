package hw.smoup.smouprct.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

public final class JoinMemory {

    private static RctConfig config;

    public static void init(RctConfig cfg) {
        config = cfg;
    }

    public static void onSlotClick(int slotId) {
        if (config == null) return;
        AbstractContainerScreen<?> screen = Menus.openContainer(Minecraft.getInstance());
        if (screen == null) return;

        ItemStack stack = Menus.itemAt(screen.getMenu(), slotId);
        if (stack.isEmpty()) return;

        ModeRegistry.parseServer(Menus.textOf(stack)).ifPresent(JoinMemory::remember);
    }

    private static void remember(ModeRegistry.ServerListing server) {
        boolean categoryChanged =
                config.putCategory(server.mode().id(), server.number(), server.categoryId());
        config.setLastJoined(server.mode().id(), server.number());
        if (categoryChanged) config.save();
    }
}
