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

public class HistoryHandler extends BaseHttpHandler implements HttpHandler {
    private final TaskManager manager;

    public HistoryHandler(TaskManager manager) {
        super();
        this.manager = manager;
    }

    @Override
    public void handle(HttpExchange h) throws IOException {
        try {
            if (!"GET".equals(h.getRequestMethod())) {
                sendServerError(h, "Unsupported method");
                return;
            }
            sendOk(h, manager.getHistory());
        } catch (Exception e) {
            sendServerError(h, e.getMessage());
        }
    }
}
