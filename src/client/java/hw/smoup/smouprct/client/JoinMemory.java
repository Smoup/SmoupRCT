package hw.smoup.smouprct.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class JoinMemory {

    private static final int STEP_CONFIRM_TICKS = 60;
    private static final int JOIN_WAIT_TICKS = 20 * 60;
    private static final int MENU_CLOSED_TICKS = 40;

    private static RctConfig config;
    private static boolean modIsClicking;

    private static final List<String> steps = new ArrayList<>();
    private static final List<String> stepScreens = new ArrayList<>();

    private static String clickedStep;
    private static String clickedOnScreen;
    private static String screenBeforeClick;
    private static int clickTicks;
    private static int menuClosedTicks;

    private static List<String> joiningPath;
    private static String joiningNumber;
    private static boolean sidebarDisappeared;
    private static int joinTicks;

    private JoinMemory() {
    }

    public static void init(RctConfig cfg) {
        config = cfg;
    }

    public static void selfClick(boolean active) {
        modIsClicking = active;
    }

    public static void onSlotClick(int slot) {
        if (config == null || modIsClicking) return;

        AbstractContainerScreen<?> screen = Menus.openContainer(Minecraft.getInstance());
        if (screen == null) return;

        AbstractContainerMenu menu = screen.getMenu();
        if (slot < 0 || slot >= Menus.contentSlotCount(menu)) return;

        ItemStack stack = Menus.itemAt(menu, slot);
        if (stack.isEmpty()) return;

        String number = MenuText.serverNumber(Menus.textOf(stack));
        if (number != null) {
            expectJoin(pathIncludingUnconfirmedStep(), number);
            return;
        }

        String label = Menus.label(stack);
        if (label.isEmpty()) return;

        clickedStep = label;
        clickedOnScreen = screen.getTitle().getString();
        screenBeforeClick = Menus.snapshot(screen);
        clickTicks = 0;
    }

    public static void expectJoin(List<String> path, String number) {
        if (config == null) return;
        joiningPath = new ArrayList<>(path);
        joiningNumber = number;
        sidebarDisappeared = false;
        joinTicks = 0;
        clickedStep = null;
    }

    public static void tick(Minecraft mc, String sidebarLine) {
        if (config == null) return;
        confirmClickedStep(mc);
        confirmJoin(sidebarLine);
        forgetPathIfMenuLeft(mc);
    }

    private static List<String> pathIncludingUnconfirmedStep() {
        List<String> path = new ArrayList<>(steps);
        if (clickedStep != null && path.isEmpty()) path.add(clickedStep);
        return path;
    }

    private static void confirmClickedStep(Minecraft mc) {
        if (clickedStep == null) return;

        AbstractContainerScreen<?> screen = Menus.openContainer(mc);
        if (screen != null && !Menus.snapshot(screen).equals(screenBeforeClick)) {
            appendStep();
            RctLog.detail("Шаг записан: \"{}\", путь теперь {}", clickedStep, steps);
            clickedStep = null;
            return;
        }
        if (++clickTicks > STEP_CONFIRM_TICKS) {
            clickedStep = null;
        }
    }

    private static void appendStep() {
        if (isSameMenuLevel()) {
            steps.set(steps.size() - 1, clickedStep);
            return;
        }
        steps.add(clickedStep);
        stepScreens.add(clickedOnScreen);
    }

    private static boolean isSameMenuLevel() {
        return !stepScreens.isEmpty() && stepScreens.getLast().equals(clickedOnScreen);
    }

    private static void confirmJoin(String sidebarLine) {
        if (joiningPath == null) return;

        if (++joinTicks > JOIN_WAIT_TICKS) {
            forgetJoin();
            return;
        }
        if (sidebarLine == null) {
            sidebarDisappeared = true;
            return;
        }
        if (!sidebarDisappeared) return;

        String number = MenuText.serverNumber(sidebarLine);
        if (number == null || !number.equals(joiningNumber)) return;

        String boardKey = MenuText.boardKey(sidebarLine);
        if (joiningPath.isEmpty()) {
            RctLog.warn("Сервер #{} (отпечаток \"{}\") — путь потерян, запоминать нечего", number, boardKey);
        } else {
            RctLog.detail("Запомнил: путь {} -> сервер #{}, отпечаток \"{}\"", joiningPath, number, boardKey);
            config.remember(joiningPath, boardKey, number);
        }
        forgetJoin();
    }

    private static void forgetPathIfMenuLeft(Minecraft mc) {
        boolean menuBusy = joiningPath != null || clickedStep != null
                || Menus.openContainer(mc) != null;
        if (menuBusy) {
            menuClosedTicks = 0;
            return;
        }
        if (steps.isEmpty()) return;
        if (++menuClosedTicks < MENU_CLOSED_TICKS) return;

        RctLog.detail("Меню закрыто — след {} забыт", steps);
        forgetPath();
        menuClosedTicks = 0;
    }

    private static void forgetJoin() {
        joiningPath = null;
        joiningNumber = null;
        sidebarDisappeared = false;
        joinTicks = 0;
        forgetPath();
    }

    private static void forgetPath() {
        steps.clear();
        stepScreens.clear();
    }
}
