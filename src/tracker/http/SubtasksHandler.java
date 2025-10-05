/**
 * HTTP-обработчик для пути /subtasks.
 * <p>
 * Реализует REST-эндпоинты для управления подзадачами:
 * <ul>
 *   <li>GET /subtasks — получение списка всех подзадач</li>
 *   <li>GET /subtasks/{id} — получение одной подзадачи по ID</li>
 *   <li>POST /subtasks — создание новой или обновление существующей подзадачи</li>
 *   <li>DELETE /subtasks — удаление всех подзадач</li>
 *   <li>DELETE /subtasks/{id} — удаление одной подзадачи</li>
 * </ul>
 * <p>
 * Использует методы TaskManager: addSubtask, updateSubtask, getSubtask, getAllSubtasks, removeSubtask, clearSubtasks.
 * Все ответы возвращаются в формате JSON.
 */

package tracker.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import tracker.controllers.TaskManager;
import tracker.model.Subtask;

import java.io.IOException;
import java.util.ArrayList;

public class SubtasksHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;

    public SubtasksHandler(TaskManager manager) {
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
            ArrayList<Subtask> all = manager.getAllSubtasks();
            sendOk(h, all);
        } else {
            Subtask s = manager.getSubtask(id);
            if (s == null) { sendNotFound(h, "Subtask " + id + " not found"); return; }
            sendOk(h, s);
        }
    }

    private void handlePost(HttpExchange h) throws IOException {
        Subtask incoming = gson.fromJson(readBody(h), Subtask.class);
        if (incoming.getId() == 0) {
            manager.addSubtask(incoming);
            sendCreated(h, incoming);
        } else {
            if (manager.getSubtask(incoming.getId()) == null) {
                sendNotFound(h, "Subtask " + incoming.getId() + " not found");
                return;
            }
            manager.updateSubtask(incoming);
            sendCreated(h, incoming);
        }
    }

    private void handleDelete(HttpExchange h) throws IOException {
        Integer id = pathId(h);
        if (id == null) {
            manager.clearSubtasks();
            sendOk(h, null);
        } else {
            if (manager.getSubtask(id) == null) {
                sendNotFound(h, "Subtask " + id + " not found");
                return;
            }
            manager.removeSubtask(id);
            sendOk(h, null);
        }
    }
}

