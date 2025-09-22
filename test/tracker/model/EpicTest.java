package tracker.model;

/*
 * SPRINT-8: Тесты для модели Epic
 * --------------------------------
 * Проверяем:
 *  - начальное состояние без подзадач;
 *  - работу "прямых" сеттеров (set*Direct), которые менеджер вызывает при пересчёте;
 *  - базовые операции со списком id подзадач (add/remove/clear).
 *
 * ВАЖНО: агрегация статуса/времени из подзадач (бизнес-логика) тестируется в TaskManagerTest,
 * т.к. именно менеджер её пересчитывает (updateEpicStatus).
 */

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class EpicTest {

    private final LocalDateTime base = LocalDateTime.of(2025, 1, 1, 9, 0);

    // 1) Начальное состояние эпика без подзадач: статус NEW, поля времени не заданы
    @Test
    void ctor_withoutSubtasks_initialState_isNew_andNoTimes() {
        Epic epic = new Epic("E", "desc");

        assertEquals("E", epic.getTitle());
        assertEquals("desc", epic.getDescription());

        // По ТЗ статус эпика расчётный, по умолчанию — NEW у пустого эпика
        assertEquals(Status.NEW, epic.getStatus(), "Пустой эпик должен быть NEW");

        // Расчётные поля времени у пустого эпика не заданы
        assertNull(epic.getStartTime(), "startTime пустого эпика должен быть null");
        assertNull(epic.getDuration(),  "duration пустого эпика должен быть null");
        assertNull(epic.getEndTime(),   "endTime пустого эпика должен быть null");

        // И список подзадач изначально пуст
        assertTrue(epic.getSubtaskIds().isEmpty(), "У нового эпика не должно быть подзадач");
    }

    // 2) Прямые сеттеры времени/статуса корректно отражаются в геттерах
    @Test
    void directSetters_updateTimeAndStatus() {
        Epic epic = new Epic("E", "desc");

        LocalDateTime start = base.plusMinutes(15);
        LocalDateTime end   = base.plusMinutes(120);
        Duration dur        = Duration.ofMinutes(105);

        // Менеджер в рантайме будет вызывать эти методы после пересчёта
        epic.setStartTimeDirect(start);
        epic.setDurationDirect(dur);
        epic.setEndTimeDirect(end);
        epic.setStatusDirect(Status.IN_PROGRESS);

        assertEquals(start, epic.getStartTime());
        assertEquals(dur,   epic.getDuration());
        assertEquals(end,   epic.getEndTime());
        assertEquals(Status.IN_PROGRESS, epic.getStatus());
    }

    // 3) Сброс временных агрегатов через direct-сеттеры
    @Test
    void directSetters_canClearTimeAggregates() {
        Epic epic = new Epic("E", "desc");

        epic.setStartTimeDirect(base);
        epic.setDurationDirect(Duration.ofMinutes(30));
        epic.setEndTimeDirect(base.plusMinutes(30));

        // Сбрасываем
        epic.setStartTimeDirect(null);
        epic.setDurationDirect(null);
        epic.setEndTimeDirect(null);

        assertNull(epic.getStartTime());
        assertNull(epic.getDuration());
        assertNull(epic.getEndTime());
    }

    // 4) Операции со списком подзадач: add/remove/clear
    @Test
    void subtaskIdList_addRemoveClear() {
        Epic epic = new Epic("E", "desc");

        // Добавляем id подзадач
        epic.addSubtaskId(10);
        epic.addSubtaskId(20);
        epic.addSubtaskId(30);

        assertEquals(3, epic.getSubtaskIds().size());
        assertTrue(epic.getSubtaskIds().contains(10));
        assertTrue(epic.getSubtaskIds().contains(20));
        assertTrue(epic.getSubtaskIds().contains(30));

        // Удаляем один id
        epic.removeSubtaskId(20);
        assertEquals(2, epic.getSubtaskIds().size());
        assertFalse(epic.getSubtaskIds().contains(20));
        assertTrue(epic.getSubtaskIds().contains(10));
        assertTrue(epic.getSubtaskIds().contains(30));

        // Очищаем все id
        epic.clearSubtasks();
        assertTrue(epic.getSubtaskIds().isEmpty());
    }
}
