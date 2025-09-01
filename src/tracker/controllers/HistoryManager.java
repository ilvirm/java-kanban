package tracker.controllers;

import tracker.model.Task;
import java.util.List;

public interface HistoryManager {
    /**
     * Добавляет задачу в историю просмотров.
     *
     * @param task задача, которую просмотрел пользователь
     */
    void add(Task task);

    void remove(int id); //Добавляем метод для удаления задачи из просмотра

    /**
     * Возвращает список последних просмотренных задач (до 10 штук).
     *
     * @return список задач в порядке просмотра
     */
    List<Task> getHistory();
}