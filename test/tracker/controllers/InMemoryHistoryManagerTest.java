/*
 * SPRINT-8: Тесты для InMemoryHistoryManager
 * ------------------------------------------
 * Проверяем:
 *
 * 1) emptyHistory_returnsEmptyList
 *    — Пустая история должна возвращать пустой список.
 *
 * 2) add_null_isIgnored_noCrash
 *    — Добавление null игнорируется (без NPE и изменений).
 *
 * 3) add_deduplicates_andMovesToTail
 *    — В истории нет дублей: повторный просмотр переносит задачу в КОНЕЦ,
 *      а старая копия вырезается за O(1) (поведение без проверки сложности).
 *
 * 4) remove_fromBeginning_middle_end
 *    — Удаление из начала/середины/конца корректно меняет порядок и состав истории.
 *
 * 5) remove_nonExisting_orFromEmpty_noCrash
 *    — Удаление несуществующего id и удаление из пустой истории ничего не ломает.
 *
 * 6) preservesOrder_oldestToNewest
 *    — Порядок истории — «от самой старой к самой новой».
 *
 * 7) worksWithDifferentTaskTypes
 *    — История хранит любые типы (Task/Epic/Subtask) и их порядок.
 *
 * 8) manyAdds_keepOnlyUnique_inProperOrder
 *    — Многоразовые добавления одинаковых id оставляют только уникальные элементы в конце,
 *      итоговый порядок соответствует последним «просмотрам».
 */

package tracker.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tracker.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryHistoryManagerTest {

    private HistoryManager history;

    @BeforeEach
    void setUp() {
        history = new InMemoryHistoryManager();
    }

    // ------------ helpers ------------

    private Task task(int id, String name) {
        Task t = new Task(name, "d", Status.NEW);
        t.setId(id);
        return t;
    }

    private Epic epic(int id, String name) {
        Epic e = new Epic(name, "d");
        e.setId(id);
        return e;
    }

    private Subtask subtask(int id, String name, int epicId) {
        Subtask s = new Subtask(name, "d", Status.NEW, epicId);
        s.setId(id);
        return s;
    }

    private static List<Integer> ids(List<Task> ts) {
        return ts.stream().map(Task::getId).collect(Collectors.toList());
    }

    private static void assertOrderByIds(List<Task> actual, int... expectedIds) {
        List<Integer> a = ids(actual);
        List<Integer> e = new ArrayList<>();
        for (int x : expectedIds) e.add(x);
        assertEquals(e, a, "Неожиданный порядок/состав истории");
    }

    // ---------------------------------
    // 1) Пустая история
    // ---------------------------------
    @Test
    void emptyHistory_returnsEmptyList() {
        assertTrue(history.getHistory().isEmpty(), "Пустая история должна быть пустой");
    }

    // ---------------------------------
    // 2) add(null) игнорируется
    // ---------------------------------
    @Test
    void add_null_isIgnored_noCrash() {
        assertDoesNotThrow(() -> history.add(null));
        assertTrue(history.getHistory().isEmpty(), "Добавление null не должно менять историю");
    }

    // ---------------------------------
    // 3) Дедупликация и перенос в хвост
    // ---------------------------------
    @Test
    void add_deduplicates_andMovesToTail() {
        Task a = task(1, "A");
        Task b = task(2, "B");
        Task c = task(3, "C");

        history.add(a);
        history.add(b);
        history.add(c);
        assertOrderByIds(history.getHistory(), 1, 2, 3);

        // Повторный просмотр B: старый узел B удаляется, B уходит в хвост
        history.add(b);
        assertOrderByIds(history.getHistory(), 1, 3, 2);

        // Повторный просмотр A: A переносится в хвост
        history.add(a);
        assertOrderByIds(history.getHistory(), 3, 2, 1);

        // Повторный просмотр C: C в хвост, дублей нет
        history.add(c);
        assertOrderByIds(history.getHistory(), 2, 1, 3);
    }

    // ---------------------------------
    // 4) Удаление: начало, середина, хвост
    // ---------------------------------
    @Test
    void remove_fromBeginning_middle_end() {
        Task a = task(1, "A");
        Task b = task(2, "B");
        Task c = task(3, "C");
        Task d = task(4, "D");

        history.add(a);
        history.add(b);
        history.add(c);
        history.add(d);
        assertOrderByIds(history.getHistory(), 1, 2, 3, 4);

        // Удаляем голову
        history.remove(1);
        assertOrderByIds(history.getHistory(), 2, 3, 4);

        // Удаляем из середины
        history.remove(3);
        assertOrderByIds(history.getHistory(), 2, 4);

        // Удаляем хвост
        history.remove(4);
        assertOrderByIds(history.getHistory(), 2);

        // Удаляем последний
        history.remove(2);
        assertTrue(history.getHistory().isEmpty(), "После удаления всех история должна быть пустой");
    }

    // ---------------------------------
    // 5) Удаление несуществующих/из пустой истории — без ошибки
    // ---------------------------------
    @Test
    void remove_nonExisting_orFromEmpty_noCrash() {
        assertDoesNotThrow(() -> history.remove(999));
        assertTrue(history.getHistory().isEmpty());

        Task a = task(1, "A");
        history.add(a);
        assertOrderByIds(history.getHistory(), 1);

        assertDoesNotThrow(() -> history.remove(777));
        assertOrderByIds(history.getHistory(), 1);

        history.remove(1);
        assertTrue(history.getHistory().isEmpty());
        assertDoesNotThrow(() -> history.remove(1)); // повторное удаление
    }

    // ---------------------------------
    // 6) Порядок: от старой записи к новой
    // ---------------------------------
    @Test
    void preservesOrder_oldestToNewest() {
        Task a = task(1, "A");
        Task b = task(2, "B");

        history.add(a); // старее
        history.add(b); // новее
        assertOrderByIds(history.getHistory(), 1, 2);

        // Повторный просмотр A переносит его в КОНЕЦ — теперь старее B
        history.add(a);
        assertOrderByIds(history.getHistory(), 2, 1);
    }

    // ---------------------------------
    // 7) Работает с разными типами задач
    // ---------------------------------
    @Test
    void worksWithDifferentTaskTypes() {
        Epic e = epic(10, "E");
        Subtask s = subtask(20, "S", e.getId());
        Task t = task(30, "T");

        history.add(e);
        history.add(s);
        history.add(t);

        assertOrderByIds(history.getHistory(), 10, 20, 30);

        // Дедупликация тоже работает для любых типов
        history.add(e);
        assertOrderByIds(history.getHistory(), 20, 30, 10);
    }

    // ---------------------------------
    // 8) Много добавлений: уникальные id остаются по одному, порядок соответствует последним просмотрам
    // ---------------------------------
    @Test
    void manyAdds_keepOnlyUnique_inProperOrder() {
        Task t1 = task(1, "1");
        Task t2 = task(2, "2");
        Task t3 = task(3, "3");
        Task t4 = task(4, "4");
        Task t5 = task(5, "5");

        // Первая «волна» просмотров
        history.add(t1);
        history.add(t2);
        history.add(t3);
        history.add(t4);
        history.add(t5);
        assertOrderByIds(history.getHistory(), 1, 2, 3, 4, 5);

        // Вторая «волна» тех же id (каждый переносится в хвост, дублей нет)
        history.add(t1); // [2,3,4,5,1]
        history.add(t2); // [3,4,5,1,2]
        history.add(t3); // [4,5,1,2,3]
        history.add(t4); // [5,1,2,3,4]
        history.add(t5); // [1,2,3,4,5] — снова исходный порядок
        assertOrderByIds(history.getHistory(), 1, 2, 3, 4, 5);
    }
}
