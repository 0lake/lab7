package com.ollogi.server.commands;

import com.general.command.Command;
import com.general.exceptions.WrongAmountOfElementsException;
import com.general.managers.CollectionManager;
import com.general.models.base.Element;
import com.general.network.Request;
import com.general.network.Response;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Команда 'remove_greater {element}'. Удаляет из коллекции все элементы, превышающие заданный.
 */
public class RemoveGreater<T extends Element & Comparable<T>> extends Command {
    private final CollectionManager<T> collectionManager;

    public RemoveGreater(CollectionManager<T> collectionManager) {
        super("remove_greater {element}", "удалить из коллекции все элементы, превышающие заданный");
        this.collectionManager = collectionManager;
    }

    /**
     * Выполняет команду
     * @return Response с результатом выполнения команды.
     */
    @Override
    public Response execute(Request request) {
        try {
            if (request.getData() == null) throw new WrongAmountOfElementsException();

            @SuppressWarnings("unchecked")
            T element = (T) request.getData();

            int removedElementsCount = removeGreater(element, request);
            return new Response(true, "Удалено " + removedElementsCount + " элементов, превышающих заданный.");

        } catch (WrongAmountOfElementsException exception) {
            return new Response(false, "Неправильное количество аргументов! Правильное использование: '" + getName() + "'");
        } catch (Exception e) {
            return new Response(false, e.getMessage());
        }
    }

    private int removeGreater(T element, Request request) {
        var collection = collectionManager.getCollection();
        collectionManager.sortCollection();

        // Проверка на пустоту коллекции
        if (collection == null || collection.isEmpty()) {
            return 0;
        }

        // Использование Stream API для фильтрации элементов, которые больше указанного
        List<T> elementsToRemove = collection.stream()
                .filter(e -> e.compareTo(element) > 0)
                .collect(Collectors.toList());

        // Подсчет успешно удалённых элементов с помощью метода removeFromCollection
        int removedCount = 0;
        for (T elementToRemove : elementsToRemove) {
            if (collectionManager.removeFromCollection(elementToRemove, request.getLogin())) {
                removedCount++;
            }
        }

        return removedCount;
    }

}
