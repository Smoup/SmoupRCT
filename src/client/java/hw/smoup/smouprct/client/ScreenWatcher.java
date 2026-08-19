package hw.smoup.smouprct.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class ScreenWatcher {

    private final RctConfig config;

    private boolean appeared;
    private boolean logged;
    private String snapshot;
    private int stableTicks;

    public ScreenWatcher(RctConfig config) {
        this.config = config;
    }

    public boolean hasAppeared() {
        return appeared;
    }

    public void noteAppeared() {
        appeared = true;
        logged = false;
        snapshot = null;
        stableTicks = 0;
    }

    public void forget() {
        appeared = false;
        logged = false;
        snapshot = null;
        stableTicks = 0;
    }

    public boolean trackStability(AbstractContainerScreen<?> screen) {
        String current = Menus.snapshot(screen);
        if (!current.equals(snapshot)) {
            snapshot = current;
            stableTicks = 0;
            return false;
        }
        return ++stableTicks == config.contentStableTicks;
    }

    public boolean isSettled(AbstractContainerMenu menu, int ticksOnScreen) {
        if (!Menus.hasContent(menu)) return ticksOnScreen >= config.menuOpenTimeoutTicks;
        return stableTicks >= config.contentStableTicks && ticksOnScreen >= config.menuSettleTicks;
    }

    public void logOnce(AbstractContainerScreen<?> screen) {
        if (logged) return;
        logged = true;
        RctLog.detail("Экран {}", Menus.describe(screen));
    }
}
