/*
 * Исторически класс использовал LinkedList и лимит HISTORY_LIMIT=10,
 * удаляя самый старый просмотр при переполнении (removeFirst()) и
 * возвращая копии задач для инкапсуляции.
 *
 * Текущая версия:
 * - Без лимита: HISTORY_LIMIT удалён.
 * - Без дублей: повторный просмотр переносит задачу в конец, старый узел вырезается сразу.
 * - O(1) на add/remove: за счёт собственной двусвязной структуры + индекса (Map<id, Node>).
 * - Порядок просмотров = порядок вызовов add (добавляем в хвост).
 * - getHistory() формирует ArrayList проходом от head к tail (O(n) только при чтении).
 *
 * Почему нужен Map<id, Node>:
 * - Позволяет за O(1) найти узел конкретной задачи и удалить его без обхода списка (в отличие от LinkedList).
 * - Обеспечивает выполнение требования «удалить предыдущий просмотр сразу за O(1)».
 */

package tracker.controllers;

import tracker.model.Task;

//import java.util.LinkedList;
import java.util.ArrayList; // По ТЗ нужно использовать, вместо связанного списка

import java.util.HashMap; // Индекс по id для O(1) удаления/поиска

import java.util.List;

import java.util.Map; // Интерфейс. Чтобы находить и удалять просмотр по id за O(1), а не бегать по всему списку.

/**
 * Реализация истории просмотров без лимита, без дублей и с операциями O(1).
 * Идея: собственный ДВУСВЯЗНЫЙ СПИСОК + ИНДЕКС (Map<id, Node>).
 * - Двусвязный список хранит порядок просмотров (add -> в хвост).
 * - Map даёт моментальный доступ к узлу по id задачи, чтобы:
 *   1) убрать старый просмотр при повторном add (без обхода списка),
 *   2) удалить задачу из истории по id за O(1).
 **/

public class InMemoryHistoryManager implements HistoryManager {

    //  private static final int HISTORY_LIMIT = 10;
    //  private final LinkedList<Task> history = new LinkedList<>();

    // Узел собственного двусвязного списка
    private static class Node<T> {
        T data;
        Node<T> prev;
        Node<T> next;

        Node(Node<T> prev, T data, Node<T> next) {
            this.prev = prev;
            this.data = data;
            this.next = next;
        }
    }

    // Голова/хвост двусвязного списка
    private Node<Task> head;
    private Node<Task> tail;

    //Индекс для O(1) доступа к узлу по id задачи
    private final Map<Integer, Node<Task>> index = new HashMap<>();

    @Override
    public void add(Task task) {
        if (task == null) return;

        // Legacy (до перехода на двусвязный список + Map):
        // history.add(new Task(task));
        // if (history.size() > HISTORY_LIMIT) {
        //     history.removeFirst();
        // }

        // Текущая реализация: без лимита, без дублей, O(1) add/remove

        // Если задача уже была в истории — удаляем её прежний узел за O(1).
        Node<Task> old = index.remove(task.getId());
        if (old != null) {
            removeNode(old);
        }

        // Добавляем новый просмотр в хвост и обновляем индекс.
        Node<Task> node = linkLast(task);
        index.put(task.getId(), node);

    }

    @Override
    public void remove(int id) {

        // Удаляем просмотр по id без обхода списка —
        // берём узел из Map и вырезаем его за O(1).
        Node<Task> node = index.remove(id);
        if (node != null) {
            removeNode(node);
        }
    }

    @Override
    public List<Task> getHistory() {
        //return new LinkedList<>(history); // Возвращаем копию, чтобы не нарушать инкапсуляцию
        List<Task> result = new ArrayList<>(index.size()); //Инициализируем ArrayList с известным размером
        Node<Task> cur = head;
        while (cur != null) {
            result.add(cur.data);
            cur = cur.next;
        }
        return result;
    }


    // ** Помощники **
    // добавить элемент в хвост списка (последний просмотр)
    private Node<Task> linkLast(Task task) {
        Node<Task> node = new Node<>(tail, task, null);
        if (tail != null) {
            tail.next = node;
        } else {
            head = node; // первый элемент
        }
        tail = node;
        return node;
    }

    // NEW: удалить узел из середины/головы/хвоста за O(1)
    private void removeNode(Node<Task> node) {
        Node<Task> prev = node.prev;
        Node<Task> next = node.next;

        if (prev != null) {
            prev.next = next;
        } else {
            head = next;  // удалили голову
        }

        if (next != null) {
            next.prev = prev;
        } else {
            tail = prev; // удалили хвост
        }

        // Разрываем ссылки, чтобы освободить память
        node.prev = null;
        node.next = null;
        node.data = null;
    }
}