package hw.smoup.smouprct.client;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

public final class MenuSearch {

    private static final List<String> SERVER_HINTS =
            List.of("подключ", "зайти", "онлайн", "игрок", "tps", "нажми");

    private static final List<String> NOT_A_BRANCH =
            List.of("назад", "back", "закрыть", "close", "выход", "главное меню",
                    "информация", "профиль", "магазин", "донат", "shop", "помощь");

    private MenuSearch() {
    }

    public static OptionalInt slotMatching(AbstractContainerMenu menu, Predicate<ItemStack> matches) {
        int slots = Menus.contentSlotCount(menu);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = menu.slots.get(slot).getItem();
            if (!stack.isEmpty() && matches.test(stack)) return OptionalInt.of(slot);
        }
        return OptionalInt.empty();
    }

    public static OptionalInt namedSlot(AbstractContainerMenu menu, String wanted) {
        return slotMatching(menu, stack -> MenuText.matches(Menus.textOf(stack), wanted));
    }

    public static OptionalInt serverSlot(AbstractContainerMenu menu, String number) {
        OptionalInt card = slotMatching(menu, stack -> isServerCard(stack, number));
        return card.isPresent() ? card : slotMatching(menu, stack -> hasNumber(stack, number));
    }

    public static OptionalInt unvisitedBranchSlot(AbstractContainerMenu menu, Set<String> visited) {
        return slotMatching(menu, stack -> isBranch(stack, visited));
    }

    private static boolean isServerCard(ItemStack stack, String number) {
        String text = Menus.textOf(stack);
        return MenuText.hasNumber(text, number) && looksLikeServer(text);
    }

    private static boolean hasNumber(ItemStack stack, String number) {
        return MenuText.hasNumber(Menus.textOf(stack), number);
    }

    private static boolean looksLikeServer(String text) {
        String normalized = MenuText.normalize(text);
        for (String hint : SERVER_HINTS) {
            if (normalized.contains(hint)) return true;
        }
        return false;
    }

    private static boolean isBranch(ItemStack stack, Set<String> visited) {
        String label = Menus.label(stack);
        if (label.isEmpty()) return false;
        if (MenuText.serverNumber(Menus.textOf(stack)) != null) return false;

        String normalized = MenuText.normalize(label);
        if (visited.contains(normalized)) return false;
        for (String stopWord : NOT_A_BRANCH) {
            if (normalized.contains(stopWord)) return false;
        }
        return true;
    }
}
