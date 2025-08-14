/*
 * ТЕСТЫ ДЛЯ Epic и интеграции с TaskManager
 * -----------------------------------------
 * Порядок проверок (совпадает с порядком тестов ниже):
 *
 * 1) epicsWithSameIdShouldBeEqual
 *    - Два разных объекта Epic с одинаковым id считаются равными (equals по id и типу).
 *
 * 2) subtaskCannotBeItsOwnEpic
 *    - Подзадача не может ссылаться на саму себя как на эпик:
 *      условие subtask.getId() == subtask.getEpicId() должно приводить к отказу (исключению),
 *      и при этом в эпик не должна добавляться ни одна подзадача.
 *
 * 3) subtaskEpicMustExist
 *    - Нельзя добавить подзадачу, если epicId не ссылается на существующий эпик.
 *      Ожидается исключение и отсутствие изменений в данных.
 */

package tracker.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tracker.controllers.Managers;
import tracker.controllers.TaskManager;

import static org.junit.jupiter.api.Assertions.*;

public class EpicTest {

    private TaskManager manager;

    @BeforeEach
    void setUp() {
        manager = Managers.getDefault();
    }

    // 1) Равенство эпиков с одинаковым id
    @Test
    void epicsWithSameIdShouldBeEqual() {
        Epic epic1 = new Epic("Epic One", "First epic");
        Epic epic2 = new Epic("Epic Two", "Second epic");

        epic1.setId(500);
        epic2.setId(500);

        assertEquals(epic1, epic2, "Epic объекты с одинаковым id должны считаться равными");
    }

    // 2) Подзадача не может быть «своим же эпиком»
    // Т.е. subtask.getId() == subtask.getEpicId() — недопустимое состояние.
    @Test
    void subtaskCannotBeItsOwnEpic() {
        // Arrange: создаём валидный эпик
        Epic epic = new Epic("Epic", "Test epic");
        manager.addEpic(epic);
        int epicId = epic.getId();

        // Создаём подзадачу, указывая epicId = id эпика (это нормально),
        // но принудительно ставим subtask.id == epicId (это симуляция неверных данных)
        Subtask subtask = new Subtask("Invalid Subtask", "Should not be added", Status.NEW, epicId);
        subtask.setId(epicId);

        // Act & Assert: ожидаем исключение при добавлении
        assertThrows(IllegalArgumentException.class,
                () -> manager.addSubtask(subtask),
                "Ожидалось исключение при попытке сделать подзадачу своим же эпиком");

        // Дополнительно проверяем, что список подзадач у эпика пуст (ничего не добавлено)
        assertTrue(manager.getEpicSubtaskIds(epicId).isEmpty(),
                "После неуспешной попытки у эпика не должно появиться подзадач");
    }

    // 3) Нельзя добавить подзадачу к несуществующему эпику
    @Test
    void subtaskEpicMustExist() {
        // Arrange: не создаём ни одного эпика
        int nonExistentEpicId = 999_999;

        Subtask subtask = new Subtask("Orphan Subtask", "No real epic", Status.NEW, nonExistentEpicId);

        // Act & Assert: ожидаем исключение
        assertThrows(IllegalArgumentException.class,
                () -> manager.addSubtask(subtask),
                "Ожидалось исключение при добавлении подзадачи к несуществующему эпику");
    }
}