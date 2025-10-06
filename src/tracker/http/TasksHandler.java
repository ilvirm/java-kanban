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

public class TasksHandler extends BaseHttpHandler implements HttpHandler {
    public TasksHandler(TaskManager manager) {
        super(manager);
    }

    @Override
    public void handle(HttpExchange h) throws IOException {
        try {
            String method = h.getRequestMethod();
            switch (method) {
                case "GET":
                    handleGet(h);
                    break;
                case "POST":
                    handlePost(h);
                    break;
                case "DELETE":
                    handleDelete(h);
                    break;
                default:
                    sendServerError(h, "Unsupported method");
            }
        } catch (Exception e) {
            sendServerError(h, e.getMessage());
        }
    }

    private void handleGet(HttpExchange h) throws IOException {
        Integer id = pathId(h);
        if (id == null) {
            sendOk(h, manager.getAllTasks());
        } else {
            Task t = manager.getTask(id);
            if (t == null) {
                sendNotFound(h, "Task " + id + " not found");
                return;
            }
            sendOk(h, t);
        }
    }

    private void handlePost(HttpExchange h) throws IOException {
        Task incoming = gson.fromJson(readBody(h), Task.class);
        if (incoming.getId() == 0) {
            // создание
            manager.addTask(incoming);
            sendCreated(h, incoming);
        } else {
            // обновление
            if (manager.getTask(incoming.getId()) == null) {
                sendNotFound(h, "Task " + incoming.getId() + " not found");
                return;
            }
            manager.updateTask(incoming);
            sendCreated(h, incoming);
        }
    }

    private void handleDelete(HttpExchange h) throws IOException {
        Integer id = pathId(h);
        if (id == null) {
            manager.clearTasks();
            sendOk(h, null);
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
