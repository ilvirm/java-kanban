/**
 * HTTP-обработчик для пути /tasks.
 * <p>
 * Реализует REST-эндпоинты для управления обычными задачами:
 * <ul>
 *   <li>GET /tasks — получение списка всех задач</li>
 *   <li>GET /tasks/{id} — получение одной задачи по ID</li>
 *   <li>POST /tasks — создание новой или обновление существующей задачи</li>
 *   <li>DELETE /tasks — удаление всех задач</li>
 *   <li>DELETE /tasks/{id} — удаление одной задачи</li>
 * </ul>
 * <p>
 * Взаимодействует с методами TaskManager: addTask, updateTask, getTask, getAllTasks, removeTask, clearTasks.
 * Возвращает ответы в формате JSON, с кодами 200, 201, 404, 500 в зависимости от результата.
 */

package tracker.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import tracker.controllers.TaskManager;
import tracker.model.Task;

import java.io.IOException;
import java.util.ArrayList;

public class TasksHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;

    public TasksHandler(TaskManager manager) {
        super();
        this.manager = manager;
    }

    @Override
    public void handle(HttpExchange h) throws IOException {
        try {
            switch (h.getRequestMethod()) {
                case "GET"    -> handleGet(h);
                case "POST"   -> handlePost(h);
                case "DELETE" -> handleDelete(h);
                default       -> sendServerError(h, "Unsupported method");
            }
        } catch (Exception e) {
            sendServerError(h, e.getMessage());
        }
    }

    private void handleGet(HttpExchange h) throws IOException {
        Integer id = pathId(h);
        if (id == null) {
            ArrayList<Task> all = manager.getAllTasks();
            sendOk(h, all);
        } else {
            Task t = manager.getTask(id);
            if (t == null) { sendNotFound(h, "Task " + id + " not found"); return; }
            sendOk(h, t);
        }
    }

    private void handlePost(HttpExchange h) throws IOException {
        Task incoming = gson.fromJson(readBody(h), Task.class);
        // Если id = 0 (или отсутствует), считаем это созданием
        if (incoming.getId() == 0) {
            manager.addTask(incoming);
            // Менеджер присвоит id — вернём актуальную задачу из менеджера
            sendCreated(h, incoming); // 201 по ТЗ
        } else {
            // Проверим, существует ли задача
            if (manager.getTask(incoming.getId()) == null) {
                sendNotFound(h, "Task " + incoming.getId() + " not found");
                return;
            }
            manager.updateTask(incoming);
            sendCreated(h, incoming); // 201 по ТЗ
        }
    }

    private void handleDelete(HttpExchange h) throws IOException {
        Integer id = pathId(h);
        if (id == null) {
            manager.clearTasks();
            sendOk(h, null); // 200, без тела
        } else {
            if (manager.getTask(id) == null) {
                sendNotFound(h, "Task " + id + " not found");
                return;
            }
            manager.removeTask(id);
            sendOk(h, null);
        }
    }
}
