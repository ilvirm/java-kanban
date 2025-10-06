/**
 * Основной HTTP-сервер приложения трекера задач.
 * <p>
 * Класс поднимает встроенный {@link com.sun.net.httpserver.HttpServer} на порту 8080,
 * регистрирует обработчики всех базовых путей API:
 * <ul>
 *   <li>/tasks — для работы с обычными задачами</li>
 *   <li>/subtasks — для работы с подзадачами</li>
 *   <li>/epics — для работы с эпиками и списками их подзадач</li>
 *   <li>/history — для получения истории просмотров</li>
 * </ul>
 * <p>
 * Экземпляр {@link tracker.controllers.TaskManager} передаётся в сервер извне (через Managers.getDefault()).
 * При запуске метод {@link #start()} начинает прослушивать входящие HTTP-запросы до завершения процесса.
 */

package tracker.http;

import com.sun.net.httpserver.HttpServer;
import tracker.controllers.TaskManager;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpTaskServer {
    public static final int PORT = 8080;

    private final HttpServer server;

    public HttpTaskServer(TaskManager manager) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(PORT), 0);
        // Регистрация обработчиков
        server.createContext("/tasks",    new TasksHandler(manager));
        server.createContext("/subtasks", new SubtasksHandler(manager));
        server.createContext("/epics",    new EpicsHandler(manager));
        server.createContext("/history",  new HistoryHandler(manager));
    }

    public void start() {
        server.start();
        System.out.println("HTTP server started at http://localhost:" + PORT);
    }

    public void stop(int delaySec) {
        server.stop(delaySec);
    }

    public static void main(String[] args) throws Exception {

        TaskManager manager = null;

        if (manager == null) {
            throw new IllegalStateException("TaskManager is not initialized");
        }
        new HttpTaskServer(manager).start();
    }
}

