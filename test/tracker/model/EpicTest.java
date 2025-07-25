package tracker.model;

import org.junit.jupiter.api.Test;
import tracker.controllers.Managers;
import tracker.controllers.TaskManager;

import static org.junit.jupiter.api.Assertions.*;

public class EpicTest {

    @Test
    void epicsWithSameIdShouldBeEqual() {
        // Создаём два разных объекта Epic с разными названиями и описаниями
        // Обрати внимание: конструктор Epic не принимает статус — он всегда NEW
        Epic epic1 = new Epic("Epic One", "First epic");
        Epic epic2 = new Epic("Epic Two", "Second epic");

        // Назначаем одинаковый id обоим объектам
        epic1.setId(500);
        epic2.setId(500);

        // Проверяем, что equals() возвращает true
        // Это возможно потому, что метод equals() в Task сравнивает только id и тип объекта
        assertEquals(epic1, epic2, "Epic объекты с одинаковым id должны считаться равными");
    }

    // проверьте, что объект Epic нельзя добавить в самого себя в виде подзадачи нужно создать Subtask,
    // у которой epicId совпадает с id самого Epic, и проверить, что эта подзадача не будет добавлена.
    @Test
    void epicCannotBeItsOwnSubtask() {
        // Arrange
        TaskManager manager = Managers.getDefault();

        Epic epic = new Epic("Epic", "Test epic");
        manager.addEpic(epic);
        int epicId = epic.getId();

        // Act & Assert
        Subtask invalidSubtask = new Subtask("Invalid Subtask", "Should not be added", Status.NEW, epicId);
        invalidSubtask.setId(epicId); // Симулируем, что подзадача уже имеет такой же id, как epic

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> manager.addSubtask(invalidSubtask),
                "Ожидалось исключение при попытке добавить эпик как свою же подзадачу"
        );

        assertEquals("Ошибка: подзадача не может быть своим же эпиком (id=" + epicId + ")", exception.getMessage());
    }

    // Нужно убедиться, что Subtask не может ссылаться на себя как на эпик,
    // то есть subtask.getId() == subtask.getEpicId()
    @Test
    void subtaskCannotBeItsOwnEpic() {
        // Arrange
        TaskManager manager = Managers.getDefault();

        Epic epic = new Epic("Parent Epic", "Will be used as real epic");
        manager.addEpic(epic);
        int epicId = epic.getId();

        Subtask subtask = new Subtask("Invalid Subtask", "Should not be allowed", Status.NEW, epicId);

        // Принудительно задаём subtask id == epicId (симулируем ошибку)
        subtask.setId(epicId);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> manager.addSubtask(subtask),
                "Ожидалось исключение при попытке сделать подзадачу своим же эпиком"
        );

        assertEquals("Ошибка: подзадача не может быть своим же эпиком (id=" + epicId + ")", exception.getMessage());
    }

}