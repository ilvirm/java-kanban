/*
 * SPRINT-8: Тесты для FileBackedTaskManager
 * -----------------------------------------
 * Этот класс расширяет общий набор тестов из TaskManagerTest<FileBackedTaskManager>
 * (там уже проверяются: базовые CRUD, Stream API в getSubtasksOfEpic, приоритизация,
 * пересечения, и агрегаты эпика).
 *
 * Здесь добавляем ИМЕННО ФАЙЛОВЫЕ проверки, важные для спринта-8:
 *
 * 1) roundTrip_persistsTimeFieldsAndHistory
 *    — Поля времени (startTime/duration) у Task/Subtask корректно сохраняются и загружаются,
 *      приоритизация после загрузки работает, история просмотров восстанавливается.
 *
 * 2) load_recomputesEpicAggregates_ignoresCsv
 *    — По ТЗ: после загрузки эпики пересчитываются по сабтаскам, игнорируя значения,
 *      записанные в CSV (включая статус и поля времени). Проверяем, что «битый» статус
 *      у эпика без сабтасков заменяется на корректный NEW, а time-агрегаты становятся null.
 *
 * 3) load_malformedCsv_throwsManagerSaveException
 *    — Корректный перехват ошибок разбора (в т.ч. через catch(RuntimeException) в загрузчике).
 *      Любая поломка CSV должна оборачиваться в ManagerSaveException.
 *
 * Дополнительно:
 *  - В каждом тесте используем отдельный временный файл, чтобы не пересекаться между тестами.
 */

package tracker.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tracker.exceptions.ManagerSaveException;
import tracker.model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {

    private Path tmp;      // путь к временному файлу текущего теста
    private File csvFile;  // сам файл (нужен в конструкторе менеджера)

    /** Конкретная реализация менеджера для базовых тестов из TaskManagerTest. */
    @Override
    protected FileBackedTaskManager createManager() {
        try {
            tmp = Files.createTempFile("fbm_sprint8_", ".csv");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        csvFile = tmp.toFile();
        return new FileBackedTaskManager(csvFile);
    }

    /** После каждого теста удаляем временный файл. */
    @AfterEach
    void cleanup() throws IOException {
        if (tmp != null) {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------------------------------------------------------------
    // 1) Раунд-трип: время, приоритизация и история корректно переносятся
    // ---------------------------------------------------------------------

    /**
     * SPRINT-8: Проверяем полный цикл «сохрани → загрузи»:
     *  - Task/Subtask сохраняют и восстанавливают startTime и duration;
     *  - getPrioritizedTasks() после загрузки корректно отсортирован и НЕ содержит эпиков;
     *  - история просмотров восстанавливается в исходном порядке.
     */
    @Test
    void roundTrip_persistsTimeFieldsAndHistory() {
        // Task: [09:10, 09:40)
        Task t = new Task("T", "d", Status.NEW, base.plusMinutes(10), Duration.ofMinutes(30));
        manager.addTask(t);

        // Epic + Subtask: [10:00, 10:45)
        Epic e = new Epic("E", "d");
        manager.addEpic(e);
        Subtask s = new Subtask("S", "d", Status.NEW, e.getId(),
                base.plusMinutes(60), Duration.ofMinutes(45));
        manager.addSubtask(s);

        // Сформируем историю (порядок: Task -> Epic -> Subtask).
        manager.getTask(t.getId());
        manager.getEpic(e.getId());
        manager.getSubtask(s.getId());
        // (FileBackedTaskManager автосохраняет на каждом изменении/просмотре.)

        // Загружаем из того же файла новый менеджер.
        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(csvFile);

        // --- Поля времени сохранились? ---
        Task tL = loaded.getTask(t.getId());
        Subtask sL = loaded.getSubtask(s.getId());
        assertNotNull(tL);
        assertNotNull(sL);
        assertEquals(t.getStartTime(), tL.getStartTime(), "startTime Task должен сохраниться");
        assertEquals(t.getDuration(),  tL.getDuration(),  "duration Task должен сохраниться");
        assertEquals(s.getStartTime(), sL.getStartTime(), "startTime Subtask должен сохраниться");
        assertEquals(s.getDuration(),  sL.getDuration(),  "duration Subtask должен сохраниться");

        // --- Приоритизация после загрузки корректна: порядок по startTime, без эпиков ---
        List<Task> pr = loaded.getPrioritizedTasks();
        assertEquals(List.of(tL, sL), pr, "Ожидался порядок по времени (Task раньше Subtask)");
        assertFalse(pr.contains(loaded.getEpic(e.getId())), "Эпик не должен попадать в приоритизированный список");

        // --- История восстановилась в исходном порядке ---
        List<Task> hist = loaded.getHistory();
        assertEquals(List.of(tL, loaded.getEpic(e.getId()), sL), hist,
                "История просмотров должна восстановиться в исходном порядке");
    }

    // ---------------------------------------------------------------------
    // 2) Пересчёт эпиков при загрузке: игнорируем любые значения в CSV
    // ---------------------------------------------------------------------

    /**
     * SPRINT-8: По ТЗ после загрузки статус/время эпика должны пересчитаться.
     * Эмуляция: пишем CSV руками с EPIC без сабтасков, но статус = DONE (некорректно).
     * Ожидание: после loadFromFile эпик станет NEW, а start/duration/end = null.
     */
    @Test
    void load_recomputesEpicAggregates_ignoresCsv() throws IOException {
        // Пишем свой CSV, НЕ через save(), чтобы сознательно занести "битый" статус у эпика.
        String header = "id,type,name,status,description,startTime,durationMinutes,epicId";
        // id=1, EPIC, статус "DONE", пустые поля времени
        String epicCsv = "1,EPIC,MyEpic,DONE,desc,,,";
        String all = header + System.lineSeparator()
                + epicCsv + System.lineSeparator()
                + System.lineSeparator(); // пустая строка-разделитель, история отсутствует

        Files.writeString(tmp, all, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(csvFile);
        Epic e = loaded.getEpic(1);
        assertNotNull(e, "Эпик должен быть загружен");
        assertEquals(Status.NEW, e.getStatus(), "Эпик без сабтасков должен стать NEW при пересчёте");
        assertNull(e.getStartTime(), "У эпика без сабтасков startTime должен быть null");
        assertNull(e.getDuration(),  "У эпика без сабтасков duration должен быть null");
        assertNull(e.getEndTime(),   "У эпика без сабтасков endTime должен быть null");
    }

    // ---------------------------------------------------------------------
    // 3) Поломанный CSV => ManagerSaveException (обёртка на любые runtime-ошибки)
    // ---------------------------------------------------------------------

    /**
     * SPRINT-8: Любая ошибка разбора CSV должна приводить к ManagerSaveException
     * (в т.ч. благодаря catch(RuntimeException) внутри loadFromFile).
     *
     * Эмуляция: строка с недостаточным числом столбцов.
     */
    @Test
    void load_malformedCsv_throwsManagerSaveException() throws IOException {
        String header = "id,type,name,status,description,startTime,durationMinutes,epicId";
        // Мало столбцов — парсер обязан упасть
        String badLine = "42,TASK,Name,NEW";
        String all = header + System.lineSeparator()
                + badLine + System.lineSeparator()
                + System.lineSeparator();

        Files.writeString(tmp, all, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

        assertThrows(ManagerSaveException.class,
                () -> FileBackedTaskManager.loadFromFile(csvFile),
                "Поломанный CSV должен приводить к ManagerSaveException");
    }
}
