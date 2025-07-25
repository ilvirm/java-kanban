package tracker.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SubtaskTest {

// Проверяем, что наследники класса Task равны друг другу, если равен их id;

    @Test
    void subtasksWithSameIdShouldBeEqual() {
        // Arrange: создаём две разные подзадачи
        Subtask subtask1 = new Subtask("Read book", "Chapter 1", Status.NEW, 1);
        Subtask subtask2 = new Subtask("Write report", "Chapter 2", Status.IN_PROGRESS, 2);

        // Присваиваем одинаковый ID
        subtask1.setId(100);
        subtask2.setId(100);

        // Assert: проверяем, что они равны по equals()
        // В Task переопределён equals() по id.
        assertEquals(subtask1, subtask2, "Subtask объекты с одинаковым id должны считаться равными");
    }


}