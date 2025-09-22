/*
 * SPRINT-8: Тесты для InMemoryTaskManager
 * ---------------------------------------
 * Этот класс — «тонкий» адаптер над базовым TaskManagerTest<T>.
 *
 * 1) Мы наследуемся от TaskManagerTest<InMemoryTaskManager>, чтобы получить ВСЕ
 *    базовые проверки спринта-8:
 *    - добавление/получение Task/Epic/Subtask;
 *    - getSubtasksOfEpic(...) переписан на Stream API;
 *    - getPrioritizedTasks(): сортировка по startTime, записи без времени не участвуют;
 *    - проверка пересечений (add и update бросают исключение при конфликте);
 *    - расчёт агрегатов эпика (status/start/duration/end) по сабтаскам.
 *
 * 2) Дополнительно здесь есть два узкоспецифичных теста InMemoryTaskManager:
 *    - prioritizedDoesNotContainEpics(): эпики не попадают в приоритетный список;
 *    - hasOverlaps_ignoresTasksWithoutTime(): задачи без времени не считаются конфликтными
 *      и допускаются к добавлению; они не попадают в приоритетный список.
 */

package tracker.controllers;

import org.junit.jupiter.api.Test;
import tracker.model.Epic;
import tracker.model.Status;
import tracker.model.Subtask;
import tracker.model.Task;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryTaskManagerTest extends TaskManagerTest<InMemoryTaskManager> {

    /** Подкладываем конкретную реализацию менеджера в общий набор тестов. */
    @Override
    protected InMemoryTaskManager createManager() {
        return new InMemoryTaskManager();
    }

    /**
     * SPRINT-8: Приоритизированный список не должен содержать эпики.
     *
     * Почему: приоритизация — это «план исполняемых работ», туда попадают Task и Subtask.
     * Эпик — агрегирующая сущность; у неё расчётные поля времени, но сама она не исполняется.
     */
    @Test
    void prioritizedDoesNotContainEpics() {
        Epic epic = new Epic("E", "desc");
        manager.addEpic(epic);

        // Добавим сабтаски, чтобы у эпика посчитались агрегаты времени:
        Subtask s1 = new Subtask("S1", "d", Status.NEW, epic.getId(),
                base.plusMinutes(10), Duration.ofMinutes(30));
        Subtask s2 = new Subtask("S2", "d", Status.NEW, epic.getId(),
                base.plusMinutes(60), Duration.ofMinutes(30));
        manager.addSubtask(s1);
        manager.addSubtask(s2);

        List<Task> pr = manager.getPrioritizedTasks();
        assertTrue(pr.containsAll(List.of(s1, s2)), "Сабтаски должны быть в приоритизированном списке");
        assertFalse(pr.contains(epic), "Эпик не должен попадать в приоритизированный список");
    }

    /**
     * SPRINT-8: Задачи без времени не считаются пересекающимися и допускаются к добавлению.
     *
     * Проверяем два момента:
     *  1) hasOverlaps(...) для задачи без startTime/duration возвращает false;
     *  2) addTask(...) не бросает при добавлении такой задачи;
     *  3) задача без времени НЕ попадает в приоритетный список.
     */
    @Test
    void hasOverlaps_ignoresTasksWithoutTime() {
        // Базовая «занятая» задача со временем: [09:00, 10:00)
        Task a = new Task("A", "with time", Status.NEW, base, Duration.ofMinutes(60));
        manager.addTask(a);

        // Кандидат без времени (startTime == null). duration можно не задавать вовсе — логика игнорирует любую «пустоту».
        Task b = new Task("B", "no time", Status.NEW, null, null);

        assertFalse(manager.hasOverlaps(b), "Задача без времени не должна считаться пересекающейся");
        assertDoesNotThrow(() -> manager.addTask(b), "Добавление задачи без времени не должно кидать исключение");

        // В приоритизации остаётся только «A» (у неё есть startTime). «B» в список не попадает.
        List<Task> pr = manager.getPrioritizedTasks();
        assertEquals(List.of(a), pr, "Задача без времени не должна участвовать в приоритизации");
    }
}