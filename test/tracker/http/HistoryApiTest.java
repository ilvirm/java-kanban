// test/tracker/http/HistoryApiTest.java
/**
 * Набор unit-тестов для эндпоинта /history.
 *
 * Что проверяем:
 *  • GET /history — успешный ответ 200 и корректный JSON-массив;
 *  • наполнение истории осуществляется обращением к менеджеру (getTask(id) и т.п.),
 *    после чего эндпоинт возвращает список задач в ожидаемом формате.
 *
 * Подготовка:
 *  • перед запросом создаётся задача и читается из менеджера, чтобы попасть в историю;
 *  • сервер и менеджер создаются на каждый тест (через HttpApiTestBase).
 */


package tracker.http;

import org.junit.jupiter.api.Test;
import tracker.model.Status;
import tracker.model.Task;

import static org.junit.jupiter.api.Assertions.*;

public class HistoryApiTest extends HttpApiTestBase {

    @Test
    void history_returns200_andJsonArray() throws Exception {
        // Наполним историю: получение задачи через менеджер
        Task t = new Task("A", "B", Status.NEW);
        manager.addTask(t);
        manager.getTask(t.getId()); // добавили в историю

        var resp = GET("/history");
        assertStatus(200, resp);
        assertTrue(resp.body().startsWith("["), "ожидается JSON-массив");
    }
}
