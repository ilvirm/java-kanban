package tracker.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import tracker.controllers.InMemoryTaskManager;
import tracker.controllers.TaskManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;   // JDK 11+
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Базовый класс для HTTP-тестов.
 * Содержит:
 *  - запуск и остановку HttpTaskServer перед/после каждого теста;
 *  - общий HttpClient (java.net.http);
 *  - Gson и хелперы для GET/POST/DELETE;
 *  - assertStatus для проверки статуса ответа.
 *
 * абстрактный класс — не запускается напрямую.
 * Конкретные тесты (TasksApiTest, SubtasksApiTest, ...) наследуются от него.
 */
public abstract class HttpApiTestBase {

    /** Менеджер задач для тестов — “чистая” InMemory-реализация */
    protected TaskManager manager;

    /** Тестируемый HTTP-сервер */
    protected HttpTaskServer server;

    /** HTTP-клиент JDK (без внешних зависимостей) */
    protected HttpClient client;

    /** JSON-сериализатор */
    protected Gson gson;

    /**
     * Подготовка окружения перед каждым тестом:
     *  1) создаём InMemoryTaskManager;
     *  2) поднимаем HttpTaskServer на 8080;
     *  3) чистим данные в менеджере (на всякий случай);
     *  4) готовим HttpClient и Gson.
     */
    @BeforeEach
    void setUp() throws Exception {
        manager = new InMemoryTaskManager();
        server  = new HttpTaskServer(manager);  // порт 8080 внутри конструктора
        client  = HttpClient.newHttpClient();
        gson    = new GsonBuilder().serializeNulls().create();

        // “чистый лист” перед каждым тестом
        manager.clearTasks();
        manager.clearSubtasks();
        manager.clearEpics();

        server.start(); // начинаем слушать порт
    }

    /**
     * Корректная остановка сервера после каждого теста.
     * Null-check нужен на случай, если setUp() не завершился (например, исключение при старте),
     * чтобы не получить NullPointerException и не “подвесить” порт.
     */
    @AfterEach
    void tearDown() {
        if (server != null) { // ✅ фикс NPE и “Address already in use”
            server.stop(0);   // 0 = остановить немедленно, освободить порт
        }
    }

    // =================== HTTP helpers ===================

    /**
     * Выполнить HTTP GET на путь API (например, "/tasks/1").
     */
    protected HttpResponse<String> GET(String path) throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder(URI.create("http://localhost:8080" + path))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Выполнить HTTP DELETE на путь API.
     */
    protected HttpResponse<String> DELETE(String path) throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder(URI.create("http://localhost:8080" + path))
                .DELETE()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Выполнить HTTP POST с JSON-телом на путь API.
     */
    protected HttpResponse<String> POST(String path, String json) throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder(URI.create("http://localhost:8080" + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json == null ? "" : json))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Утверждение на код статуса.
     */
    protected void assertStatus(int expected, HttpResponse<?> resp) {
        assertEquals(expected, resp.statusCode(), "HTTP status mismatch");
    }
}
