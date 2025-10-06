/**
 * HTTP-обработчик для пути /history.
 * <p>
 * Поддерживает только один эндпоинт:
 * <ul>
 *   <li>GET /history — возвращает историю просмотров задач (список Task) в порядке от самой старой к новой</li>
 * </ul>
 * <p>
 * Использует метод TaskManager.getHistory().
 * Возвращает JSON-массив с объектами задач.
 */

package tracker.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import tracker.controllers.TaskManager;

import java.io.IOException;
import java.util.List;

public class HistoryHandler extends BaseHttpHandler implements HttpHandler {
    public HistoryHandler(TaskManager manager) {
        super(manager);
    }

    @Override
    public void handle(HttpExchange h) throws IOException {
        try {
            String method = h.getRequestMethod();
            if (!"GET".equals(method)) {
                // Неподдерживаемый метод → 405 + заголовок Allow
                sendMethodNotAllowed(h, "Method not allowed", "GET");
                return;
            }
            List<?> history = manager.getHistory();
            // Правильный запрос, но данных нет → 404
            if (history == null || history.isEmpty()) {
                sendNotFound(h, "History is empty");
                return;
            }
            sendOk(h, history);
        } catch (Exception e) {
            sendServerError(h, e.getMessage());
        }
    }
}
