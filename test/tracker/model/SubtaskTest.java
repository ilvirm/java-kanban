/*
 * ТЕСТЫ ДЛЯ Subtask (модельный уровень)
 * -------------------------------------
 * Порядок проверок (совпадает с порядком тестов ниже):
 *
 * 1) subtasksWithSameIdShouldBeEqual
 *    - Две разные подзадачи с одинаковым id считаются равными (equals по id).
 *
 * 2) subtasksWithDifferentIdsShouldNotBeEqual
 *    - Подзадачи с разными id не равны, даже если остальные поля совпадают.
 *
 * 3) equalsHashCodeContract
 *    - Согласованность equals/hashCode: одинаковые id → одинаковый hashCode.
 *
 * 4) epicIdIsStoredAndCanBeChanged
 *    - Subtask корректно хранит epicId и позволяет его изменить (если это предусмотрено моделью).
 *      (Примечание: проверка валидности epicId и существования эпика — задача уровня TaskManager,
 *       и тестируется отдельно в интеграционных тестах.)
 */

package tracker.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SubtaskTest {

    // 1) Равенство по одинаковому id
    @Test
    void subtasksWithSameIdShouldBeEqual() {
        Subtask s1 = new Subtask("Read book", "Chapter 1", Status.NEW, 1);
        Subtask s2 = new Subtask("Write report", "Chapter 2", Status.IN_PROGRESS, 2);

        s1.setId(100);
        s2.setId(100);

        assertEquals(s1, s2, "Subtask объекты с одинаковым id должны считаться равными");
    }

    // 2) Неравенство при разных id (даже если остальные поля совпадают)
    @Test
    void subtasksWithDifferentIdsShouldNotBeEqual() {
        Subtask s1 = new Subtask("Task", "Desc", Status.NEW, 10);
        Subtask s2 = new Subtask("Task", "Desc", Status.NEW, 10);

        s1.setId(1);
        s2.setId(2);

        assertNotEquals(s1, s2, "Subtask с разными id не должны быть равны");
    }

    // 3) Контракт equals/hashCode: одинаковые id → одинаковый hashCode
    @Test
    void equalsHashCodeContract() {
        Subtask a = new Subtask("A", "D", Status.NEW, 5);
        Subtask b = new Subtask("B", "E", Status.DONE, 7);

        a.setId(777);
        b.setId(777);

        assertEquals(a, b, "Подзадачи с одинаковым id должны быть равны");
        assertEquals(a.hashCode(), b.hashCode(), "Равные объекты должны иметь одинаковый hashCode");
    }

    // 4) Хранение и изменение epicId (модельный уровень)
    @Test
    void epicIdIsStoredAndCanBeChanged() {
        Subtask s = new Subtask("Name", "Desc", Status.NEW, 42);
        assertEquals(42, s.getEpicId(), "Изначальный epicId должен совпадать с переданным в конструктор");

        // Если модель допускает изменение epicId сеттером — проверим это.
        // (Если сеттера нет по ТЗ, удалите строки ниже.)
        s.setEpicId(99);
        assertEquals(99, s.getEpicId(), "После изменения epicId должен обновиться");
    }
}