package tracker.controllers;

import tracker.model.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

public class InMemoryTaskManager implements TaskManager {
    private int currentId = 1;

    private final HashMap<Integer, Task> tasks = new HashMap<>();
    private final HashMap<Integer, Epic> epics = new HashMap<>();
    private final HashMap<Integer, Subtask> subtasks = new HashMap<>();

    // Ддобавлена переменная-ссылка на менеджер истории
    private final HistoryManager historyManager = Managers.getDefaultHistory();

    // --- TASK ---

    @Override
    public void addTask(Task task) {
        int id = generateId();
        task.setId(id);
        tasks.put(id, task);
    }

    // Используется только для тестов — добавляет задачу с заданным вручную id
    public void addTaskWithCustomId(Task task) {
        tasks.put(task.getId(), task);
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
            tasks.put(task.getId(), task);
        }
    }

    @Override
    public void removeTask(int id) {
        tasks.remove(id);
    }

    @Override
    public void clearTasks() {
        tasks.clear();
    }

    // --- EPIC ---

    @Override
    public void addEpic(Epic epic) {
        int id = generateId();
        epic.setId(id);
        epics.put(id, epic);
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
            for (int subtaskId : epic.getSubtaskIds()) {
                subtasks.remove(subtaskId);
            }
        }
    }

    @Override
    public void clearEpics() {
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
        subtasks.put(id, subtask);
        epic.addSubtaskId(id);
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
        ArrayList<Subtask> result = new ArrayList<>();
        Epic epic = epics.get(epicId);
        if (epic != null) {
            for (int id : epic.getSubtaskIds()) {
                result.add(subtasks.get(id));
            }
        }
        return result;
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (subtasks.containsKey(subtask.getId())) {
            subtasks.put(subtask.getId(), subtask);
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                updateEpicStatus(epic);
            }
        }
    }

    @Override
    public void removeSubtask(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubtaskId(id);
                updateEpicStatus(epic);
            }
        }
    }

    @Override
    public void clearSubtasks() {
        for (Epic epic : epics.values()) {
            epic.clearSubtasks();
            updateEpicStatus(epic);
        }
        subtasks.clear();
    }

    // --- HISTORY ---

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }


    // --- PRIVATE HELPERS ---

    private int generateId() {
        return currentId++;
    }

    private void updateEpicStatus(Epic epic) {
        ArrayList<Integer> subtaskIds = epic.getSubtaskIds();

        if (subtaskIds.isEmpty()) {
            epic.setStatusDirect(Status.NEW);
            return;
        }

        boolean allNew = true;
        boolean allDone = true;

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
        }

        if (allDone) {
            epic.setStatusDirect(Status.DONE);
        } else if (allNew) {
            epic.setStatusDirect(Status.NEW);
        } else {
            epic.setStatusDirect(Status.IN_PROGRESS);
        }
    }
}