package tracker.controllers;

import org.junit.jupiter.api.Test;
import tracker.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Юнит-тесты сохранения и восстановления FileBackedTaskManager (CSV, история, nextId).
 *
 * Используем временные файлы (File.createTempFile) — они создаются
 * во временных каталогах операционной системы и помечаются deleteOnExit(), чтобы не засорять проект.
 */
class FileBackedTaskManagerTest {

    // ---------- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ----------

    /** Создаёт временный CSV-файл и помечает его на удаление при завершении JVM. */
    private File tempCsv(String prefix) throws IOException {
        File f = File.createTempFile(prefix, ".csv");
        f.deleteOnExit();
        return f;
    }

    private Task t(String title) { return new Task(title, "desc " + title, Status.NEW); }
    private Epic e(String title) { return new Epic(title, "desc " + title); }
    private Subtask s(String title, int epicId) { return new Subtask(title, "desc " + title, Status.NEW, epicId); }

    /** Максимальный id среди всех сущностей в менеджере — для проверки корректности nextId. */
    private int maxId(TaskManager m) {
        int max = 0;
        for (Task t : m.getAllTasks())   max = Math.max(max, t.getId());
        for (Epic e : m.getAllEpics())   max = Math.max(max, e.getId());
        for (Subtask s : m.getAllSubtasks()) max = Math.max(max, s.getId());
        return max;
    }

    // ---------- ТЕСТЫ ПО ТЗ ----------

    @Test
    void saveAndLoad_emptyFile_ok() throws IOException {
        // === 1) ПУСТОЙ ФАЙЛ ===
        // Идея: создать менеджер на файл, "сохранить пустое состояние" вызывая clear*,
        // затем загрузить из файла и убедиться, что все коллекции пусты, история пуста.
        File file = tempCsv("empty");

        // Менеджер с автосохранением на этот файл
        TaskManager mgr = Managers.getFileBacked(file);

        // Принудительно сохраняем пустое состояние (clear* дергают save())
        mgr.clearTasks();
        mgr.clearSubtasks();
        mgr.clearEpics();

        assertTrue(file.exists(), "CSV должен быть создан");
        List<String> lines = Files.readAllLines(file.toPath());
        assertFalse(lines.isEmpty(), "CSV не должен быть полностью пустым");
        assertEquals("id,type,name,status,description,epic", lines.get(0), "Ожидаем корректный заголовок CSV");

        // Загружаем обратно
        FileBackedTaskManager restored = FileBackedTaskManager.loadFromFile(file);

        assertTrue(restored.getAllTasks().isEmpty(), "Нет задач");
        assertTrue(restored.getAllEpics().isEmpty(), "Нет эпиков");
        assertTrue(restored.getAllSubtasks().isEmpty(), "Нет подзадач");
        assertTrue(restored.getHistory().isEmpty(), "История пуста");
    }

    @Test
    void save_multipleTasks_createsNonEmptyCsv() throws IOException {
        // === 2) СОХРАНЕНИЕ НЕСКОЛЬКИХ ЗАДАЧ ===
        // Добавляем задачи/эпики/подзадачи, инициирование просмотров для истории,
        // проверяем, что CSV-файл создан и не пустой.
        File file = tempCsv("save-multi");
        TaskManager m = Managers.getFileBacked(file);

        // 2 Task
        Task t1 = t("T1"); Task t2 = t("T2");
        m.addTask(t1); m.addTask(t2);

        // 1 Epic + 2 Subtask
        Epic e1 = e("E1"); m.addEpic(e1);
        Subtask s1 = s("S1", e1.getId()); m.addSubtask(s1);
        Subtask s2 = s("S2", e1.getId()); m.addSubtask(s2);

        // История просмотров: при save() записывается последней строкой
        m.getTask(t2.getId());
        m.getEpic(e1.getId());
        m.getSubtask(s1.getId());

        assertTrue(file.exists(), "CSV должен быть создан");
        assertTrue(file.length() > 0, "CSV не должен быть пустым");
    }

    @Test
    void load_multipleTasks_ok_and_nextIdContinues() throws IOException {
        // === 3) ЗАГРУЗКА НЕСКОЛЬКИХ ЗАДАЧ + ПРОВЕРКА nextId ===

        // Шаг 1: сохранить задачи/эпики/подзадачи в файл первым менеджером.
        // Шаг 2: загрузить файл вторым. Проверить размеры коллекций, связи, историю
        // и что новая задача получает id > max.

        File file = tempCsv("load-multi");

        // Сначала писатель
        TaskManager writer = Managers.getFileBacked(file);
        Task t1 = t("T1"); Task t2 = t("T2");
        writer.addTask(t1); writer.addTask(t2);

        Epic e1 = e("E1"); writer.addEpic(e1);
        Subtask s1 = s("S1", e1.getId()); writer.addSubtask(s1);
        Subtask s2 = s("S2", e1.getId()); writer.addSubtask(s2);

        // История (порядок важен: T1, затем S2)
        writer.getTask(t1.getId());
        writer.getSubtask(s2.getId());

        // Теперь читаем другим экземпляром
        FileBackedTaskManager restored = FileBackedTaskManager.loadFromFile(file);

        // Сущности загрузились
        assertEquals(2, restored.getAllTasks().size(), "Должно быть 2 задачи");
        assertEquals(1, restored.getAllEpics().size(), "Должен быть 1 эпик");
        assertEquals(2, restored.getAllSubtasks().size(), "Должно быть 2 подзадачи");

        // Связи эпика восстановились
        Epic reEpic = restored.getAllEpics().get(0); // один эпик — безопасно брать по индексу
        List<Integer> subIds = restored.getEpicSubtaskIds(reEpic.getId());
        assertEquals(2, subIds.size(), "У эпика должно быть 2 подзадачи");
        for (int sid : subIds) {
            assertNotNull(restored.getSubtask(sid), "Подзадача с id=" + sid + " должна существовать");
        }

        // История восстановилась и в правильном порядке
        List<Task> hist = restored.getHistory();
        assertEquals(2, hist.size(), "В истории должно быть 2 записи");
        assertEquals(t1.getId(), hist.get(0).getId(), "Сначала должен быть просмотр T1");
        assertEquals(s2.getId(), hist.get(1).getId(), "Затем просмотр S2");

        // nextId продолжился (новая задача получает id больше максимального из файла)
        int beforeMax = maxId(restored);
        Task after = t("after-restore");
        restored.addTask(after);
        assertTrue(after.getId() > beforeMax, "nextId должен быть больше максимального id из файла");
    }
}