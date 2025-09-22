package tracker.controllers;

import org.junit.jupiter.api.*;
import tracker.controllers.*;
import tracker.model.*;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Базовый абстрактный набор тестов для всех реализаций TaskManager.
 *
 * Покрывает требования спринта-8:
 *  - приоритетный список задач (getPrioritizedTasks) — сортировка по startTime, null-даты исключаются;
 *  - проверка пересечений интервалов при add/update — бросаем исключение и сохраняем прежнее состояние;
 *  - замена циклов на Stream API — проверяем getSubtasksOfEpic(...);
 *  - корректный пересчёт агрегатов Epic по времени (start/duration/end) и статусу;
 *  - базовые операции интерфейса TaskManager для задач/эпиков/подзадач.
 *
 * От этого класса наследуются конкретные тесты для InMemoryTaskManager и FileBackedTaskManager.
 */
public abstract class TaskManagerTest<T extends TaskManager> {

    protected T manager;

    /** Наследник должен вернуть конкретную реализацию менеджера. */
    protected abstract T createManager();

    /** Базовая «опорная» дата для построения временных интервалов в тестах. */
    protected final LocalDateTime base = LocalDateTime.of(2025, 1, 1, 9, 0);

    @BeforeEach
    void setUp() {
        manager = createManager();
    }

    // ---- helpers ------------------------------------------------------------
    /**
     * Утилита для создания Task:
     *  - startMin < 0  => startTime = null (задача без времени — важно для приоритизации в спринте-8);
     *  - durMin <= 0   => duration = null (нет длительности).
     */
    protected Task t(String name, int startMin, int durMin) {
        LocalDateTime st = startMin < 0 ? null : base.plusMinutes(startMin);
        Duration d = durMin <= 0 ? null : Duration.ofMinutes(durMin);
        return new Task(name, "desc", Status.NEW, st, d);
    }

    /** Утилита для Epic. */
    protected Epic e(String name) { return new Epic(name, "desc"); }

    /**
     * Утилита для Subtask:
     *  - те же правила по startMin/durMin, что и для Task;
     *  - связывает подзадачу с эпиком epicId.
     */
    protected Subtask s(String name, int epicId, int startMin, int durMin) {
        LocalDateTime st = startMin < 0 ? null : base.plusMinutes(startMin);
        Duration d = durMin <= 0 ? null : Duration.ofMinutes(durMin);
        return new Subtask(name, "desc", Status.NEW, epicId, st, d);
    }

    // -------- базовые проверки интерфейса ------------------------------------
    /**
     * Базовый happy-path: убедиться, что add/get по id работают для
     * Task, Epic и Subtask (основа для остальных тестов).
     */
    @Test
    void addsAndFindsTasksById() {
        Task task = t("T1", 0, 60);
        manager.addTask(task);
        Epic epic = e("E1");
        manager.addEpic(epic);
        Subtask sub = s("S1", epic.getId(), 60, 30);
        manager.addSubtask(sub);

        assertEquals(task, manager.getTask(task.getId()));
        assertEquals(epic, manager.getEpic(epic.getId()));
        assertEquals(sub, manager.getSubtask(sub.getId()));
    }

    /**
     * ТЗ (спринт-8): заменить циклы на Stream API.
     * Проверяем, что getSubtasksOfEpic(epicId) возвращает только подзадачи нужного эпика
     * и корректно работает со стримом.
     */
    @Test
    void getSubtasksOfEpic_usesStream_andReturnsOnlyIts() {
        Epic epicA = e("EA"); manager.addEpic(epicA);
        Epic epicB = e("EB"); manager.addEpic(epicB);

        Subtask sa1 = s("SA1", epicA.getId(), 0, 10);  manager.addSubtask(sa1);
        Subtask sa2 = s("SA2", epicA.getId(), 10, 10); manager.addSubtask(sa2);
        Subtask sb1 = s("SB1", epicB.getId(), 20, 10); manager.addSubtask(sb1);

        var ofA = manager.getSubtasksOfEpic(epicA.getId());
        assertEquals(2, ofA.size());
        assertTrue(ofA.containsAll(List.of(sa1, sa2)));
        assertFalse(ofA.contains(sb1));
    }

    // -------- приоритизация --------------------------------------------------
    /**
     * ТЗ (спринт-8): getPrioritizedTasks — сортировка по startTime возрастанию,
     * записи без startTime НЕ попадают в список приоритета.
     */
    @Test
    void getPrioritizedTasks_sortedByStartTime_nullsExcluded() {
        Task t1 = t("T1", 60, 30);      // 10:00
        Task t2 = t("T2", 10, 30);      // 09:10
        Task t3 = t("T3", -1, 45);      // startTime=null => не участвует

        manager.addTask(t1);
        manager.addTask(t2);
        manager.addTask(t3);

        var pr = manager.getPrioritizedTasks();
        assertEquals(List.of(t2, t1), pr, "Ожидался порядок по startTime, записи без времени — вне списка");
        assertFalse(pr.contains(t3), "Задача без startTime не должна участвовать в приоритизации");
    }

    // -------- пересечения ----------------------------------------------------
    /**
     * ТЗ (спринт-8): запрещены пересекающиеся по времени задачи/подзадачи.
     * При добавлении пересекающейся задачи — IllegalStateException.
     */
    @Test
    void addTask_overlaps_shouldThrow() {
        Task a = t("A", 0, 60);     // [09:00, 10:00)
        Task b = t("B", 30, 30);    // [09:30, 10:00) пересекается

        manager.addTask(a);
        assertThrows(IllegalStateException.class, () -> manager.addTask(b),
                "Должно быть исключение при добавлении пересекающейся задачи");
    }

    /**
     * ТЗ (спринт-8): при update тоже должна быть проверка пересечения;
     * если новое состояние конфликтует — кидаем исключение и ОТМЕНЯЕМ изменение,
     * оставляя старую версию в менеджере.
     */
    @Test
    void updateTask_toOverlap_shouldThrowAndKeepOld() {
        Task a = t("A", 0, 60);     // [09:00, 10:00)
        Task c = t("C", 70, 30);    // [10:10, 10:40) — не пересекается

        manager.addTask(a);
        manager.addTask(c);

        // Сделаем C пересекающейся с A: [09:50, 10:20)
        Task cNew = new Task("C", "desc", Status.NEW, base.plusMinutes(50), Duration.ofMinutes(30));
        cNew.setId(c.getId());
        assertThrows(IllegalStateException.class, () -> manager.updateTask(cNew),
                "Должно быть исключение при обновлении в пересечение");

        // старая версия должна остаться неизменной
        assertEquals(base.plusMinutes(70), manager.getTask(c.getId()).getStartTime());
    }

    // -------- статус и время эпика ------------------------------------------
    /**
     * Граничный случай (а): все подзадачи NEW => Epic.NEW
     */
    @Test
    void epicStatus_allNew() {
        Epic epic = e("E"); manager.addEpic(epic);
        manager.addSubtask(new Subtask("S1", "d", Status.NEW, epic.getId()));
        manager.addSubtask(new Subtask("S2", "d", Status.NEW, epic.getId()));
        assertEquals(Status.NEW, manager.getEpic(epic.getId()).getStatus());
    }

    /**
     * Граничный случай (b): все подзадачи DONE => Epic.DONE
     */
    @Test
    void epicStatus_allDone() {
        Epic epic = e("E"); manager.addEpic(epic);
        var s1 = new Subtask("S1", "d", Status.DONE, epic.getId());
        var s2 = new Subtask("S2", "d", Status.DONE, epic.getId());
        manager.addSubtask(s1); manager.addSubtask(s2);
        assertEquals(Status.DONE, manager.getEpic(epic.getId()).getStatus());
    }

    /**
     * Граничный случай (c): есть NEW и DONE => Epic.IN_PROGRESS
     */
    @Test
    void epicStatus_mixedNewAndDone() {
        Epic epic = e("E"); manager.addEpic(epic);
        manager.addSubtask(new Subtask("S1", "d", Status.NEW, epic.getId()));
        manager.addSubtask(new Subtask("S2", "d", Status.DONE, epic.getId()));
        assertEquals(Status.IN_PROGRESS, manager.getEpic(epic.getId()).getStatus());
    }

    /**
     * Граничный случай (d): есть хотя бы одна IN_PROGRESS => Epic.IN_PROGRESS
     */
    @Test
    void epicStatus_hasInProgress() {
        Epic epic = e("E"); manager.addEpic(epic);
        manager.addSubtask(new Subtask("S1", "d", Status.IN_PROGRESS, epic.getId()));
        assertEquals(Status.IN_PROGRESS, manager.getEpic(epic.getId()).getStatus());
    }

    /**
     * Агрегаты времени эпика (спринт-8): start = мин. старт подзадач,
     * end = макс. окончание, duration = сумма длительностей.
     */
    @Test
    void epicTimeAggregates_bySubtasks() {
        Epic epic = e("E"); manager.addEpic(epic);
        // SA: [09:10, 09:40), SB: [10:00, 10:30)
        manager.addSubtask(s("SA", epic.getId(), 10, 30));
        manager.addSubtask(s("SB", epic.getId(), 60, 30));

        Epic eAfter = manager.getEpic(epic.getId());
        assertEquals(base.plusMinutes(10), eAfter.getStartTime());
        assertEquals(base.plusMinutes(90), eAfter.getEndTime());
        assertEquals(Duration.ofMinutes(60), eAfter.getDuration());
    }
}
