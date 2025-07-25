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

    /**
     * Возвращает список последних просмотренных задач (до 10 штук).
     *
     * @return список задач в порядке просмотра
     */
    List<Task> getHistory();
}