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
import tracker.http.BaseHttpHandler.BadRequestException;

import java.io.IOException;

public class SubtasksHandler extends BaseHttpHandler implements HttpHandler {
    public SubtasksHandler(TaskManager manager) {
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
                    // 405 + Allow для неподдерживаемых методов
                    sendMethodNotAllowed(h, "Method not allowed", "GET, POST, DELETE");
            }
        } catch (BadRequestException e) {
            sendBadRequest(h, e.getMessage());
        } catch (Exception e) {
            sendServerError(h, e.getMessage());
        }
    }

    private void handleGet(HttpExchange h) throws IOException {
        // различаем "id отсутствует" и "id не число" → 400
        String[] parts = h.getRequestURI().getPath().split("/");
        Integer id = pathId(h);
        // если сегмент с ID есть, но распарсить его не удалось — 400
        if (parts.length >= 3 && parts[2] != null && !parts[2].isBlank() && id == null) {
            sendBadRequest(h, "Invalid subtask id");
            return;
        }
        if (id == null) {
            sendOk(h, manager.getAllSubtasks());
        } else {
            Subtask s = manager.getSubtask(id);
            if (s == null) {
                sendNotFound(h, "Subtask " + id + " not found");
                return;
            }
            sendOk(h, s);
        }
    }

    private void handlePost(HttpExchange h) throws IOException {

        String body = readBody(h);
        if (body == null || body.isBlank()) {
            throw new BadRequestException("Empty request body");
        }

        Subtask incoming;
        try {
            incoming = gson.fromJson(body, Subtask.class);
        } catch (Exception parse) {
            throw new BadRequestException("Invalid JSON");
        }
        if (incoming == null) {
            throw new BadRequestException("Invalid JSON");
        }

        if (incoming.getId() == 0) {
            // создание
            manager.addSubtask(incoming);
            sendCreated(h, incoming);
        } else {
            // обновление
            if (manager.getSubtask(incoming.getId()) == null) {
                sendNotFound(h, "Subtask " + incoming.getId() + " not found");
                return;
            }
            manager.updateSubtask(incoming);
            sendOk(h, incoming); // 200 OK на update
        }
    }

    private void handleDelete(HttpExchange h) throws IOException {
        String[] parts = h.getRequestURI().getPath().split("/");
        Integer id = pathId(h);
        // если сегмент с ID есть, но распарсить его не удалось — 400
        if (parts.length >= 3 && parts[2] != null && !parts[2].isBlank() && id == null) {
            sendBadRequest(h, "Invalid subtask id");
            return;
        }
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
