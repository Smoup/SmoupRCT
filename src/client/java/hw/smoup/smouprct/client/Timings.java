package hw.smoup.smouprct.client;

import java.util.ArrayList;
import java.util.List;

public class Timings {

    public List<Integer> hubTravel = new ArrayList<>();
    public List<Integer> menuOpen = new ArrayList<>();
    public List<Integer> contentSettle = new ArrayList<>();
    public List<Integer> screenChange = new ArrayList<>();

    public void repair() {
        if (hubTravel == null) hubTravel = new ArrayList<>();
        if (menuOpen == null) menuOpen = new ArrayList<>();
        if (contentSettle == null) contentSettle = new ArrayList<>();
        if (screenChange == null) screenChange = new ArrayList<>();
    }

    public int samples() {
        return menuOpen.size();
    }
}
