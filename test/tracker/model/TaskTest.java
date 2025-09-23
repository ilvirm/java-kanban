package tracker.model;

/*
 * SPRINT-8: Тесты для модели Task
 * --------------------------------
 * Проверяем:
 *
 * 1) Поля времени:
 *    - duration (Duration) — длительность в минутах.
 *    - startTime (LocalDateTime) — дата/время начала.
 *    - getEndTime() — вычисляемое окончание = startTime + duration.
 *
 * 2) Граничные случаи:
 *    - Если startTime == null ИЛИ duration == null, то endTime == null.
 *    - Значения полей, переданные в конструктор, сохраняются как есть.
 *
 * Примечание:
 *  — Здесь тестируем только модель Task (не менеджер).
 *  — Приоритизация/пересечения проверяются в TaskManagerTest.
 */

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class TaskTest {

    private final LocalDateTime base = LocalDateTime.of(2025, 1, 1, 9, 0);

    // 1) end = start + duration, когда оба заданы
    @Test
    void endTime_isStartPlusDuration_whenBothSet() {
        LocalDateTime start = base;
        Duration dur = Duration.ofMinutes(90);
        Task t = new Task("T", "d", Status.NEW, start, dur);

        assertEquals(start, t.getStartTime());
        assertEquals(dur,   t.getDuration());
        assertEquals(base.plusMinutes(90), t.getEndTime());
    }

    // 2) Если start == null, то end == null (даже при ненулевой duration)
    @Test
    void endTime_null_whenStartIsNull() {
        Task t = new Task("T", "d", Status.NEW, null, Duration.ofMinutes(30));
        assertNull(t.getStartTime());
        assertEquals(Duration.ofMinutes(30), t.getDuration());
        assertNull(t.getEndTime());
    }

    // 3) Если duration == null, то end == null (даже при заданном start)
    @Test
    void endTime_null_whenDurationIsNull() {
        Task t = new Task("T", "d", Status.NEW, base.plusMinutes(10), null);
        assertEquals(base.plusMinutes(10), t.getStartTime());
        assertNull(t.getDuration());
        assertNull(t.getEndTime());
    }

    // 4) Конструктор без времени — поля времени остаются null, статус сохраняется
    @Test
    void ctor_withoutTime_keepsStatus_andNullTimes() {
        Task t = new Task("Plain", "desc", Status.IN_PROGRESS);
        assertEquals(Status.IN_PROGRESS, t.getStatus());
        assertNull(t.getStartTime());
        assertNull(t.getDuration());
        assertNull(t.getEndTime());
    }

    // 5) Конструктор с временем — все переданные значения сохраняются
    @Test
    void ctor_withTime_preservesAllFields() {
        String title = "Write tests";
        String desc  = "Sprint-8 time fields";
        Status st    = Status.NEW;
        LocalDateTime start = base.plusMinutes(25);
        Duration dur = Duration.ofMinutes(45);

        Task t = new Task(title, desc, st, start, dur);

        assertEquals(title, t.getTitle());
        assertEquals(desc,  t.getDescription());
        assertEquals(st,    t.getStatus());
        assertEquals(start, t.getStartTime());
        assertEquals(dur,   t.getDuration());
        assertEquals(start.plus(dur), t.getEndTime());
    }
}
