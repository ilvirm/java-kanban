package tracker.controllers;

import tracker.exceptions.ManagerSaveException;
import tracker.model.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import java.time.Duration;                       // --- NEW (SPRINT-8)
import java.time.LocalDateTime;                  // --- NEW (SPRINT-8)
import java.time.format.DateTimeFormatter;       // --- NEW (SPRINT-8)


/**
 * Менеджер с автосохранением в CSV:
 * <p>
 * Формат:
 * 1-я строка: заголовок
 * Далее: задачи (TASK/EPIC/SUBTASK)
 * Пустая строка
 * Последняя строка: история просмотров: id,id,id
 */
public class FileBackedTaskManager extends InMemoryTaskManager {

    private static final String HEADER =
            "id,type,name,status,description,startTime,durationMinutes,epicId";   // --- NEW (SPRINT-8)

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");   // SPRINT-8

    private final File file;

    public FileBackedTaskManager(File file) {
        this.file = Objects.requireNonNull(file, "file is null");
    }

    // ---------------- Добавляем автосохранение для каждого изменения + просмотр (история) -----------------

    // TASK
    @Override
    public void addTask(Task task) {
        super.addTask(task);
        save();
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void removeTask(int id) {
        super.removeTask(id);
        save();
    }

    @Override
    public void clearTasks() {
        super.clearTasks();
        save();
    }

    // EPIC
    @Override
    public void addEpic(Epic epic) {
        super.addEpic(epic);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void removeEpic(int id) {
        super.removeEpic(id);
        save();
    }

    @Override
    public void clearEpics() {
        super.clearEpics();
        save();
    }

    // SUBTASK
    @Override
    public void addSubtask(Subtask subtask) {
        super.addSubtask(subtask);
        save();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void removeSubtask(int id) {
        super.removeSubtask(id);
        save();
    }

    @Override
    public void clearSubtasks() {
        super.clearSubtasks();
        save();
    }

    // Просмотры тоже сохраняем, чтобы история не терялась
    @Override
    public Task getTask(int id) {
        Task t = super.getTask(id);
        if (t != null) save();
        return t;
    }

    @Override
    public Epic getEpic(int id) {
        Epic e = super.getEpic(id);
        if (e != null) save();
        return e;
    }

    @Override
    public Subtask getSubtask(int id) {
        Subtask s = super.getSubtask(id);
        if (s != null) save();
        return s;
    }

    // ----------------- Сохранение задач в CSV-файл -----------------

    protected void save() {
        try (BufferedWriter bw = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            bw.write(HEADER);
            bw.newLine();

            // порядок: Tasks -> Epics -> Subtasks (не критично, но читаемо)
            for (Task t : getAllTasks()) {
                bw.write(taskToCsv(t));
                bw.newLine();
            }
            for (Epic e : getAllEpics()) {
                bw.write(epicToCsv(e));
                bw.newLine();
            }
            for (Subtask s : getAllSubtasks()) {
                bw.write(subtaskToCsv(s));
                bw.newLine();
            }

            bw.newLine(); // раздел пустой строкой

            // история: id,id,id
            String historyLine = getHistory().stream()
                    .map(task -> Integer.toString(task.getId()))
                    .collect(Collectors.joining(","));
            bw.write(historyLine);
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка сохранения в файл: " + file, e);
        }
    }

    private static String escape(String s) {
        // по условию — кавычек и запятых в данных не будет,
        // но на всякий случай заменим переводы строк
        return s == null ? "" : s.replace("\n", " ").replace("\r", " ");
    }

    private static String taskToCsv(Task t) { // SPRINT-8
        String start = (t.getStartTime() == null) ? "" : t.getStartTime().format(FMT);
        String dur = (t.getDuration() == null) ? "" : String.valueOf(t.getDuration().toMinutes());
        return String.join(",",
                Integer.toString(t.getId()),
                TaskType.TASK.name(),
                escape(t.getTitle()),
                t.getStatus().name(),
                escape(t.getDescription()),
                start,              // startTime
                dur,                // durationMinutes
                ""                  // epicId пусто
        );
    }

    private static String epicToCsv(Epic e) { // SPRINT-8
        // агрегаты времени эпика не пишем — пересчитаются при загрузке
        return String.join(",",
                Integer.toString(e.getId()),
                TaskType.EPIC.name(),
                escape(e.getTitle()),
                e.getStatus().name(),
                escape(e.getDescription()),
                "",                 // startTime
                "",                 // durationMinutes
                ""                  // epicId
        );
    }

    private static String subtaskToCsv(Subtask s) { // SPRINT-8
        String start = (s.getStartTime() == null) ? "" : s.getStartTime().format(FMT);
        String dur = (s.getDuration() == null) ? "" : String.valueOf(s.getDuration().toMinutes());
        return String.join(",",
                Integer.toString(s.getId()),
                TaskType.SUBTASK.name(),
                escape(s.getTitle()),
                s.getStatus().name(),
                escape(s.getDescription()),
                start,              // startTime
                dur,                // durationMinutes
                Integer.toString(s.getEpicId())
        );
    }

    private static Task taskFromCsv(String line) { // SPRINT-8
        // id,type,name,status,description,startTime,durationMinutes,epicId
        String[] p = line.split(",", -1);
        if (p.length < 8) {
            throw new ManagerSaveException("Неверная строка CSV (ожидалось 8 столбцов): " + line);
        }

        int id = Integer.parseInt(p[0]);
        TaskType type = TaskType.valueOf(p[1]);
        String name = p[2];
        Status status = Status.valueOf(p[3]);
        String description = p[4];

        LocalDateTime start = p[5].isEmpty() ? null : LocalDateTime.parse(p[5], FMT);
        Duration duration = p[6].isEmpty() ? null : Duration.ofMinutes(Long.parseLong(p[6]));
        String epicIdStr = p[7];

        return switch (type) {
            case TASK -> new Task(id, name, description, status, start, duration);
            case EPIC -> {
                Epic e = new Epic(id, name, description); // агрегаты пересчитаются после загрузки
                e.setStatusDirect(status);
                yield e;
            }
            case SUBTASK -> {
                if (epicIdStr.isEmpty())
                    throw new ManagerSaveException("Subtask без epicId: " + line);
                int epicId = Integer.parseInt(epicIdStr);
                yield new Subtask(id, name, description, status, epicId, start, duration);
            }
        };
    }


    // ----------------- Загрузка -----------------

    public static FileBackedTaskManager loadFromFile(File file) {
        FileBackedTaskManager mgr = new FileBackedTaskManager(file);
        if (!file.exists() || file.length() == 0) {
            return mgr; // пустой менеджер
        }
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            if (lines.isEmpty()) return mgr;

            int i = 0;
            if (!HEADER.equals(lines.get(i))) {
                throw new ManagerSaveException("Некорректный заголовок CSV в " + file);
            }
            i++;

            // читаем задачи до пустой строки
            int maxId = 0;
            for (; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) {
                    i++;
                    break;
                }
                Task t = taskFromCsv(line);
                maxId = Math.max(maxId, t.getId());

                if (t instanceof Epic e) {
                    mgr.addEpicWithCustomId(e);
                } else if (t instanceof Subtask s) {
                    mgr.addSubtaskWithCustomId(s);
                } else {
                    mgr.addTaskWithCustomId(t);
                }
            }

            // nextId = maxId+1
            mgr.setNextId(maxId + 1);

            // --- SPRINT-8: пересчитать агрегаты эпиков после загрузки (status/start/duration/end)
            for (Epic e : mgr.getAllEpics()) {
                mgr.updateEpicStatus(e);
            }

            // история (если есть строки ещё) — без побочных эффектов (не вызываем get*())
            if (i < lines.size()) {
                String historyLine = lines.get(i).trim();
                if (!historyLine.isEmpty()) {
                    for (String part : historyLine.split(",")) {
                        int hid = Integer.parseInt(part.trim());

                        // Берём прямой объект по id из внутренних карт
                        Task t = mgr.tasks.get(hid);
                        if (t == null) {
                            t = mgr.epics.get(hid);
                        }
                        if (t == null) {
                            t = mgr.subtasks.get(hid);
                        }
                        // Кладём в историю напрямую (без save(), без дублирования вызовов get*())
                        mgr.addToHistoryDirect(t);
                    }
                }
            }

            return mgr;
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка чтения файла " + file, e);
        } catch (RuntimeException e) {
            throw new ManagerSaveException("Ошибка разбора CSV в файле " + file + ": " + e.getMessage(), e);
        }
    }
}
