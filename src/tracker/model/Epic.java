package tracker.model;

import java.util.ArrayList;

//Epic наследуется от Task
public class Epic extends Task {
    private final ArrayList<Integer> subtaskIds = new ArrayList<>();

    public Epic(String title, String description) {
        super(title, description, Status.NEW);
    }

    public ArrayList<Integer> getSubtaskIds() {
        return subtaskIds;
    }

    public void addSubtaskId(int id) {
        subtaskIds.add(id);
    }

    public void removeSubtaskId(int id) {
        subtaskIds.remove((Integer) id);
    }

    public void clearSubtasks() {
        subtaskIds.clear();
    }

    //Переопределяем метод и не даём вручную поменять статус эпика.
    @Override
    public void setStatus(Status status) {
        // Ничего не делаем!
    }

    // Статус эпика меняется на основе подзадач в TaskManager
    public void setStatusDirect(Status status) {
        super.setStatus(status);
    }
}

