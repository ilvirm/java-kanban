package tracker.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TaskTest {

    @Test
    void tasksWithSameIdShouldBeEqual() {
        Task task1 = new Task("Test 1", "Description 1", Status.NEW);
        Task task2 = new Task("Test 2", "Description 2", Status.IN_PROGRESS);

        task1.setId(100);
        task2.setId(100);

        assertEquals(task1, task2, "Задачи с одинаковыми id должны быть равны");
    }
}