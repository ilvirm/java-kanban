package tracker.controllers;

import org.junit.jupiter.api.Test;
import tracker.model.Epic;
import tracker.model.Status;
import tracker.model.Subtask;
import tracker.model.Task;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryTaskManagerTest {

    // проверьте, что InMemoryTaskManager действительно добавляет задачи разного типа и может найти их по id;
    // добавим по одной задаче каждого типа, сохраним их id, и проверим, что getTask(), getEpic(), getSubtask()
    // возвращают правильные объекты.

    @Test
    void addsAndFindsTasksById() {
        TaskManager manager = Managers.getDefault();

        // --- Добавляем обычную задачу ---
        Task task = new Task("Task", "Simple task", Status.NEW);
        manager.addTask(task);
        int taskId = task.getId();

        // --- Добавляем эпик ---
        Epic epic = new Epic("Epic", "Parent epic");
        manager.addEpic(epic);
        int epicId = epic.getId();

        // --- Добавляем подзадачу ---
        Subtask subtask = new Subtask("Subtask", "Child of epic", Status.NEW, epicId);
        manager.addSubtask(subtask);
        int subtaskId = subtask.getId();

        // --- Проверка по id ---
        assertEquals(task, manager.getTask(taskId), "Не удалось получить Task по id");
        assertEquals(epic, manager.getEpic(epicId), "Не удалось получить Epic по id");
        assertEquals(subtask, manager.getSubtask(subtaskId), "Не удалось получить Subtask по id");
    }

    /* Проверьте, что задачи с заданным id и сгенерированным id не конфликтуют внутри менеджера;
       1.Добавить задачу с вручную заданным id (task.setId(1000)).
       2.Затем добавить задачу с автоматически сгенерированным id (generateId()).
       3.Убедиться, что оба id разные, задачи не перезаписываются, и менеджер хранит обе задачи.
    */
    @Test
    void manualAndGeneratedIdDoNotConflict() {
        InMemoryTaskManager manager = new InMemoryTaskManager();

        // Добавляем задачу с вручную заданным id
        Task manualTask = new Task("Manual Task", "Created manually", Status.NEW);
        manualTask.setId(1000);
        manager.addTaskWithCustomId(manualTask); // используем безопасный метод

        // Добавляем задачу через обычный метод — id будет сгенерирован автоматически
        Task generatedTask = new Task("Generated Task", "Created via manager", Status.NEW);
        manager.addTask(generatedTask);
        int generatedId = generatedTask.getId();

        // --- Проверки ---
        assertEquals(manualTask, manager.getTask(1000), "Ручная задача с id=1000 не найдена");
        assertEquals(generatedTask, manager.getTask(generatedId), "Сгенерированная задача не найдена");
        assertNotEquals(1000, generatedId, "Сгенерированный id не должен совпадать с ручным");
        assertEquals(2, manager.getAllTasks().size(), "Должно быть две задачи в системе");
    }

    // проверка неизменности задачи при добавлении
    @Test
    void taskFieldsRemainUnchangedAfterAdding() {
        InMemoryTaskManager manager = new InMemoryTaskManager();

        // Arrange — создаём задачу
        String title = "Test Task";
        String description = "This is a test task";
        Status status = Status.NEW;

        Task originalTask = new Task(title, description, status);

        // Act — добавляем в менеджер
        manager.addTask(originalTask);
        int taskId = originalTask.getId(); // id устанавливается менеджером
        Task retrievedTask = manager.getTask(taskId);

        // Assert — проверяем, что поля совпадают
        assertNotNull(retrievedTask, "Задача должна быть найдена в менеджере");
        assertEquals(title, retrievedTask.getTitle(), "Заголовок задачи не совпадает");
        assertEquals(description, retrievedTask.getDescription(), "Описание задачи не совпадает");
        assertEquals(status, retrievedTask.getStatus(), "Статус задачи не совпадает");
        assertEquals(taskId, retrievedTask.getId(), "ID задачи должен совпадать после добавления");
    }
}