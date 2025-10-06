// test/tracker/http/SubtasksApiTest.java

/**
 * Набор unit-тестов для эндпоинтов раздела /subtasks.
 *
 * Что проверяем:
 *  • POST /subtasks — создание подзадачи, привязанной к существующему эпику → 201;
 *  • GET  /subtasks/{id} — запрос несуществующей подзадачи → 404;
 *  • POST /subtasks — обновление существующей подзадачи → 201 и сохранение изменений;
 *  • DELETE /subtasks/{id} — удаление подзадачи → 200 и отсутствие в менеджере.
 *
 * Подготовка:
 *  • перед тестами создаётся Epic, чтобы корректно валидировать ссылку epicId;
 *  • менеджер и сервер инициализируются в HttpApiTestBase.
 */


package tracker.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tracker.model.*;

import static org.junit.jupiter.api.Assertions.*;

public class SubtasksApiTest extends HttpApiTestBase {
    private Epic epic;

    @BeforeEach
    void createEpic() {
        epic = new Epic("Epic", "E");
        manager.addEpic(epic);
    }

    @Test
    void createSubtask_returns201_andLinkedToEpic() throws Exception {
        Subtask s = new Subtask("S1", "D", Status.NEW, epic.getId());
        var resp = POST("/subtasks", gson.toJson(s));
        assertStatus(201, resp);
        assertEquals(1, manager.getAllSubtasks().size());
        assertEquals(epic.getId(), manager.getAllSubtasks().get(0).getEpicId());
    }

    @Test
    void getSubtask_notExisting_404() throws Exception {
        var resp = GET("/subtasks/999");
        assertStatus(404, resp);
    }

    @Test
    void updateSubtask_existing_201() throws Exception {
        Subtask s = new Subtask("S", "D", Status.NEW, epic.getId());
        manager.addSubtask(s);
        s.setStatus(Status.DONE);
        var resp = POST("/subtasks", gson.toJson(s));
        assertStatus(201, resp);
        assertEquals(Status.DONE, manager.getSubtask(s.getId()).getStatus());
    }

    @Test
    void deleteSubtask_existing_200() throws Exception {
        Subtask s = new Subtask("S", "D", Status.NEW, epic.getId());
        manager.addSubtask(s);
        var resp = DELETE("/subtasks/" + s.getId());
        assertStatus(200, resp);
        assertNull(manager.getSubtask(s.getId()));
    }
}
