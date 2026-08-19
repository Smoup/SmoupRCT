package hw.smoup.smouprct.client;

public class OperationTimings {

    private static final int NOT_MEASURED = -1;

    private int hubTravel = NOT_MEASURED;
    private int menuOpen = NOT_MEASURED;
    private int contentSettle = NOT_MEASURED;
    private int screenChange = NOT_MEASURED;
    private int menuRequestedAt = NOT_MEASURED;

    public void reset() {
        hubTravel = NOT_MEASURED;
        menuOpen = NOT_MEASURED;
        contentSettle = NOT_MEASURED;
        screenChange = NOT_MEASURED;
        menuRequestedAt = NOT_MEASURED;
    }

    public void hubReached(int elapsed) {
        hubTravel = elapsed;
    }

    public void menuRequested(int elapsed) {
        menuRequestedAt = elapsed;
    }

    public void menuOpened(int elapsed) {
        if (menuRequestedAt == NOT_MEASURED) return;
        menuOpen = Math.max(menuOpen, elapsed - menuRequestedAt);
        menuRequestedAt = NOT_MEASURED;
    }

    public void contentSettled(int elapsed) {
        contentSettle = Math.max(contentSettle, elapsed);
    }

    public void screenChanged(int elapsed) {
        screenChange = Math.max(screenChange, elapsed);
    }

    public void submitTo(RctConfig config) {
        TimingTuner.recordOperation(config, hubTravel, menuOpen, contentSettle, screenChange);
    }
}
