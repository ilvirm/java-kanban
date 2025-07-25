package tracker.controllers;

import org.junit.jupiter.api.Test;
import tracker.model.Task;
import tracker.model.Status;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryHistoryManagerTest {

/* Проверяем пункты:
 - Убедитесь, что задачи, добавляемые в HistoryManager, сохраняют предыдущую версию задачи и её данных
 - Убедитесь, что утилитарный класс всегда возвращает проинициализированные и готовые
   к работе экземпляры менеджеров
*/

    @Test
    void addOneTaskTwiceWithStatusChange() {

        HistoryManager historyManager = Managers.getDefaultHistory();

        Task task1 = new Task("Task1", "Test add", Status.NEW);
        historyManager.add(task1);

        // Меняем статус после добавления
        task1.setStatus(Status.IN_PROGRESS);
        historyManager.add(task1);

        List<Task> history = historyManager.getHistory();

        assertEquals(2, history.size(), "История должна содержать 2 записи задачи");

        // Проверка: оба объекта в истории имеют статус IN_PROGRESS (т.к. один и тот же task)
        assertEquals(Status.NEW, history.get(0).getStatus(), "Ожидаемый статус NEW");
        assertEquals(Status.IN_PROGRESS, history.get(1).getStatus(), "Ожидаемый статус IN_PROGRESS");
    }

    @Test
    void getHistoryReturnsCorrectOrderAndSize() {
        HistoryManager historyManager = Managers.getDefaultHistory();

        Task task1 = new Task("Task1", "First", Status.NEW);
        Task task2 = new Task("Task2", "Second", Status.IN_PROGRESS);

        historyManager.add(task1);
        historyManager.add(task2);

        List<Task> history = historyManager.getHistory();

        assertEquals(2, history.size(), "История должна содержать 2 задачи");
        assertEquals(task1, history.get(0), "Первая задача в истории — Task1");
        assertEquals(task2, history.get(1), "Вторая задача в истории — Task2");
    }
}