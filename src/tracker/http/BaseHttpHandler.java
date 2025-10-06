/**
 * Базовый абстрактный HTTP-обработчик, предоставляющий общие вспомогательные методы
 * для всех конкретных обработчиков API.
 * <p>
 * Содержит:
 * <ul>
 *   <li>Методы для отправки ответов с кодами 200, 201, 404, 500 в формате JSON</li>
 *   <li>Метод для чтения тела запроса</li>
 *   <li>Метод для извлечения ID из пути запроса (например, /tasks/123)</li>
 * </ul>
 * <p>
 * Использует библиотеку Gson для сериализации/десериализации объектов в JSON.
 * Все специализированные обработчики (TasksHandler, SubtasksHandler и т.д.) наследуются от этого класса.
 */

package tracker.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import tracker.controllers.TaskManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public abstract class BaseHttpHandler {

    protected final Gson gson;
    protected final TaskManager manager;

    // пустая строка перед конструктором (EmptyLineSeparator)
    public BaseHttpHandler(TaskManager manager) {
        this.manager = manager;
        this.gson = new GsonBuilder()
                .serializeNulls()
                .create();
    }

    // ===== успех =====
    protected void sendOk(HttpExchange h, Object payload) throws IOException {
        sendJson(h, 200, payload);
    }

    protected void sendCreated(HttpExchange h, Object payload) throws IOException {
        sendJson(h, 201, payload);
    }

    // ===== ошибки =====
    protected void sendNotFound(HttpExchange h, String message) throws IOException {
        sendJson(h, 404, new ErrorDto(message == null ? "Not Found" : message));
    }

    protected void sendServerError(HttpExchange h, String message) throws IOException {
        sendJson(h, 500, new ErrorDto(message == null ? "Internal Server Error" : message));
    }

    // 400 Bad Request
    public void sendBadRequest(HttpExchange h, String message) throws IOException {
        sendJson(h, 400, new ErrorDto(message == null ? "Bad Request" : message));
    }

    protected static class BadRequestException extends RuntimeException {
        public BadRequestException(String m) { super(m); }
    }

    // 405 Method Not Allowed (+ заголовок Allow)
    public void sendMethodNotAllowed(HttpExchange h, String message, String allowHeader) throws IOException {
        if (allowHeader != null && !allowHeader.isBlank()) {
            h.getResponseHeaders().set("Allow", allowHeader);
        }
        sendJson(h, 405, new ErrorDto(message == null ? "Method Not Allowed" : message));
    }

    private void sendJson(HttpExchange h, int code, Object payload) throws IOException {
        byte[] body = (payload == null)
                ? new byte[0]
                : gson.toJson(payload).getBytes(StandardCharsets.UTF_8);

        h.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        h.sendResponseHeaders(code, body.length);

        // переносы строк внутри блока и пробелы вокруг { } (WhitespaceAround, Left/RightCurly)
        if (body.length > 0) {
            h.getResponseBody().write(body);
        }

        h.close();
    }

    // ===== вспомогательные =====
    protected String readBody(HttpExchange h) throws IOException {
        try (InputStream is = h.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Возвращает id из пути вида /segment/{id} */
    protected Integer pathId(HttpExchange h) {
        String[] parts = h.getRequestURI().getPath().split("/");
        if (parts.length >= 3) {
            try {
                return Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {
                // ignore
            }
        }
        return null;
    }

    // закрывающая скобка класса — на своей строке (RightCurlyAlone)
    static final class ErrorDto {
        final String message;

        ErrorDto(String message) {
            this.message = message;
        }
    }
}
