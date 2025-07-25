package tracker;

//import tracker.controllers.InMemoryTaskManager;
import tracker.controllers.Managers;
import tracker.model.*;
import tracker.controllers.TaskManager;

public class Main {
    public static void main(String[] args) {
        TaskManager manager = Managers.getDefault();

        // Создание 2 задач
        Task task1 = new Task("Сходить в магазин", "Купить продукты", Status.NEW);
        Task task2 = new Task("Записаться к врачу", "Позвонить в поликлинику", Status.NEW);
        manager.addTask(task1);
        manager.addTask(task2);

        // Создание эпика с 2 подзадачами
        Epic epic1 = new Epic("Переезд", "Организация переезда");
        manager.addEpic(epic1);

        Subtask subtask1 = new Subtask("Собрать вещи", "Сложить всё по коробкам", Status.NEW, epic1.getId());
        Subtask subtask2 = new Subtask("Вызвать грузчиков", "Позвонить в транспортную компанию", Status.NEW, epic1.getId());
        manager.addSubtask(subtask1);
        manager.addSubtask(subtask2);

        // Эпик без подзадач
        Epic epic2 = new Epic("Подготовка отчёта", "Сделать презентацию");
        manager.addEpic(epic2);

        // Вывод текущего состояния
        System.out.println("=== ЗАДАЧИ ===");
        for (Task task : manager.getAllTasks()) {
            System.out.println(task);
        }

        System.out.println("\n=== ЭПИКИ ===");
        for (Epic epic : manager.getAllEpics()) {
            System.out.println(epic);
        }

        System.out.println("\n=== ПОДЗАДАЧИ ===");
        for (Subtask sub : manager.getAllSubtasks()) {
            System.out.println(sub);
        }

        // Обновление статусов подзадач
        subtask1.setStatus(Status.DONE);
        manager.updateSubtask(subtask1);
        subtask2.setStatus(Status.IN_PROGRESS);
        manager.updateSubtask(subtask2);

        System.out.println("\n=== ЭПИК после обновления подзадач ===");
        System.out.println(manager.getEpic(epic1.getId()));

        // Удаление задачи
        manager.removeTask(task1.getId());

        // Удаление эпика (удаляются и подзадачи)
        manager.removeEpic(epic1.getId());

        System.out.println("\n=== ПОСЛЕ УДАЛЕНИЯ ===");
        System.out.println("Оставшиеся задачи:");
        for (Task task : manager.getAllTasks()) {
            System.out.println(task);
        }

        System.out.println("\nОставшиеся эпики:");
        for (Epic epic : manager.getAllEpics()) {
            System.out.println(epic);
        }

        System.out.println("\nОставшиеся подзадачи:");
        for (Subtask sub : manager.getAllSubtasks()) {
            System.out.println(sub);
        }
    }
}