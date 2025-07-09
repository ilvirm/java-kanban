package tracker.controllers;

import tracker.model.*; // Импортируем всё, что нужно из model

import java.util.HashMap;
import java.util.ArrayList;

public class TaskManager {
    private int currentId = 1;  // Счетчик генерации currentId

    private final HashMap<Integer, Task> tasks = new HashMap<>();
    private final HashMap<Integer, Epic> epics = new HashMap<>();
    private final HashMap<Integer, Subtask> subtasks = new HashMap<>();

    private int generateId() {
        return currentId++;
    }

    // --- TASK ---

    public void addTask(Task task) {
        int id = generateId();
        task.setId(id);
        tasks.put(id, task);
    }

    public Task getTask(int id) {
        return tasks.get(id);
    }

    public ArrayList<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public void updateTask(Task task) {
        if (tasks.containsKey(task.getId())) {
            tasks.put(task.getId(), task);
        }
    }

    public void removeTask(int id) {
        tasks.remove(id);
    }

    public void clearTasks() {
        tasks.clear();
    }

    // --- EPIC ---

    public void addEpic(Epic epic) {
        int id = generateId();
        epic.setId(id);
        epics.put(id, epic);
    }

    public Epic getEpic(int id) {
        return epics.get(id);
    }

    public ArrayList<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    public void updateEpic(Epic epic) {
        if (epics.containsKey(epic.getId())) {
            epics.put(epic.getId(), epic);
            updateEpicStatus(epic);
        }
    }

    public void removeEpic(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            for (int subtaskId : epic.getSubtaskIds()) {
                subtasks.remove(subtaskId);
            }
        }
    }

    public void clearEpics() {
        /* Удаляются только те подзадачи, которые связаны с эпиками.
        for (Epic epic : epics.values()) {
            for (int subId : epic.getSubtaskIds()) {
                subtasks.remove(subId);
            }
        }
        */
        // Все подзадачи связаны только с эпиками, поэтому можно удалить все подзадачи.
        subtasks.clear();  // Удаляем все подзадачи сразу
        epics.clear();
    }

    // --- SUBTASK ---

    public void addSubtask(Subtask subtask) {

        /*
        //Добавляли подзадачу, а только потом проверяли, существует ли нужный эпик.
        int id = generateId();
        subtask.setId(id);
        subtasks.put(id, subtask);

        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null) {
            epic.addSubtaskId(id);
            updateEpicStatus(epic);
        }
        */

        // Не создаем подзадачу, пока не убедились, что существует эпик, к которому она относится.
        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null) {
            int id = generateId();
            subtask.setId(id);
            subtasks.put(id, subtask);
            epic.addSubtaskId(id);
            updateEpicStatus(epic);
        } else {
            System.out.println("Ошибка: нельзя создать подзадачу без существующего эпика (id=" + subtask.getEpicId() + ")");
        }
    }

    public Subtask getSubtask(int id) {
        return subtasks.get(id);
    }

    public ArrayList<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

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

    public void updateSubtask(Subtask subtask) {
        if (subtasks.containsKey(subtask.getId())) {
            subtasks.put(subtask.getId(), subtask);
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                updateEpicStatus(epic);
            }
        }
    }

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

    public void clearSubtasks() {
        for (Epic epic : epics.values()) {
            epic.clearSubtasks();
            updateEpicStatus(epic);
        }
        subtasks.clear();
    }

    // --- Обновление статуса эпика ---

    private void updateEpicStatus(Epic epic) {
        ArrayList<Integer> subtaskIds = epic.getSubtaskIds(); // Получаем список ID подзадач у эпика

        // Если у эпика нет подзадач, то статус эпика NEW
        if (subtaskIds.isEmpty()) {
            epic.setStatusDirect(Status.NEW);
            return;
        }

        boolean allNew = true; // Предполагаем, что все подзадачи — NEW
        boolean allDone = true; // Предполагаем, что все подзадачи — DONE

        for (int subtaskId : subtaskIds) {
            Subtask subtask = subtasks.get(subtaskId); // Получаем подзадачу по её ID из карты
            if (subtask == null) continue; // Пропускаем, если подзадача не найдена (например, удалена)

            Status status = subtask.getStatus(); // Получаем статус подзадачи
            if (status != Status.NEW) {
                allNew = false; // Если хотя бы одна подзадача не NEW — эпик не может быть полностью NEW
            }
            if (status != Status.DONE) {
                allDone = false; // Если хотя бы одна подзадача не DONE — эпик не может быть DONE
            }
        }

        if (allNew) {
            epic.setStatusDirect(Status.NEW); // Если все подзадачи имеют статус NEW — эпик считается NEW
        } else if (allDone) {
            epic.setStatusDirect(Status.DONE); // Если все подзадачи имеют статус DONE — эпик считается DONE
        } else {
            epic.setStatusDirect(Status.IN_PROGRESS); // В остальных случаях — эпик считается IN_PROGRESS
        }
    }
}