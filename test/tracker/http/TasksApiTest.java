// test/tracker/http/TasksApiTest.java

/**
 * Набор unit-тестов для эндпоинтов раздела /tasks.
 *
 * Что проверяем:
 *  • POST /tasks — создание новой задачи → 201 и появление в менеджере;
 *  • GET  /tasks/{id} — успешное получение существующей задачи → 200 и корректное тело;
 *  • GET  /tasks/{id} — запрос несуществующей задачи → 404;
 *  • POST /tasks — обновление существующей задачи → 201 и сохранение изменений;
 *  • POST /tasks — обновление несуществующей задачи → 404;
 *  • DELETE /tasks/{id} — удаление одной задачи → 200 и отсутствие в менеджере;
 *  • DELETE /tasks — удаление всех задач → 200 и пустой список.
 *
 * Подготовка/окружение:
 *  • для каждого теста поднимается новый HttpTaskServer с InMemoryTaskManager (через HttpApiTestBase);
 *  • сервер корректно останавливается после теста, чтобы не блокировать порт.
 *
 * Критерии:
 *  • проверяются коды ответов и фактическое состояние InMemoryTaskManager.
 */

package tracker.http;

import org.junit.jupiter.api.Test;
import tracker.model.Status;
import tracker.model.Task;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class TasksApiTest extends HttpApiTestBase {

    @Test
    void createTask_returns201_andAppearsInManager() throws Exception {
        Task t = new Task("Test 1", "Desc", Status.NEW);
        var resp = POST("/tasks", gson.toJson(t));
        assertStatus(201, resp);

        ArrayList<Task> all = manager.getAllTasks();
        assertEquals(1, all.size());
        assertEquals("Test 1", all.get(0).getTitle());
    }

    @Test
    void getTask_existing_returns200_withBody() throws Exception {
        Task t = new Task("T", "D", Status.NEW);
        manager.addTask(t);
        var resp = GET("/tasks/" + t.getId());
        assertStatus(200, resp);
        Task fromJson = gson.fromJson(resp.body(), Task.class);
        assertEquals(t.getId(), fromJson.getId());
    }

    @Test
    void getTask_notExisting_returns404() throws Exception {
        var resp = GET("/tasks/999");
        assertStatus(404, resp);
    }

    @Test
    void updateTask_existing_returns201_andChangesPersist() throws Exception {
        Task t = new Task("A", "B", Status.NEW);
        manager.addTask(t);
        t.setTitle("A+");
        t.setStatus(Status.IN_PROGRESS);

        var resp = POST("/tasks", gson.toJson(t));
        assertStatus(201, resp);

        Task saved = manager.getTask(t.getId());
        assertEquals("A+", saved.getTitle());
        assertEquals(Status.IN_PROGRESS, saved.getStatus());
    }

    @Test
    void updateTask_notExisting_returns404() throws Exception {
        Task ghost = new Task("x", "y", Status.NEW);
        ghost.setId(777);
        var resp = POST("/tasks", gson.toJson(ghost));
        assertStatus(404, resp);
    }

    @Test
    void deleteTask_existing_returns200_andRemoved() throws Exception {
        Task t = new Task("A", "B", Status.NEW);
        manager.addTask(t);
        var resp = DELETE("/tasks/" + t.getId());
        assertStatus(200, resp);
        assertNull(manager.getTask(t.getId()));
    }

    @Test
    void deleteAllTasks_returns200_andListEmpty() throws Exception {
        manager.addTask(new Task("A", "B", Status.NEW));
        manager.addTask(new Task("C", "D", Status.NEW));
        var resp = DELETE("/tasks");
        assertStatus(200, resp);
        assertTrue(manager.getAllTasks().isEmpty());
    }
}

