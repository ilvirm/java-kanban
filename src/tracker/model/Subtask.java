package tracker.model;

// Subtask наследуется от Task
public class Subtask extends Task {
    private int epicId; //Добавляем epicID, чтобы понимать к какому эпику эта подзадача относится.

    // --- NEW (SPRINT-8): добавляем поля времени и длительности из Task
    // (наследуются автоматически, так как Subtask расширяет Task).
    // Никаких дополнительных полей тут не нужно, но нужно расширить конструктор.

    // Конструктор без времени (базовый)

    public Subtask(String title, String description, Status status, int epicId) {
        super(title, description, status);
        this.epicId = epicId;
    }

    // --- NEW (SPRINT-8): перегруженный конструктор с временем и длительностью
    // Добавлен для приоритезации и проверки пересечений по времени.
    public Subtask(String title, String description, Status status, int epicId,
                   java.time.LocalDateTime startTime, java.time.Duration duration) {
        super(title, description, status, startTime, duration); // вызывает новый конструктор Task
        this.epicId = epicId;
    }

    public int getEpicId() {
        return epicId;
    }

    public void setEpicId(int epicId) {
        this.epicId = epicId;
    }

    public Subtask(int id, String title, String description, Status status, int epicId,
                   java.time.LocalDateTime startTime, java.time.Duration duration) {
        super(id, title, description, status, startTime, duration);
        this.epicId = epicId;
    }

    @Override
    public String toString() {
        return "Subtask{" +
                "id=" + getId() +
                ", title='" + getTitle() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", status=" + getStatus() +
                ", epicId=" + epicId +
                // --- NEW (SPRINT-8): добавляем отображение времени и длительности
                ", startTime=" + getStartTime() +
                ", duration=" + (getDuration() == null ? null : getDuration().toMinutes() + "m") +
                ", endTime=" + getEndTime() +
                '}';
    }
    @Override
    public TaskType getType() { return TaskType.SUBTASK; }
}