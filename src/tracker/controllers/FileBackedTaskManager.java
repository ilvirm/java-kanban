package tracker.controllers;

import tracker.exceptions.ManagerSaveException;
import tracker.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Менеджер с автосохранением в CSV:
 *
 * Формат:
 * 1-я строка: заголовок
 * Далее: задачи (TASK/EPIC/SUBTASK)
 * Пустая строка
 * Последняя строка: история просмотров: id,id,id
 */
public class FileBackedTaskManager extends InMemoryTaskManager {

    private static final String HEADER = "id,type,name,status,description,epic";
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

    private static String taskToCsv(Task t) {
        return String.join(",",
                Integer.toString(t.getId()),
                TaskType.TASK.name(),
                escape(t.getTitle()),
                t.getStatus().name(),
                escape(t.getDescription()),
                "" // epic id пустой
        );
    }

    private static String epicToCsv(Epic e) {
        return String.join(",",
                Integer.toString(e.getId()),
                TaskType.EPIC.name(),
                escape(e.getTitle()),
                e.getStatus().name(),
                escape(e.getDescription()),
                "" // epic id пустой
        );
    }

    private static String subtaskToCsv(Subtask s) {
        return String.join(",",
                Integer.toString(s.getId()),
                TaskType.SUBTASK.name(),
                escape(s.getTitle()),
                s.getStatus().name(),
                escape(s.getDescription()),
                Integer.toString(s.getEpicId())
        );
    }

    private static TaskType parseType(String s) {
        return TaskType.valueOf(s);
    }

    private static Status parseStatus(String s) {
        return Status.valueOf(s);
    }

    private static Task taskFromCsv(String line) {
        // id,type,name,status,description,epic
        String[] p = line.split(",", -1);
        int id = Integer.parseInt(p[0]);
        TaskType type = parseType(p[1]);
        String name = p[2];
        Status status = parseStatus(p[3]);
        String description = p[4];

        switch (type) {
            case TASK -> {
                Task t = new Task(name, description, status);
                t.setId(id);
                return t;
            }
            case EPIC -> {
                Epic e = new Epic(name, description);
                e.setId(id);
                e.setStatusDirect(status);
                return e;
            }
            case SUBTASK -> {
                int epicId = Integer.parseInt(p[5]);
                Subtask s = new Subtask(name, description, status, epicId);
                s.setId(id);
                return s;
            }
            default -> throw new IllegalStateException("Неизвестный тип: " + type);
        }
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

            // история (если есть строки ещё)
            if (i < lines.size()) {
                String historyLine = lines.get(i).trim();
                if (!historyLine.isEmpty()) {
                    for (String part : historyLine.split(",")) {
                        int id = Integer.parseInt(part.trim());
                        // восстанавливаем историю, вызывая get* (они добавят в historyManager)
                        if (mgr.tasks.containsKey(id)) {
                            mgr.getTask(id);
                        } else if (mgr.epics.containsKey(id)) {
                            mgr.getEpic(id);
                        } else if (mgr.subtasks.containsKey(id)) {
                            mgr.getSubtask(id);
                        }
                    }
                }
            }
            return mgr;
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка чтения файла " + file, e);
        }
    }
}
