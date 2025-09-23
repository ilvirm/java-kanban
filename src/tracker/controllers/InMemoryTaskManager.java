package tracker.controllers;

import tracker.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class InMemoryTaskManager implements TaskManager {
    // currentId делаем доступным наследнику.
    protected int currentId = 1;

    // Делаем коллекции доступными наследнику
    protected final HashMap<Integer, Task> tasks = new HashMap<>();
    protected final HashMap<Integer, Epic> epics = new HashMap<>();
    protected final HashMap<Integer, Subtask> subtasks = new HashMap<>();

    // Добавлена переменная-ссылка на менеджер истории
    private final HistoryManager historyManager = Managers.getDefaultHistory();


    // --- NEW (SPRINT-8): индекс приоритизации задач по времени
    // Сортировка: startTime по возрастанию (null в конце) -> id
    private static final Comparator<Task> BY_START_THEN_ID =
            Comparator.<Task, LocalDateTime>comparing(Task::getStartTime,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparingInt(Task::getId);

    // Только исполняемые элементы (Task и Subtask). Эпики обычно не включаем в план.
    private final NavigableSet<Task> prioritized = new TreeSet<>(BY_START_THEN_ID);

    // --- NEW (SPRINT-8): помощники индекса приоритизации

    private void index(Task t) {
        if (t != null && t.getStartTime() != null) {
            prioritized.add(t);
        }
    }

    private void deindex(Task t) {
        if (t != null) {
            prioritized.remove(t);
        }
    }

    // --- NEW (SPRINT-8): проверка пересечения интервалов [start, end)
    private static boolean overlaps(Task a, Task b) {
        LocalDateTime as = a.getStartTime(), bs = b.getStartTime();
        Duration ad = a.getDuration(), bd = b.getDuration();
        if (as == null || ad == null || bs == null || bd == null) return false; // «без времени» — не конфликтуют
        LocalDateTime ae = a.getEndTime(), be = b.getEndTime(); // end = start + duration (уже есть в моделях)
        return as.isBefore(be) && bs.isBefore(ae); // полуинтервалы: [start, end)
    }

    // --- NEW (SPRINT-8): гарантируем отсутствие пересечений с уже индексированными задачами

    private void ensureNoOverlaps(Task candidate) {
        boolean conflict = prioritized.stream()
                .filter(existing -> existing.getId() != candidate.getId())
                .anyMatch(existing -> overlaps(candidate, existing));
        if (conflict) {
            throw new IllegalStateException(
                    "Конфликт по времени: задача id=" + candidate.getId() +
                            " пересекается с другой задачей по времени выполнения");
        }
    }

    // --- FileBackedTaskManager ---

    /**
     * Устанавливает nextId (= maxId+1) при восстановлении из файла.
     */
    protected final void setNextId(int nextId) {
        this.currentId = Math.max(nextId, this.currentId);
    }

    /**
     * Вставка Task с уже заданным id (без генерации id и без истории).
     */
    protected final void addTaskWithCustomId(Task task) {
        tasks.put(task.getId(), task);
        // индексируем, чтобы getPrioritizedTasks() был корректным после загрузки
        index(task);
    }


    /**
     * Вставка Epic с уже заданным id (без генерации id и без истории).
     */
    protected final void addEpicWithCustomId(Epic epic) {
        epics.put(epic.getId(), epic);
    }

    /**
     * Вставка Subtask с уже заданным id + привязка к эпику + пересчёт статуса эпика.
     */
    protected final void addSubtaskWithCustomId(Subtask subtask) {
        subtasks.put(subtask.getId(), subtask);
        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null) {
            epic.addSubtaskId(subtask.getId());
            updateEpicStatusSilently(epic);
        }
        // индексируем сабтаск тоже
        index(subtask);
    }


    // --- TASK ---

    @Override
    public void addTask(Task task) {
        int id = generateId();
        task.setId(id);
        // --- NEW (SPRINT-8): проверяем пересечения до сохранения
        ensureNoOverlaps(task);
        tasks.put(id, task);
        // --- NEW (SPRINT-8): добавляем в индекс приоритизации
        index(task);
    }

    @Override
    public Task getTask(int id) {
        final Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);  // добавляем в историю только если задача найдена
        }
        return task;
    }

    @Override
    public ArrayList<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public void updateTask(Task task) {
        if (tasks.containsKey(task.getId())) {
            // --- NEW (SPRINT-8): временно убираем старую версию из индекса
            deindex(tasks.get(task.getId()));

            // --- NEW (SPRINT-8): проверяем пересечения новой версии
            ensureNoOverlaps(task);

            tasks.put(task.getId(), task);

            // --- NEW (SPRINT-8): возвращаем в индекс уже обновлённую задачу
            index(task);
        }
    }

    @Override
    public void removeTask(int id) {

        // --- (SPRINT-8): получить удаляемую задачу, чтобы снять её из индекса
        Task removed = tasks.remove(id);
        deindex(removed);
        if (removed != null) historyManager.remove(id);
    }

    @Override
    public void clearTasks() {

        // --- NEW (SPRINT-8): снять все задачи из индекса и удалить их из истории
        for (Task t : tasks.values()) {
            deindex(t);
            historyManager.remove(t.getId());
        }
        tasks.clear();
    }

    // --- EPIC ---

    @Override
    public void addEpic(Epic epic) {
        int id = generateId();
        epic.setId(id);
        epics.put(id, epic);

        // --- NEW (SPRINT-8): обновим агрегаты времени и статус (на всякий случай)
        updateEpicStatus(epic);
    }

    @Override
    public Epic getEpic(int id) {
        final Epic epic = epics.get(id);
        // Добавляем просмотренный эпик в историю
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic;
    }

    @Override
    public ArrayList<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public void updateEpic(Epic epic) {
        if (epics.containsKey(epic.getId())) {
            epics.put(epic.getId(), epic);
            updateEpicStatus(epic);
        }
    }

    @Override
    public void removeEpic(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {

            // --- NEW (SPRINT-8): удалить его сабтаски из индекса/хранилища
            for (int subtaskId : epic.getSubtaskIds()) {
                Subtask st = subtasks.remove(subtaskId);
                deindex(st);
                if (st != null) historyManager.remove(subtaskId);
            }
            historyManager.remove(id);

        }
    }

    @Override
    public void clearEpics() {

        // --- NEW (SPRINT-8): очистим сабтаски (и индекс) вместе с эпиками
        for (Epic epic : epics.values()) {
            for (int sid : epic.getSubtaskIds()) {
                Subtask st = subtasks.remove(sid);
                deindex(st);
                if (st != null) historyManager.remove(sid);
            }
            historyManager.remove(epic.getId());
        }
        subtasks.clear();
        epics.clear();
    }

    // --- SUBTASK ---

    @Override
    public void addSubtask(Subtask subtask) {
        Epic epic = epics.get(subtask.getEpicId());
        if (epic == null) {
            throw new IllegalArgumentException("Нельзя создать подзадачу без существующего эпика (id=" + subtask.getEpicId() + ")");
        }

        if (subtask.getId() != 0 && subtask.getId() == subtask.getEpicId()) {
            throw new IllegalArgumentException("Ошибка: подзадача не может быть своим же эпиком (id=" + subtask.getId() + ")");
        }

        int id = generateId();
        subtask.setId(id);

        // --- NEW (SPRINT-8): проверка пересечений
        ensureNoOverlaps(subtask);

        subtasks.put(id, subtask);
        epic.addSubtaskId(id);

        // --- NEW (SPRINT-8): индекс приоритизации
        index(subtask);

        updateEpicStatus(epic);
    }

    @Override
    public Subtask getSubtask(int id) {
        final Subtask subtask = subtasks.get(id);
        // Добавляем просмотренную подзадачу в историю
        if (subtask != null) {
            historyManager.add(subtask);
        }
        return subtask;
    }

    @Override
    public ArrayList<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public ArrayList<Subtask> getSubtasksOfEpic(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) return new ArrayList<>();
        return epic.getSubtaskIds().stream()
                .map(subtasks::get)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        // Защита от null / отсутствующей подзадачи
        if (subtask == null || !subtasks.containsKey(subtask.getId())) {
            return;
        }
        // --- NEW (SPRINT-8): временно снять старую версию из индекса
        Subtask old = subtasks.get(subtask.getId());
        deindex(old);
        ensureNoOverlaps(subtask);
        subtasks.put(subtask.getId(), subtask);
        index(subtask);

        if (old.getEpicId() != subtask.getEpicId()) {

            Epic oldEpic = epics.get(old.getEpicId());
            if (oldEpic != null) {
                oldEpic.removeSubtaskId(old.getId());
                updateEpicStatus(oldEpic);
            }
            Epic newEpic = epics.get(subtask.getEpicId());
            if (newEpic == null) {
                throw new IllegalArgumentException("Новый Epic не найден: id=" + subtask.getEpicId());
            }
            newEpic.addSubtaskId(subtask.getId());
            updateEpicStatus(newEpic);
        } else {
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) updateEpicStatus(epic);
        }
    }

    @Override
    public void removeSubtask(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {

            // --- NEW (SPRINT-8): снять из индекса и истории
            deindex(subtask);
            historyManager.remove(id);

            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubtaskId(id);
                updateEpicStatus(epic);
            }
        }
    }

    @Override
    public void clearSubtasks() {
        // --- NEW (SPRINT-8): снимаем все сабтаски из индекса/истории
        for (Subtask st : subtasks.values()) {
            deindex(st);
            historyManager.remove(st.getId());
        }
        subtasks.clear();

        for (Epic epic : epics.values()) {
            epic.clearSubtasks();
            updateEpicStatus(epic);
        }
    }

    public List<Integer> getEpicSubtaskIds(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) {
            return List.of(); // или можно бросить IllegalArgumentException
        }
        // Возвращаем копию, чтобы не дать внешнему коду мутировать внутренний список
        return new ArrayList<>(epic.getSubtaskIds());
    }


    // --- HISTORY ---

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }


    /**
     * SPRINT-8: добавить запись в историю без побочных эффектов.
     * Нужен для FileBackedTaskManager.loadFromFile(..), чтобы не дёргать публичные get*().
     */
    protected void addToHistoryDirect(Task t) {
        if (t != null) {
            historyManager.add(t);
        }
    }

    // --- NEW (SPRINT-8): приоритетный список для планирования
    @Override
    public List<Task> getPrioritizedTasks() {
        return Collections.unmodifiableList(new ArrayList<>(prioritized));
    }

    // --- NEW (SPRINT-8): публичная проверка пересечений (реализация метода интерфейса)
    @Override
    public boolean hasOverlaps(Task candidate) {
        if (candidate == null) return false;
        if (candidate.getStartTime() == null || candidate.getDuration() == null) {
            return false; // задачи без времени не конфликтуют
        }

        return prioritized.stream()
                .filter(existing -> existing.getId() != candidate.getId())
                .anyMatch(existing -> overlaps(candidate, existing)); // вызываем приватный overlaps(...)
    }

    // --- PRIVATE HELPERS ---

    private int generateId() {
        return currentId++;
    }

    protected void updateEpicStatus(Epic epic) {
        ArrayList<Integer> subtaskIds = epic.getSubtaskIds();

        if (subtaskIds.isEmpty()) {

            // --- NEW (SPRINT-8): сбрасываем агрегаты времени у пустого эпика (через direct)
            epic.setStartTimeDirect(null);
            epic.setDurationDirect(null);
            epic.setEndTimeDirect(null);
            epic.setStatusDirect(Status.NEW);
            return;
        }

        boolean allNew = true;
        boolean allDone = true;

        // --- NEW (SPRINT-8): объявляем агрегаторы времени перед циклом
        LocalDateTime minStart = null;
        LocalDateTime maxEnd = null;
        long totalMinutes = 0;

        for (int id : subtaskIds) {
            Subtask subtask = subtasks.get(id);
            if (subtask == null) continue;

            Status status = subtask.getStatus();
            if (status != Status.NEW) {
                allNew = false;
            }
            if (status != Status.DONE) {
                allDone = false;
            }

            // --- NEW (SPRINT-8): учитываем только полностью заданные интервалы
            if (subtask.getStartTime() != null && subtask.getDuration() != null) {
                if (minStart == null || subtask.getStartTime().isBefore(minStart)) {
                    minStart = subtask.getStartTime();
                }
                LocalDateTime end = subtask.getEndTime();
                if (end != null && (maxEnd == null || end.isAfter(maxEnd))) {
                    maxEnd = end;
                }
                totalMinutes += subtask.getDuration().toMinutes();
            }
        }

        if (allDone) {
            epic.setStatusDirect(Status.DONE);
        } else if (allNew) {
            epic.setStatusDirect(Status.NEW);
        } else {
            epic.setStatusDirect(Status.IN_PROGRESS);
        }

        // --- NEW (SPRINT-8): записываем агрегаты времени у эпика (direct-методы)
        epic.setStartTimeDirect(minStart);
        epic.setDurationDirect(totalMinutes == 0 ? null : Duration.ofMinutes(totalMinutes));
        epic.setEndTimeDirect(maxEnd);
    }

    // прямой доступ по id без записи в историю
    protected Task peekAny(int id) {
        Task t = tasks.get(id);
        if (t != null) return t;
        Task s = subtasks.get(id);
        if (s != null) return s;
        return epics.get(id);
    }

    // «тихий» пересчёт для одного эпика — без истории/сохранений и без публичных get*()
    protected void updateEpicStatusSilently(Epic e) {
        if (e == null) return;

        long totalMin = 0L;
        LocalDateTime start = null, end = null;

        boolean allNew = true;
        boolean allDone = true;

        for (Integer sid : e.getSubtaskIds()) {
            Subtask s = subtasks.get(sid);
            if (s == null) continue;

            // статус
            Status st = s.getStatus();
            if (st != Status.NEW)  allNew = false;
            if (st != Status.DONE) allDone = false;

            // агрегаты времени
            if (s.getStartTime() != null && s.getDuration() != null) {
                if (start == null || s.getStartTime().isBefore(start)) {
                    start = s.getStartTime();
                }
                LocalDateTime sEnd = s.getEndTime();
                if (sEnd != null && (end == null || sEnd.isAfter(end))) {
                    end = sEnd;
                }
                totalMin += s.getDuration().toMinutes();
            }
        }

        // статус эпика напрямую
        if (e.getSubtaskIds().isEmpty()) {
            e.setStatusDirect(Status.NEW);
        } else if (allDone) {
            e.setStatusDirect(Status.DONE);
        } else if (allNew) {
            e.setStatusDirect(Status.NEW);
        } else {
            e.setStatusDirect(Status.IN_PROGRESS);
        }

        // время эпика напрямую
        e.setStartTimeDirect(start);
        e.setDurationDirect(totalMin == 0 ? null : Duration.ofMinutes(totalMin));
        e.setEndTimeDirect(end);
    }
    
    protected Task getTaskForHistory(int id) {
        // Порядок должен соответствовать ожидаемому в тесте: Task -> Epic -> Subtask
        if (tasks.containsKey(id)) return tasks.get(id);
        if (epics.containsKey(id)) return epics.get(id);
        if (subtasks.containsKey(id)) return subtasks.get(id);
        return null;
    }
}