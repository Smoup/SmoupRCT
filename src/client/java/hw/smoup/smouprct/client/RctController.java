package hw.smoup.smouprct.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

@Slf4j(topic = "SmoupRCT")
@RequiredArgsConstructor
public class RctController {

    private static final String PREFIX = "[SmoupRCT] ";
    private static final String HUB_COMMAND = "hub";
    private static final String MENU_COMMAND = "menu";
    private static final String MODE_SCREEN_TITLE = "выберите режим";

    private static final int MAX_OPERATION_TICKS = 20 * 15;
    private static final int TELEPORT_SETTLE_TICKS = 5;

    private enum State {
        IDLE,
        GOTO_HUB,
        WAIT_MODE,
        WAIT_SERVERS,
        AFTER_SERVER_CLICK
    }

    private final RctConfig config;

    private State state = State.IDLE;
    private int timer = 0;
    private int globalTimer = 0;
    private String targetNumber = null;
    private ModeRegistry.Mode targetMode = null;

    private final Set<String> visitedCategories = new HashSet<>();
    private boolean leftServer = false;
    private int menuRetryTimer = 0;

    public boolean isBusy() {
        return state != State.IDLE;
    }

    public void start(String number, String scoreboardModeText) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || isBusy()) {
            return;
        }

        ModeRegistry.Mode mode = resolveMode(scoreboardModeText);
        if (mode == null) {
            error(mc, "Не удалось определить режим. Сначала зайди на режим.");
            return;
        }
        if (number == null) {
            error(mc, "Не знаю номер текущего сервера.");
            return;
        }

        beginOperation(mode, number);
        sendCommand(mc, HUB_COMMAND);
    }

    private ModeRegistry.Mode resolveMode(String scoreboardModeText) {
        ModeRegistry.Mode mode = ModeRegistry.matchByScoreboard(scoreboardModeText);
        if (mode != null) return mode;
        return config.lastModeId != null ? ModeRegistry.byId(config.lastModeId) : null;
    }

    private void beginOperation(ModeRegistry.Mode mode, String number) {
        targetMode = mode;
        targetNumber = number;
        visitedCategories.clear();
        leftServer = false;
        menuRetryTimer = 0;
        timer = 0;
        globalTimer = 0;
        state = State.GOTO_HUB;
    }

    public void tick(Minecraft mc) {
        if (state == State.IDLE) return;

        if (++globalTimer > MAX_OPERATION_TICKS) {
            abort(mc, "Истёк таймаут операции.");
            return;
        }

        if (mc.player == null || mc.getConnection() == null) {
            if (state == State.GOTO_HUB) leftServer = true;
            return;
        }
        timer++;

        switch (state) {
            case GOTO_HUB -> tickGotoHub(mc);
            case WAIT_MODE -> tickWaitMode(mc);
            case WAIT_SERVERS -> tickWaitServers(mc);
            case AFTER_SERVER_CLICK -> tickAfterServerClick();
            default -> { }
        }
    }

    private void tickGotoHub(Minecraft mc) {
        boolean arrived = leftServer || globalTimer >= config.hubArriveFallbackTicks;
        if (!arrived) return;
        sendCommand(mc, MENU_COMMAND);
        menuRetryTimer = 0;
        enter(State.WAIT_MODE);
    }

    private void tickWaitMode(Minecraft mc) {
        AbstractContainerScreen<?> screen = Menus.openContainer(mc);
        if (screen == null || !isModeScreen(screen)) {
            retryMenuOrTimeout(mc);
            return;
        }
        if (timer < config.menuSettleTicks) return;

        AbstractContainerMenu menu = screen.getMenu();
        OptionalInt iconSlot = findSlot(menu, stack -> hasAllKeywords(stack, targetMode.iconKeywords()));
        if (iconSlot.isEmpty()) {
            abort(mc, "Иконка режима " + targetMode.label() + " не найдена в /menu.");
            return;
        }
        click(mc, menu, iconSlot.getAsInt());
        enter(State.WAIT_SERVERS);
    }

    private void retryMenuOrTimeout(Minecraft mc) {
        if (++menuRetryTimer >= config.menuRetryTicks) {
            sendCommand(mc, MENU_COMMAND);
            menuRetryTimer = 0;
        }
        if (timer >= config.menuOpenTimeoutTicks) {
            abort(mc, "Меню не открылось.");
        }
    }

    private void tickWaitServers(Minecraft mc) {
        AbstractContainerScreen<?> screen = Menus.openContainer(mc);
        if (screen == null || isModeScreen(screen)) {
            if (timer >= config.menuOpenTimeoutTicks) {
                abort(mc, "Экран выбора сервера не открылся.");
            }
            return;
        }
        if (timer < config.menuSettleTicks) return;

        navigateServers(mc, screen.getMenu());
    }

    private void navigateServers(Minecraft mc, AbstractContainerMenu menu) {
        try {
            int targetSlot = scanAndCacheServers(menu);
            click(mc, menu, targetSlot);
            enter(State.AFTER_SERVER_CLICK);
        } catch (ServerNotOnScreenException notHere) {
            continueSearchOrFail(mc, menu);
        }
    }

    private void continueSearchOrFail(Minecraft mc, AbstractContainerMenu menu) {
        if (targetMode.hasCategories() && openNextCategory(mc, menu)) return;
        abort(mc, "Сервер #" + targetNumber + " не найден в " + targetMode.label() + ".");
    }

    private int scanAndCacheServers(AbstractContainerMenu menu) throws ServerNotOnScreenException {
        OptionalInt targetSlot = OptionalInt.empty();
        boolean changed = false;
        int slots = Menus.contentSlotCount(menu);
        for (int i = 0; i < slots; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) continue;

            ModeRegistry.ServerListing server = ModeRegistry.parseServer(Menus.textOf(stack)).orElse(null);
            if (server == null) continue;

            changed |= config.putCategory(targetMode.id(), server.number(), server.categoryId());
            visitedCategories.add(server.categoryId());
            if (server.number().equals(targetNumber)) targetSlot = OptionalInt.of(i);
        }
        if (changed) config.save();
        return targetSlot.orElseThrow(ServerNotOnScreenException::new);
    }

    private static final class ServerNotOnScreenException extends Exception {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private boolean openNextCategory(Minecraft mc, AbstractContainerMenu menu) {
        for (ModeRegistry.Category category : categoriesByPriority()) {
            if (visitedCategories.contains(category.id())) continue;
            OptionalInt slot = findSlot(menu, stack -> nameContains(stack, category.nameKeyword()));
            if (slot.isEmpty()) continue;

            visitedCategories.add(category.id());
            click(mc, menu, slot.getAsInt());
            enter(State.WAIT_SERVERS);
            return true;
        }
        return false;
    }

    private List<ModeRegistry.Category> categoriesByPriority() {
        ModeRegistry.Category cached = ModeRegistry.categoryById(config.getCategory(targetMode.id(), targetNumber));
        if (cached == null) return ModeRegistry.CATEGORIES;

        List<ModeRegistry.Category> ordered = new ArrayList<>();
        ordered.add(cached);
        ordered.addAll(ModeRegistry.CATEGORIES);
        return ordered;
    }

    private void tickAfterServerClick() {
        boolean leftMenu = Menus.openContainer(Minecraft.getInstance()) == null;
        if (leftMenu && timer >= TELEPORT_SETTLE_TICKS) {
            reset();
        } else if (timer >= config.postClickTicks) {
            reset();
        }
    }

    private static OptionalInt findSlot(AbstractContainerMenu menu, Predicate<ItemStack> matches) {
        int slots = Menus.contentSlotCount(menu);
        for (int i = 0; i < slots; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (!stack.isEmpty() && matches.test(stack)) return OptionalInt.of(i);
        }
        return OptionalInt.empty();
    }

    private static boolean hasAllKeywords(ItemStack stack, List<String> keywords) {
        return ModeRegistry.allContained(Menus.textOf(stack), keywords);
    }

    private static boolean nameContains(ItemStack stack, String keyword) {
        return stack.getHoverName().getString().toLowerCase().contains(keyword);
    }

    private void enter(State next) {
        state = next;
        timer = 0;
    }

    private void abort(Minecraft mc, String reason) {
        log.warn("Abort: {}", reason);
        error(mc, reason);
        closeMenu(mc);
        reset();
    }

    private void reset() {
        state = State.IDLE;
        timer = 0;
        targetNumber = null;
        targetMode = null;
        visitedCategories.clear();
        leftServer = false;
        menuRetryTimer = 0;
    }

    private static void click(Minecraft mc, AbstractContainerMenu menu, int slotId) {
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handleInventoryMouseClick(menu.containerId, slotId, 0, ClickType.PICKUP, mc.player);
    }

    private static void closeMenu(Minecraft mc) {
        if (mc.player != null && Menus.openContainer(mc) != null) {
            mc.player.closeContainer();
            mc.setScreen(null);
        }
    }

    private static void sendCommand(Minecraft mc, String command) {
        if (mc.getConnection() != null) {
            mc.getConnection().sendCommand(command);
        }
    }

    private static boolean isModeScreen(AbstractContainerScreen<?> screen) {
        return screen.getTitle().getString().toLowerCase().contains(MODE_SCREEN_TITLE);
    }

    private static void error(Minecraft mc, String text) {
        if (mc.player == null) return;
        Component message = Component
                .literal(PREFIX)
                .withStyle(ChatFormatting.GOLD)
                .append(
                        Component
                                .literal(text)
                                .withStyle(ChatFormatting.RED)
                );
        mc.player.displayClientMessage(message, false);
    }
}
