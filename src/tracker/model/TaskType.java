package tracker.model;

// При сохранении в CSV нужно знать не только статус, но и тип задачи.
// TaskType нужен, чтобы при загрузке файла понять, какой объект создать (Task, Epic, SUBTASK)

public enum TaskType {
    TASK,
    EPIC,
    SUBTASK
}