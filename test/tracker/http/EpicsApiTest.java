// test/tracker/http/EpicsApiTest.java
/**
 * Набор unit-тестов для эндпоинтов раздела /epics и маршрута /epics/{id}/subtasks.
 *
 * Что проверяем:
 *  • POST /epics — создание эпика → 201, далее GET /epics/{id} → 200 и корректное тело;
 *  • GET  /epics/{id} — запрос несуществующего эпика → 404;
 *  • GET  /epics/{id}/subtasks — список подзадач конкретного эпика → 200;
 *  • DELETE /epics/{id} — каскадное удаление эпика и его подзадач → 200 и пустой список подзадач.
 *
 * Подготовка:
 *  • в ряде тестов предварительно создаются подзадачи, связанные с эпиком;
 *  • сервер/менеджер поднимаются/очищаются через HttpApiTestBase.
 *
 * Критерии:
 *  • проверяем коды ответов;
 *  • убеждаемся, что каскадное удаление работает (подзадачи эпика исчезают).
 */

package tracker.http;

import org.junit.jupiter.api.Test;
import tracker.model.*;

import static org.junit.jupiter.api.Assertions.*;

public class EpicsApiTest extends HttpApiTestBase {

    @Test
    void createEpic_201_andGetById_200() throws Exception {
        Epic e = new Epic("E", "D");
        var r1 = POST("/epics", gson.toJson(e));
        assertStatus(201, r1);

        Epic saved = manager.getAllEpics().get(0);
        var r2 = GET("/epics/" + saved.getId());
        assertStatus(200, r2);
        Epic fromJson = gson.fromJson(r2.body(), Epic.class);
        assertEquals(saved.getId(), fromJson.getId());
    }

    @Test
    void getEpic_notExisting_404() throws Exception {
        assertStatus(404, GET("/epics/999"));
    }

    @Test
    void epicSubtasks_existingEpic_returns200_list() throws Exception {
        Epic e = new Epic("E", "D");
        manager.addEpic(e);
        manager.addSubtask(new Subtask("S1", "d", Status.NEW, e.getId()));
        manager.addSubtask(new Subtask("S2", "d", Status.NEW, e.getId()));

        var resp = GET("/epics/" + e.getId() + "/subtasks");
        assertStatus(200, resp);
        //проверка статуса
    }

    @Test
    void epicSubtasks_notExistingEpic_returns404() throws Exception {
        assertStatus(404, GET("/epics/999/subtasks"));
    }

    @Test
    void deleteEpic_existing_200_andCascade() throws Exception {
        Epic e = new Epic("E", "D");
        manager.addEpic(e);
        manager.addSubtask(new Subtask("S", "d", Status.NEW, e.getId()));

        assertFalse(manager.getAllSubtasks().isEmpty());
        assertStatus(200, DELETE("/epics/" + e.getId()));
        assertTrue(manager.getAllSubtasks().isEmpty(), "подзадачи эпика должны удаляться каскадно");
    }
}
