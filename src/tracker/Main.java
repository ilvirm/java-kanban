package tracker;

import tracker.controllers.Managers;
import tracker.controllers.TaskManager;
import tracker.http.HttpTaskServer;
import tracker.model.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // Получаем экземпляр менеджера задач (реализация InMemoryTaskManager через Managers.getDefault)
        TaskManager manager = Managers.getDefault();

        // Наполняем демо-данными
        seedDemoData(manager);

        // Запускаем HTTP-сервер на порту 8080
        HttpTaskServer server = new HttpTaskServer(manager);
        server.start();

        System.out.println("HTTP API запущено на http://localhost:8080");
        System.out.println("Примеры запросов:");
        System.out.println("GET  http://localhost:8080/tasks");
        System.out.println("GET  http://localhost:8080/epics");
        System.out.println("GET  http://localhost:8080/subtasks");
        System.out.println("GET  http://localhost:8080/history");
    }

    private static void seedDemoData(TaskManager manager) {
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

        // Обновление статусов подзадач
        subtask1.setStatus(Status.DONE);
        manager.updateSubtask(subtask1);
        subtask2.setStatus(Status.IN_PROGRESS);
        manager.updateSubtask(subtask2);
    }
}
