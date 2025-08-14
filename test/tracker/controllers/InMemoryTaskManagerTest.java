/*
 * ТЕСТЫ ДЛЯ InMemoryTaskManager
 * -----------------------------
 * Порядок и содержание проверок (соответствует порядку тестов ниже):
 *
 * 1) addsAndFindsTasksById
 *    - Менеджер добавляет задачи всех типов (Task/Epic/Subtask) и возвращает их по id.
 *
 * 2) manualAndGeneratedIdDoNotConflict
 *    - Ручной id не конфликтует с автоматически сгенерированными id:
 *      обе задачи доступны по своим id, и количество задач корректно.
 *
 * 3) taskFieldsRemainUnchangedAfterAdding
 *    - Поля задачи после добавления не меняются (совпадают с исходными значениями).
 */

package tracker.controllers;

//один менеджер на тест через @BeforeEach, чтобы тесты не зависели друг от друга
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tracker.model.Epic;
import tracker.model.Status;
import tracker.model.Subtask;
import tracker.model.Task;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryTaskManagerTest {

    private InMemoryTaskManager manager;

    @BeforeEach
    void setUp() {
        manager = new InMemoryTaskManager();
    }

    // 1) Менеджер добавляет и находит сущности по id
    @Test
    void addsAndFindsTasksById() {
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
        assertEquals(task,    manager.getTask(taskId),     "Не удалось получить Task по id");
        assertEquals(epic,    manager.getEpic(epicId),     "Не удалось получить Epic по id");
        assertEquals(subtask, manager.getSubtask(subtaskId),"Не удалось получить Subtask по id");
    }

    /*
     * 2) Ручной id + авто-id не конфликтуют
     * Шаги:
     *  - добавляем задачу с вручную заданным id (через безопасный метод addTaskWithCustomId)
     *  - добавляем задачу с авто-id
     *  - убеждаемся, что обе доступны по своим id и не перетёрли друг друга
     */
    @Test
    void manualAndGeneratedIdDoNotConflict() {
        // Добавляем задачу с вручную заданным id
        Task manualTask = new Task("Manual Task", "Created manually", Status.NEW);
        manualTask.setId(1000);
        manager.addTaskWithCustomId(manualTask); // используем безопасный метод, чтобы не ломать генератор id

        // Добавляем задачу с автоматически сгенерированным id
        Task generatedTask = new Task("Generated Task", "Created via manager", Status.NEW);
        manager.addTask(generatedTask);
        int generatedId = generatedTask.getId();

        // --- Проверки ---
        assertEquals(manualTask,    manager.getTask(1000),     "Ручная задача с id=1000 не найдена");
        assertEquals(generatedTask, manager.getTask(generatedId),"Сгенерированная задача не найдена");
        assertNotEquals(1000, generatedId,                     "Сгенерированный id не должен совпадать с ручным");
        assertEquals(2, manager.getAllTasks().size(),          "Должно быть две задачи в системе");
    }

    // 3) Поля задачи остаются неизменными после добавления в менеджер
    @Test
    void taskFieldsRemainUnchangedAfterAdding() {
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
        assertEquals(title,       retrievedTask.getTitle(),       "Заголовок задачи не совпадает");
        assertEquals(description, retrievedTask.getDescription(), "Описание задачи не совпадает");
        assertEquals(status,      retrievedTask.getStatus(),      "Статус задачи не совпадает");
        assertEquals(taskId,      retrievedTask.getId(),          "ID задачи должен совпадать после добавления");
    }
}