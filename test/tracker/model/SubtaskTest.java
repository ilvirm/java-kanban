package tracker.model;

/*
 * SPRINT-8: Тесты для модели Subtask
 * -----------------------------------
 * Проверяем:
 *
 * 1) Поля времени:
 *    - duration (Duration) — длительность в минутах.
 *    - startTime (LocalDateTime) — дата/время начала.
 *    - getEndTime() — вычисляемое окончание = startTime + duration.
 *
 * 2) Граничные случаи для времени:
 *    - Если startTime == null ИЛИ duration == null, то endTime == null.
 *
 * 3) Привязка к эпику:
 *    - epicId, переданный в конструктор, сохраняется как есть и доступен через getEpicId().
 *
 * Важно:
 *  — Здесь мы НЕ проверяем бизнес-правила менеджера (например, что сабтаск нельзя создать без существующего эпика
 *    или что сабтаск не может «принадлежать сам себе») — это ответственность TaskManager и тестируется отдельно.
 */

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class SubtaskTest {

    private final LocalDateTime base = LocalDateTime.of(2025, 1, 1, 9, 0);

    // 1) Конструктор с временем и epicId — все поля сохраняются, end = start + duration
    @Test
    void ctor_withTimeAndEpicId_preservesFields_andComputesEnd() {
        int epicId = 42;
        LocalDateTime start = base.plusMinutes(15);
        Duration dur = Duration.ofMinutes(45);

        Subtask s = new Subtask("Sub", "desc", Status.NEW, epicId, start, dur);

        assertEquals("Sub", s.getTitle());
        assertEquals("desc", s.getDescription());
        assertEquals(Status.NEW, s.getStatus());
        assertEquals(epicId, s.getEpicId());

        assertEquals(start, s.getStartTime());
        assertEquals(dur,   s.getDuration());
        assertEquals(start.plus(dur), s.getEndTime());
    }

    // 2) Если start == null, то end == null (даже при заданной duration)
    @Test
    void endTime_isNull_whenStartIsNull() {
        int epicId = 7;
        Subtask s = new Subtask("S", "d", Status.NEW, epicId, null, Duration.ofMinutes(30));

        assertNull(s.getStartTime());
        assertEquals(Duration.ofMinutes(30), s.getDuration());
        assertNull(s.getEndTime(), "Без startTime вычислять endTime нельзя");
    }

    // 3) Если duration == null, то end == null (даже при заданном start)
    @Test
    void endTime_isNull_whenDurationIsNull() {
        int epicId = 8;
        LocalDateTime start = base.plusMinutes(5);
        Subtask s = new Subtask("S", "d", Status.NEW, epicId, start, null);

        assertEquals(start, s.getStartTime());
        assertNull(s.getDuration());
        assertNull(s.getEndTime(), "Без duration вычислять endTime нельзя");
    }

    // 4) Конструктор без времени — поля времени null, epicId сохраняется
    @Test
    void ctor_withoutTime_keepsEpicId_andNullTimes() {
        int epicId = 101;
        Subtask s = new Subtask("Plain sub", "desc", Status.IN_PROGRESS, epicId);

        assertEquals(Status.IN_PROGRESS, s.getStatus());
        assertEquals(epicId, s.getEpicId());
        assertNull(s.getStartTime());
        assertNull(s.getDuration());
        assertNull(s.getEndTime());
    }
}
