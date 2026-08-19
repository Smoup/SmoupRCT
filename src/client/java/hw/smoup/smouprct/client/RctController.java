package hw.smoup.smouprct.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

public class RctController {

    private static final String HUB_COMMAND = "hub";
    private static final String MENU_COMMAND = "menu";
    private static final int TELEPORT_SETTLE_TICKS = 5;

    private enum State {
        IDLE,
        TRAVELLING_TO_HUB,
        WALKING_MENU,
        REOPENING_MENU,
        AWAITING_SCREEN_CHANGE,
        AFTER_SERVER_CLICK
    }

    private final RctConfig config;
    private final OperationTimings timings = new OperationTimings();
    private final ScreenWatcher watcher;
    private final Set<String> visitedBranches = new HashSet<>();

    private State state = State.IDLE;
    private int stateTicks;
    private int operationTicks;

    private String targetNumber;
    private List<String> plannedSteps = List.of();
    private int nextStep;
    private String enteredMode;
    private String enteredBranch;
    private int branchAttempts;

    private Level levelAtStart;
    private boolean hubReached;
    private boolean connectionLost;
    private int menuRetryTicks;
    private int modeIconRetries;

    private String snapshotBeforeClick;

    public RctController(RctConfig config) {
        this.config = config;
        this.watcher = new ScreenWatcher(config);
    }

    public boolean isBusy() {
        return state != State.IDLE;
    }

    public void start(String number, String boardKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null || isBusy()) return;

        List<String> path = resolvePath(mc, boardKey);
        if (path == null) return;
        if (number == null) {
            Chat.error(mc, "Не знаю номер сервера. Напиши .rct <номер>.");
            return;
        }

        beginOperation(path, number);
        sendCommand(mc, HUB_COMMAND);
    }

    private List<String> resolvePath(Minecraft mc, String boardKey) {
        List<ModeEntry> candidates = config.byBoard(boardKey);
        if (candidates.size() == 1) return candidates.getFirst().steps;

        if (candidates.size() > 1) {
            for (ModeEntry candidate : candidates) {
                if (candidate.sameSteps(config.lastSteps)) return candidate.steps;
            }
            Chat.error(mc, RctMessages.ambiguousMode(candidates));
            return null;
        }
        if (boardKey == null && !config.lastSteps.isEmpty()) return config.lastSteps;

        Chat.error(mc, RctMessages.unknownMode());
        return null;
    }

    private void beginOperation(List<String> path, String number) {
        plannedSteps = new ArrayList<>(path);
        targetNumber = number;
        levelAtStart = Minecraft.getInstance().level;
        timings.reset();
        forgetProgress();
        branchAttempts = 0;
        connectionLost = false;
        modeIconRetries = 0;
        operationTicks = 0;
        switchTo(State.TRAVELLING_TO_HUB);
    }

    public void tick(Minecraft mc) {
        if (state == State.IDLE) return;

        if (++operationTicks > config.operationTimeoutTicks) {
            TimingTuner.onOperationTimeout(config);
            abort(mc, "Истёк таймаут операции.");
            return;
        }
        if (mc.player == null || mc.getConnection() == null) {
            if (state == State.TRAVELLING_TO_HUB) connectionLost = true;
            return;
        }
        stateTicks++;

        switch (state) {
            case TRAVELLING_TO_HUB -> travelToHub(mc);
            case WALKING_MENU -> walkMenu(mc);
            case REOPENING_MENU -> reopenMenu(mc);
            case AWAITING_SCREEN_CHANGE -> awaitScreenChange(mc);
            case AFTER_SERVER_CLICK -> awaitTeleport();
            default -> { }
        }
    }

    private void travelToHub(Minecraft mc) {
        if (!hubReached) {
            if (!hasArrivedInHub(mc)) return;
            noteHubReached(mc);
            return;
        }
        if (stateTicks < config.hubSettleTicks) return;
        requestMenu(mc);
    }

    private boolean hasArrivedInHub(Minecraft mc) {
        return connectionLost
                || hasChangedWorld(mc)
                || operationTicks >= config.hubArriveFallbackTicks;
    }

    private boolean hasChangedWorld(Minecraft mc) {
        return mc.level != null && mc.level != levelAtStart;
    }

    private void noteHubReached(Minecraft mc) {
        hubReached = true;
        stateTicks = 0;
        if (hasChangedWorld(mc)) timings.hubReached(operationTicks);
        RctLog.detail("В хабе за {} тиков, мир сменился: {}", operationTicks, hasChangedWorld(mc));
    }

    private void requestMenu(Minecraft mc) {
        sendCommand(mc, MENU_COMMAND);
        menuRetryTicks = 0;
        timings.menuRequested(operationTicks);
        forgetProgress();
        switchTo(State.WALKING_MENU);
    }

    private void forgetProgress() {
        nextStep = 0;
        enteredMode = null;
        enteredBranch = null;
        visitedBranches.clear();
        watcher.forget();
    }

    private void walkMenu(Minecraft mc) {
        AbstractContainerScreen<?> screen = Menus.openContainer(mc);
        if (screen == null) {
            watcher.forget();
            awaitMenu(mc);
            return;
        }
        if (!watcher.hasAppeared()) {
            noteScreenAppeared();
            return;
        }

        AbstractContainerMenu menu = screen.getMenu();
        if (watcher.trackStability(screen)) timings.contentSettled(stateTicks);

        if (clickVisibleTarget(mc, screen, menu)) return;
        if (!watcher.isSettled(menu, stateTicks)) return;

        watcher.logOnce(screen);
        handleTargetMissing(mc, screen, menu);
    }

    private void noteScreenAppeared() {
        watcher.noteAppeared();
        timings.menuOpened(operationTicks);
        stateTicks = 0;
    }

    private void awaitMenu(Minecraft mc) {
        if (insideMode()) {
            if (stateTicks >= config.menuOpenTimeoutTicks) {
                TimingTuner.onMenuTimeout(config);
                abort(mc, "Меню закрылось на середине пути.");
            }
            return;
        }
        if (++menuRetryTicks >= config.menuRetryTicks) {
            sendCommand(mc, MENU_COMMAND);
            menuRetryTicks = 0;
            forgetProgress();
        }
        if (stateTicks >= config.menuOpenTimeoutTicks) {
            TimingTuner.onMenuTimeout(config);
            abort(mc, "Меню не открылось.");
        }
    }

    private boolean clickVisibleTarget(Minecraft mc, AbstractContainerScreen<?> screen, AbstractContainerMenu menu) {
        if (insideMode()) {
            OptionalInt serverSlot = MenuSearch.serverSlot(menu, targetNumber);
            if (serverSlot.isPresent()) {
                rememberVisibleServers(menu);
                clickServer(mc, menu, serverSlot.getAsInt());
                return true;
            }
        }
        if (nextStep < plannedSteps.size()) {
            OptionalInt stepSlot = MenuSearch.namedSlot(menu, plannedSteps.get(nextStep));
            if (stepSlot.isPresent()) {
                followStep(mc, screen, menu, stepSlot.getAsInt());
                return true;
            }
        }
        return false;
    }

    private void handleTargetMissing(Minecraft mc, AbstractContainerScreen<?> screen, AbstractContainerMenu menu) {
        nextStep = plannedSteps.size();
        if (!insideMode()) {
            RctLog.warn("Не нашёл шаг \"{}\"", plannedModeName());
            retryFromHubOrGiveUp(mc);
            return;
        }
        rememberVisibleServers(menu);
        if (tryAnotherBranch(mc, screen, menu)) return;

        RctLog.warn("Не нашёл сервер #{} в \"{}\"", targetNumber, enteredMode);
        abort(mc, RctMessages.serverNotFound(targetNumber, enteredMode));
    }

    private void followStep(Minecraft mc, AbstractContainerScreen<?> screen, AbstractContainerMenu menu, int slot) {
        String label = Menus.label(menu.slots.get(slot).getItem());
        if (insideMode()) {
            enteredBranch = label;
            visitedBranches.add(MenuText.normalize(label));
        } else {
            enteredMode = label;
        }
        RctLog.detail("Жму шаг {}: \"{}\"", nextStep, label);
        nextStep++;
        clickAndAwaitChange(mc, screen, menu, slot);
    }

    private boolean tryAnotherBranch(Minecraft mc, AbstractContainerScreen<?> screen, AbstractContainerMenu menu) {
        if (branchAttempts >= config.branchClickLimit) return false;

        OptionalInt slot = knownBranchSlot(menu);
        if (slot.isEmpty()) slot = MenuSearch.unvisitedBranchSlot(menu, visitedBranches);
        if (slot.isEmpty()) return false;

        enteredBranch = Menus.label(menu.slots.get(slot.getAsInt()).getItem());
        visitedBranches.add(MenuText.normalize(enteredBranch));
        branchAttempts++;
        RctLog.detail("Пробую раздел \"{}\" ({} из {})", enteredBranch, branchAttempts, config.branchClickLimit);
        clickAndAwaitChange(mc, screen, menu, slot.getAsInt());
        return true;
    }

    private OptionalInt knownBranchSlot(AbstractContainerMenu menu) {
        for (String branch : branchesByPriority()) {
            if (visitedBranches.contains(MenuText.normalize(branch))) continue;
            OptionalInt slot = MenuSearch.namedSlot(menu, branch);
            if (slot.isPresent()) return slot;
        }
        return OptionalInt.empty();
    }

    private List<String> branchesByPriority() {
        List<String> ordered = new ArrayList<>();
        String remembered = config.branchFor(enteredMode, targetNumber);
        if (remembered != null && !remembered.isEmpty()) ordered.add(remembered);
        for (String known : config.knownBranches(enteredMode)) {
            if (!ordered.contains(known)) ordered.add(known);
        }
        return ordered;
    }

    private void rememberVisibleServers(AbstractContainerMenu menu) {
        boolean changed = false;
        int slots = Menus.contentSlotCount(menu);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = menu.slots.get(slot).getItem();
            if (stack.isEmpty()) continue;

            String number = MenuText.serverNumber(Menus.textOf(stack));
            if (number != null) changed |= config.putBranch(enteredMode, number, enteredBranch);
        }
        if (changed) config.save();
    }

    private void clickServer(Minecraft mc, AbstractContainerMenu menu, int slot) {
        RctLog.detail("Жму сервер #{}, путь {}", targetNumber, walkedPath());
        JoinMemory.expectJoin(walkedPath(), targetNumber);
        click(mc, menu, slot);
        switchTo(State.AFTER_SERVER_CLICK);
    }

    private List<String> walkedPath() {
        List<String> path = new ArrayList<>();
        if (enteredMode != null) path.add(enteredMode);
        if (enteredBranch != null) path.add(enteredBranch);
        return path;
    }

    private void clickAndAwaitChange(Minecraft mc, AbstractContainerScreen<?> screen,
                                     AbstractContainerMenu menu, int slot) {
        snapshotBeforeClick = Menus.snapshot(screen);
        click(mc, menu, slot);
        switchTo(State.AWAITING_SCREEN_CHANGE);
    }

    private void awaitScreenChange(Minecraft mc) {
        AbstractContainerScreen<?> screen = Menus.openContainer(mc);
        boolean changed = screen != null && !Menus.snapshot(screen).equals(snapshotBeforeClick);
        if (changed) timings.screenChanged(stateTicks);

        if (changed || stateTicks >= config.screenChangeTimeoutTicks) {
            watcher.forget();
            switchTo(State.WALKING_MENU);
        }
    }

    private void retryFromHubOrGiveUp(Minecraft mc) {
        TimingTuner.onSlowScreen(config);
        if (modeIconRetries >= config.iconNotFoundRetries) {
            abort(mc, RctMessages.modeIconNotFound(plannedModeName()));
            return;
        }
        modeIconRetries++;
        switchTo(State.REOPENING_MENU);
    }

    private void reopenMenu(Minecraft mc) {
        if (stateTicks < config.iconRetryDelayTicks) return;
        sendCommand(mc, HUB_COMMAND);
        levelAtStart = mc.level;
        hubReached = false;
        forgetProgress();
        switchTo(State.TRAVELLING_TO_HUB);
    }

    private void awaitTeleport() {
        boolean menuClosed = Menus.openContainer(Minecraft.getInstance()) == null;
        boolean teleported = menuClosed && stateTicks >= TELEPORT_SETTLE_TICKS;
        if (teleported || stateTicks >= config.postClickTicks) {
            timings.submitTo(config);
            reset();
        }
    }

    private boolean insideMode() {
        return enteredMode != null;
    }

    private String plannedModeName() {
        return plannedSteps.isEmpty() ? "режима" : plannedSteps.getFirst();
    }

    private void switchTo(State next) {
        state = next;
        stateTicks = 0;
    }

    private void abort(Minecraft mc, String reason) {
        abort(mc, Chat.red(reason));
    }

    private void abort(Minecraft mc, Component reason) {
        RctLog.warn("Отмена: {}", reason.getString());
        Chat.error(mc, reason);
        closeMenu(mc);
        reset();
    }

    private void reset() {
        targetNumber = null;
        plannedSteps = List.of();
        levelAtStart = null;
        hubReached = false;
        connectionLost = false;
        menuRetryTicks = 0;
        modeIconRetries = 0;
        branchAttempts = 0;
        snapshotBeforeClick = null;
        forgetProgress();
        switchTo(State.IDLE);
    }

    private static void click(Minecraft mc, AbstractContainerMenu menu, int slot) {
        if (mc.gameMode == null || mc.player == null) return;
        JoinMemory.selfClick(true);
        try {
            mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, ClickType.PICKUP, mc.player);
        } finally {
            JoinMemory.selfClick(false);
        }
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
}
