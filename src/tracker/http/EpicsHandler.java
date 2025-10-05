/**
 * HTTP-обработчик для пути /epics.
 * <p>
 * Реализует REST-эндпоинты для управления эпиками и получения списка их подзадач:
 * <ul>
 *   <li>GET /epics — получение списка всех эпиков</li>
 *   <li>GET /epics/{id} — получение одного эпика по ID</li>
 *   <li>GET /epics/{id}/subtasks — получение списка подзадач конкретного эпика</li>
 *   <li>POST /epics — создание нового или обновление существующего эпика</li>
 *   <li>DELETE /epics — удаление всех эпиков</li>
 *   <li>DELETE /epics/{id} — удаление одного эпика</li>
 * </ul>
 * <p>
 * Использует методы TaskManager: addEpic, updateEpic, getEpic, getAllEpics, removeEpic, clearEpics, getSubtasksOfEpic.
 * Все данные передаются в формате JSON.
 */

package tracker.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import tracker.controllers.TaskManager;
import tracker.model.Epic;

import java.io.IOException;

public class EpicsHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;

    public EpicsHandler(TaskManager manager) {
        super();
        this.manager = manager;
    }

    @Override
    public void handle(HttpExchange h) throws IOException {
        try {
            String path = h.getRequestURI().getPath(); // /epics | /epics/{id} | /epics/{id}/subtasks
            String[] parts = path.split("/");

            String method = h.getRequestMethod();
            switch (method) {
                case "GET":
                    handleGet(h, parts);
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

    private void handleGet(HttpExchange h, String[] parts) throws IOException {
        // /epics/{id}/subtasks
        if (parts.length == 4 && "subtasks".equals(parts[3])) {
            int epicId;
            try {
                epicId = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                sendNotFound(h, "Invalid epic id");
                return;
            }
            if (manager.getEpic(epicId) == null) {
                sendNotFound(h, "Epic " + epicId + " not found");
                return;
            }
            sendOk(h, manager.getSubtasksOfEpic(epicId));
            return;
        }

        // /epics или /epics/{id}
        Integer id = pathId(h);
        if (id == null) {
            sendOk(h, manager.getAllEpics());
        } else {
            Epic e = manager.getEpic(id);
            if (e == null) {
                sendNotFound(h, "Epic " + id + " not found");
                return;
            }
            sendOk(h, e);
        }
    }

    private void handlePost(HttpExchange h) throws IOException {
        Epic epic = gson.fromJson(readBody(h), Epic.class);
        if (epic.getId() == 0) {
            manager.addEpic(epic);
            sendCreated(h, epic);
        } else {
            if (manager.getEpic(epic.getId()) == null) {
                sendNotFound(h, "Epic " + epic.getId() + " not found");
                return;
            }
            manager.updateEpic(epic);
            sendCreated(h, epic);
        }
    }

    private void handleDelete(HttpExchange h) throws IOException {
        Integer id = pathId(h);
        if (id == null) {
            manager.clearEpics();
            sendOk(h, null);
        } else {
            if (manager.getEpic(id) == null) {
                sendNotFound(h, "Epic " + id + " not found");
                return;
            }
            manager.removeEpic(id);
            sendOk(h, null);
        }
    }
}
