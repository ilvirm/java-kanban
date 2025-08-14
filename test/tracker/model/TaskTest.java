/*
 * ТЕСТЫ ДЛЯ Task
 * ----------------------------------
 * Порядок проверок (совпадает с порядком тестов ниже):
 *
 * 1) tasksWithSameIdShouldBeEqual
 *    - Две задачи с одинаковым id считаются равными (equals по id).
 *
 * 2) tasksWithDifferentIdsShouldNotBeEqual
 *    - Задачи с разными id не равны, даже если остальные поля совпадают.
 *
 * 3) equalsHashCodeContract
 *    - Контракт equals/hashCode: одинаковые id → equals == true и одинаковый hashCode.
 *
 * 4) changingNonIdFieldsDoesNotAffectEquality
 *    - Изменение полей, не связанных с id (title/description/status),
 *      не влияет на равенство, если equals основан только на id.
 */

package tracker.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TaskTest {

    // 1) Равенство по одинаковому id
    @Test
    void tasksWithSameIdShouldBeEqual() {
        Task t1 = new Task("Test 1", "Description 1", Status.NEW);
        Task t2 = new Task("Test 2", "Description 2", Status.IN_PROGRESS);

        t1.setId(100);
        t2.setId(100);

        assertEquals(t1, t2, "Задачи с одинаковыми id должны быть равны");
    }

    // 2) Неравенство при разных id (даже если остальные поля совпадают)
    @Test
    void tasksWithDifferentIdsShouldNotBeEqual() {
        Task t1 = new Task("Same", "Same", Status.NEW);
        Task t2 = new Task("Same", "Same", Status.NEW);

        t1.setId(1);
        t2.setId(2);

        assertNotEquals(t1, t2, "Задачи с разными id не должны быть равны");
    }

    // 3) Контракт equals/hashCode
    @Test
    void equalsHashCodeContract() {
        Task a = new Task("A", "D", Status.NEW);
        Task b = new Task("B", "E", Status.DONE);

        a.setId(777);
        b.setId(777);

        assertEquals(a, b, "Задачи с одинаковым id должны быть равны");
        assertEquals(a.hashCode(), b.hashCode(), "Равные объекты должны иметь одинаковый hashCode");
    }

    // 4) Изменение не-id полей не влияет на равенство (если equals основан на id)
    @Test
    void changingNonIdFieldsDoesNotAffectEquality() {
        Task t1 = new Task("Title", "Desc", Status.NEW);
        Task t2 = new Task("Title", "Desc", Status.NEW);

        t1.setId(42);
        t2.setId(42);

        // Меняем любые поля, кроме id
        t2.setTitle("Another title");
        t2.setDescription("Another description");
        t2.setStatus(Status.IN_PROGRESS);

        assertEquals(t1, t2,
                "Если equals у Task основан только на id, изменение не-id полей не должно влиять на равенство");
    }
}