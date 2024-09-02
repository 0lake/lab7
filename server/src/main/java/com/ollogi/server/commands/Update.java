package com.ollogi.server.commands;

import com.general.command.Command;
import com.general.exceptions.CollectionIsEmptyException;
import com.general.exceptions.InvalidFormException;
import com.general.exceptions.WrongAmountOfElementsException;
import com.general.managers.CollectionManager;
import com.general.models.base.Element;
import com.general.network.Request;
import com.general.network.Response;

import java.nio.file.AccessDeniedException;
import java.util.Optional;

/**
 * Команда 'update'. Обновляет элемент коллекции.
 */
public class Update<T extends Element & Comparable<T>> extends Command {
    private final CollectionManager<T> collectionManager;

    public Update(CollectionManager<T> collectionManager) {
        super("update <ID> {element}", "обновить значение элемента коллекции по ID");
        this.collectionManager = collectionManager;
    }

    /**
     * Выполняет команду
     *
     * @return Response с результатом выполнения команды.
     */
    @Override
    public Response execute(Request request) {
        try {
            // Проверяем наличие данных в запросе и их тип
            if (request.getData() == null || !(request.getData() instanceof Element)) {
                throw new WrongAmountOfElementsException();
            }

            @SuppressWarnings("unchecked")
            T newElement = (T) request.getData();

            // Извлекаем ID из нового элемента
            Long id = newElement.getId();

            // Проверяем, пуста ли коллекция
            if (collectionManager.collectionSize() == 0) {
                throw new CollectionIsEmptyException();
            }

            // Ищем элемент по ID с использованием Stream API
            Optional<T> optionalElement = collectionManager.getCollection().stream()
                    .filter(element -> element.getId().equals(id))
                    .findFirst();

            if (optionalElement.isEmpty()) {
                return new Response(false, "Элемента с таким ID в коллекции нет!");
            }

            T existingElement = optionalElement.get();
            if (!existingElement.getUsername().equals(newElement.getUsername()))
                throw new AccessDeniedException("У вас нет доступа к данному элементу.");
            // Проверяем валидность нового элемента
            if (!newElement.validate()) {
                throw new InvalidFormException();
            }

            collectionManager.updateInCollection(newElement);
            collectionManager.sortCollection();

            return new Response(true, "Элемент успешно обновлен.");

        } catch (WrongAmountOfElementsException exception) {
            return new Response(false, "Неправильное количество аргументов! Правильное использование: '" + getName() + "'");
        } catch (CollectionIsEmptyException exception) {
            return new Response(false, "Коллекция пуста!");
        } catch (InvalidFormException e) {
            return new Response(false, "Поля элемента не валидны! Элемент не обновлен!");
        } catch (AccessDeniedException e) {
            return new Response(false, "Ошибка доступа: " + e.getMessage());
        } catch (Exception e) {
            return new Response(false, "Произошла непредвиденная ошибка: " + e.getMessage());
        }
    }
}