package tracker.controllers;

import tracker.model.Epic;
import tracker.model.Subtask;
import tracker.model.Task;

import java.util.ArrayList;
import java.util.List;

public interface TaskManager {
    void addTask(Task task);

    Task getTask(int id);

    ArrayList<Task> getAllTasks();

    void updateTask(Task task);

    void removeTask(int id);

    void clearTasks();

    void addEpic(Epic epic);

    Epic getEpic(int id);

    ArrayList<Epic> getAllEpics();

    void updateEpic(Epic epic);

    void removeEpic(int id);

    void clearEpics();

    void addSubtask(Subtask subtask);

    Subtask getSubtask(int id);

    ArrayList<Subtask> getAllSubtasks();

    ArrayList<Subtask> getSubtasksOfEpic(int epicId);

    void updateSubtask(Subtask subtask);

    void removeSubtask(int id);

    void clearSubtasks();

    // --- HISTORY ---

    /*
     * Возвращает список последних просмотренных задач.
     * Порядок — от самой старой к самой новой.
     *
     */
    List<Task> getHistory();  // 🔧 Добавлен метод истории
}