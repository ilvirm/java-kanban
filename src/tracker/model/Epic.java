package tracker.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

//Epic наследуется от Task
public class Epic extends Task {
    private final ArrayList<Integer> subtaskIds = new ArrayList<>();

    public Epic(String title, String description) {
        super(title, description, Status.NEW);
    }

    // --- NEW (SPRINT-8): конструктор с id для восстановления из файла (CSV/менеджера)
    public Epic(int id, String title, String description) {
        super(id, title, description, Status.NEW);
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

    // --- NEW (SPRINT-8): duration/start/end эпика агрегируются из сабтасков

    // getDuration: возвращаем как есть (null означает «не вычислено/нет сабтасков»)
    @Override
    public Duration getDuration() {                             // null = «нет данных/пусто»
        return super.getDuration();
    }

    @Override
    public LocalDateTime getStartTime() {
        return super.getStartTime();
        // Обычно TaskManager пересчитывает: минимальное startTime среди subtasks
    }

    // getEndTime переопределим ниже — будет возвращать агрегированное endTime эпика

    // --- NEW (SPRINT-8): тип задачи для сериализации/CSV
    @Override
    public TaskType getType() {
        return TaskType.EPIC;
    }

    @Override
    public String toString() {
        return "Epic{" +
                "id=" + getId() +
                ", title='" + getTitle() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", status=" + getStatus() +
                ", subtaskIds=" + subtaskIds +
                // --- NEW (SPRINT-8): добавили время/длительность
                ", duration=" + getDuration() +
                ", startTime=" + getStartTime() +
                ", endTime=" + getEndTime() +
                '}';
    }

    // --- NEW (SPRINT-8): агрегированное endTime эпика (max по сабтаскам)
    private LocalDateTime endTimeAgg;

    @Override
    public LocalDateTime getEndTime() { // теперь не из start+duration!
        return endTimeAgg;
    }

    // --- NEW (SPRINT-8): прямые методы для менеджера (нужны public — другой пакет)
    public void setStartTimeDirect(LocalDateTime startTime) {
        super.setStartTime(startTime); }

    public void setDurationDirect(Duration duration)       {
        super.setDuration(duration); }

    public void setEndTimeDirect(LocalDateTime endTime)    {
        this.endTimeAgg = endTime; }

    // (опционально) запрет ручных сеттеров времени у эпика:
    @Override public void setStartTime(LocalDateTime startTime) { /* no-op */ }
    @Override public void setDuration(Duration duration)         { /* no-op */ }
}

