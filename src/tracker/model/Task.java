package tracker.model;

import java.time.Duration;          // --- NEW (SPRINT-8): длительность задачи в минутах, как тип Duration
import java.time.LocalDateTime;     // --- NEW (SPRINT-8): дата/время старта задачи

public class Task {
    // Базовый класс, описывающий стандартную задачу.
    // Инкапсуляция: все поля private, доступ через геттеры и сеттеры.

    private int id;                  // Уникальный идентификатор задачи
    private String title;            // Название задачи, кратко описывающее суть
    private String description;      // Подробное описание задачи
    private Status status;           // Статус выполнения задачи

    // --- NEW (SPRINT-8): время/длительность для приоритезации и проверки пересечений
    // duration — оценка длительности задачи (в минутах, храним как java.time.Duration)
    // startTime — предполагаемая дата и время начала выполнения задачи (LocalDateTime)
    private Duration duration;               // может быть null, если оценка не задана
    private LocalDateTime startTime;         // может быть null, если старт не задан

    // Конструктор
    public Task(String title, String description, Status status) {
        this.title = title;
        this.description = description;
        this.status = status;
    }

    // --- NEW (SPRINT-8): перегруженный конструктор с полями времени.
    // Удобно использовать там, где сразу известны startTime и duration.
    public Task(String title, String description, Status status,
                LocalDateTime startTime, Duration duration) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.startTime = startTime;
        this.duration = duration;
    }

    // --- NEW (SPRINT-8): конструктор с id (для восстановления из файла/CSV)
    // Добавлен, чтобы Epic/Manager могли воссоздавать объект с заданным id.
    public Task(int id, String title, String description, Status status) {      // NEW
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    // --- NEW (SPRINT-8): конструктор с id + временем (на случай CSV с датами)
    public Task(int id, String title, String description, Status status,        // NEW
                LocalDateTime startTime, Duration duration) {
        this(id, title, description, status);
        this.startTime = startTime;
        this.duration = duration;
    }

    // Копирующий конструктор.
    // Чтобы при добавлении задачи в список, создавался новый объект (ссылка).

    public Task(Task task) {
        this.id = task.id;
        this.title = task.title;
        this.description = task.description;
        this.status = task.status;

        // --- NEW (SPRINT-8): копируем новые поля времени, чтобы клон был полным.
        this.duration = task.duration;
        this.startTime = task.startTime;

    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    // --- NEW (SPRINT-8): геттеры/сеттеры для времени и длительности.

    /**
     * Возвращает оценку длительности задачи.
     * Может быть null, если длительность не задана.
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Устанавливает длительность задачи.
     * @param duration продолжительность; допускается null (означает «не задано»)
     */
    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    /**
     * Возвращает дату/время начала задачи.
     * Может быть null, если старт не задан.
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Устанавливает дату/время начала задачи.
     * @param startTime дата/время; допускается null (означает «не задано»)
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    // --- NEW (SPRINT-8): вычисление времени завершения.
    // Конвенция: если startTime или duration отсутствуют — вернуть null.
    public LocalDateTime getEndTime() {
        if (startTime == null || duration == null) return null;
        return startTime.plus(duration);
    }

    // --- NEW (SPRINT-8): тип задачи для CSV/сериализации/логики
    public TaskType getType() {                                          // NEW
        return TaskType.TASK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    // --- NEW (SPRINT-8): вывод новых полей для удобства отладки/логирования.
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", startTime=" + startTime +
                ", duration=" + (duration == null ? null : duration.toMinutes() + "m") +
                ", endTime=" + getEndTime() +
                '}';
    }
}
