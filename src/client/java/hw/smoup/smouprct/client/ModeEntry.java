package hw.smoup.smouprct.client;

import java.util.ArrayList;
import java.util.List;

public class ModeEntry {

    public List<String> steps = new ArrayList<>();
    public List<String> boards = new ArrayList<>();

    public ModeEntry() {
    }

    public ModeEntry(List<String> steps) {
        this.steps = new ArrayList<>(steps);
    }

    public String root() {
        return steps.isEmpty() ? "" : steps.getFirst();
    }

    public String branch() {
        return steps.size() > 1 ? steps.get(1) : null;
    }

    public boolean knowsBoard(String boardKey) {
        for (String board : boards) {
            if (board.equals(boardKey)) return true;
        }
        return false;
    }

    public void addBoard(String boardKey) {
        if (boardKey == null || knowsBoard(boardKey)) return;
        boards.add(boardKey);
    }

    public boolean sameSteps(List<String> other) {
        if (other == null || other.size() != steps.size()) return false;
        for (int i = 0; i < steps.size(); i++) {
            if (!MenuText.sameName(steps.get(i), other.get(i))) return false;
        }
        return true;
    }
}
