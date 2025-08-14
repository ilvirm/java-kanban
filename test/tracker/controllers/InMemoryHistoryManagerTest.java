/*
 * ТЕСТЫ ДЛЯ InMemoryHistoryManager
 * --------------------------------
 * Порядок и содержание проверок (соответствует порядку тестов в коде):
 *
 * 1) managersGetDefaultHistory_returnsInitializedInstance
 *    - Проверяет, что Managers.getDefaultHistory() возвращает проинициализированный экземпляр менеджера,
 *      и начальная история пуста.
 *
 * 2) addSameTaskTwice_movesToTail_withoutDuplicates
 *    - Повторный просмотр той же задачи НЕ создаёт дубль, а переносит задачу в хвост.
 *    - В истории остаётся 1 запись с актуальным состоянием задачи.
 *
 * 3) getHistoryReturnsCorrectOrderAndSize_withoutDuplicates
 *    - Порядок просмотров соответствует вызовам add.
 *    - Повторный add для существующей задачи переносит её в конец (хвост),
 *      итерация даёт порядок [другие, ... , повторённая_задача].
 *
 * 4) removeById_handlesHeadMiddleTail_andIsIdempotent
 *    - Удаление по id корректно обрабатывает все позиции: голову, середину, хвост.
 *    - Повторный remove по тому же id не меняет состояние (идемпотентность).
 *
 * 5) remove_nonExistingId_doesNothing — удаление несуществующего id не должно менять историю.
 * 6) add_null_ignored — add(null) игнорируется, история не меняется.
 * 7) remove_onlyElement_leavesHistoryEmpty — удаление единственного элемента обнуляет историю.
 */

package tracker.controllers;

import org.junit.jupiter.api.Test;
import tracker.model.Status;
import tracker.model.Task;

import java.util.List;
import java.util.stream.Collectors; // для Java 11 совместимости (вместо .toList())

import static org.junit.jupiter.api.Assertions.*;

class InMemoryHistoryManagerTest {

    // 1) Инициализация через Managers
    @Test
    void managersGetDefaultHistory_returnsInitializedInstance() {
        HistoryManager historyManager = Managers.getDefaultHistory();
        assertNotNull(historyManager, "Managers.getDefaultHistory() должен вернуть не-null экземпляр");
        assertTrue(historyManager.getHistory().isEmpty(), "Новая история должна быть пустой");
    }

    // 2) Повторный просмотр: без дублей, перенос в хвост, актуализация состояния
    @Test
    void addSameTaskTwice_movesToTail_withoutDuplicates() {
        HistoryManager historyManager = Managers.getDefaultHistory();

        Task task1 = new Task("Task1", "Test add", Status.NEW);
        task1.setId(1); // Важно: id уникален до add()

        historyManager.add(task1);
        // Меняем состояние и снова добавляем — ожидается перенос в хвост без дублей
        task1.setStatus(Status.IN_PROGRESS);
        historyManager.add(task1);

        List<Task> history = historyManager.getHistory();

        assertEquals(1, history.size(), "История должна содержать 1 запись (без дублей)");
        assertEquals(1, history.get(0).getId(), "В истории должна быть та же задача по id");
        assertEquals(Status.IN_PROGRESS, history.get(0).getStatus(),
                "Повторное добавление должно актуализировать единственную запись");
    }

    // 3) Порядок просмотров и перенос при повторе
    @Test
    void getHistoryReturnsCorrectOrderAndSize_withoutDuplicates() {
        HistoryManager historyManager = Managers.getDefaultHistory();

        Task task1 = new Task("Task1", "First", Status.NEW);          task1.setId(1);
        Task task2 = new Task("Task2", "Second", Status.IN_PROGRESS); task2.setId(2);

        historyManager.add(task1); // [1]
        historyManager.add(task2); // [1,2]
        historyManager.add(task1); // повторный просмотр 1 → [2,1]

        List<Task> history = historyManager.getHistory();

        assertEquals(2, history.size(), "История должна содержать 2 уникальные задачи");
        assertEquals(2, history.get(0).getId(), "После повторного add(1) первой должна быть 2");
        assertEquals(1, history.get(1).getId(), "Задача 1 должна быть перенесена в хвост");
    }

    // 4) Удаление: голова/середина/хвост + идемпотентность
    @Test
    void removeById_handlesHeadMiddleTail_andIsIdempotent() {
        HistoryManager historyManager = Managers.getDefaultHistory();

        Task t1 = new Task("T1", "H", Status.NEW); t1.setId(1);
        Task t2 = new Task("T2", "M", Status.NEW); t2.setId(2);
        Task t3 = new Task("T3", "T", Status.NEW); t3.setId(3);

        historyManager.add(t1);
        historyManager.add(t2);
        historyManager.add(t3); // [1,2,3]

        // Удаляем голову
        historyManager.remove(1); // [2,3]
        assertEquals(
                List.of(2, 3),
                historyManager.getHistory().stream().map(Task::getId).collect(Collectors.toList()),
                "После remove(1) должно остаться [2,3]"
        );

        // Удаляем середину
        historyManager.add(t1); // [2,3,1]
        historyManager.remove(3); // [2,1]
        assertEquals(
                List.of(2, 1),
                historyManager.getHistory().stream().map(Task::getId).collect(Collectors.toList()),
                "После remove(3) должно остаться [2,1]"
        );

        // Удаляем хвост
        historyManager.remove(1); // [2]
        assertEquals(
                List.of(2),
                historyManager.getHistory().stream().map(Task::getId).collect(Collectors.toList()),
                "После remove(1) должно остаться [2]"
        );

        // Идемпотентность: повторный remove не должен менять состояние
        historyManager.remove(1); // всё ещё [2]
        assertEquals(
                List.of(2),
                historyManager.getHistory().stream().map(Task::getId).collect(Collectors.toList()),
                "Повторный remove(1) не должен изменять историю"
        );
    }

    // 5) Edge: удаление несуществующего id не меняет историю
    @Test
    void remove_nonExistingId_doesNothing() {
        HistoryManager historyManager = Managers.getDefaultHistory();
        Task t = new Task("T", "X", Status.NEW); t.setId(10);
        historyManager.add(t);
        historyManager.remove(999);
        assertEquals(1, historyManager.getHistory().size());
    }

    // 6) Edge: add(null) игнорируется
    @Test
    void add_null_ignored() {
        HistoryManager historyManager = Managers.getDefaultHistory();
        historyManager.add(null);
        assertTrue(historyManager.getHistory().isEmpty());
    }

    // 7) Edge: удаление единственного элемента обнуляет историю
    @Test
    void remove_onlyElement_leavesHistoryEmpty() {
        HistoryManager historyManager = Managers.getDefaultHistory();
        Task t = new Task("Only", "One", Status.NEW); t.setId(42);
        historyManager.add(t);
        historyManager.remove(42);
        assertTrue(historyManager.getHistory().isEmpty());
    }
}