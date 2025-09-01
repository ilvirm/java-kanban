package tracker.exceptions;

/**
 * Исключение для ошибок сохранения/загрузки задач.
 * Создано вместо стандартных IOException/RuntimeException,
 * чтобы сразу было видно: ошибка связана именно с Task Manager.
 */
public class ManagerSaveException extends RuntimeException {

    // Ошибка с указанием причины (например, IOException)
    public ManagerSaveException(String message, Throwable cause) {
        super(message, cause);
    }

    // Когда ошибки логические (без исключения).
    public ManagerSaveException(String message) {
        super(message);
    }
}