package com.ollogi.server.managers;

import com.general.managers.CollectionManager;
import com.general.models.Flat;
import com.general.models.base.Element;
import com.ollogi.server.data.FlatDAO;
import com.ollogi.server.data.UserDAO;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Класс {@code FlatCollectionManager} управляет коллекцией объектов типа {@link Flat}.
 * <p>
 * Этот класс предоставляет методы для загрузки, добавления, удаления и сортировки объектов Flat в коллекции.
 * Он работает с базой данных через DAO-классы {@link FlatDAO} и {@link UserDAO}.
 */
public class FlatCollectionManager extends CollectionManager<Flat> {
    private final FlatDAO flatDAO;
    private final UserDAO userDAO;

    /**
     * Конструктор, инициализирующий DAO и загружающий коллекцию объектов Flat из базы данных.
     *
     * @param flatDAO объект для работы с базой данных Flat.
     * @param userDAO объект для работы с базой данных пользователей.
     */
    public FlatCollectionManager(FlatDAO flatDAO, UserDAO userDAO) {
        this.flatDAO = flatDAO;
        this.userDAO = userDAO;
        loadCollection();
    }

    /**
     * Создает коллекцию объектов {@link Flat}.
     *
     * @return новая коллекция типа {@link PriorityQueue}.
     */
    @Override
    protected Collection<Flat> createCollection() {
        return new PriorityQueue<>();
    }

    /**
     * Загружает коллекцию объектов Flat из базы данных и сортирует её.
     */
    @Override
    protected void loadCollection() {
        Collection<Flat> loadedCollection = flatDAO.getAllFlats();
        setCollection(loadedCollection);
        sortCollection();  // Сортировка коллекции после загрузки
        setLastInitTime(LocalDateTime.now());
    }

    /**
     * Возвращает идентификатор объекта Flat.
     *
     * @param element объект Flat, для которого нужно получить ID.
     * @return ID объекта Flat.
     */
    @Override
    protected Long getId(Flat element) {
        return element.getId();
    }

    /**
     * Синхронизированный метод для установки новой коллекции объектов Flat.
     *
     * @param collection коллекция объектов Flat для установки.
     */
    private synchronized void setCollection(Collection<Flat> collection) {
        super.getCollection().clear();
        super.getCollection().addAll(collection);
    }

    /**
     * Добавляет объект Flat в коллекцию и базу данных.
     *
     * @param username имя пользователя, добавляющего объект.
     * @param element  объект Flat для добавления.
     * @return ID добавленного объекта или отрицательное значение в случае ошибки.
     */
    @Override
    public Long addToCollection(String username, Flat element) {
        element.setUsername(username);
        long id = flatDAO.insertFlat(element);
        if (id < 0) return id;
        element.setId(id);
        synchronized (this) {
            return super.addToCollection("", element);
        }
    }

    /**
     * Удаляет объект Flat из коллекции и базы данных.
     *
     * @param element объект Flat для удаления.
     * @return {@code true}, если удаление прошло успешно, иначе {@code false}.
     */
    @Override
    public boolean removeFromCollection(Flat element) {
        try {
            if (!flatDAO.removeFlatById(element.getId())) throw new Exception();
        } catch (Exception e) {
            return false;
        }
        synchronized (this) {
            return super.removeFromCollection(element);
        }
    }

    /**
     * Сортирует коллекцию объектов Flat по имени.
     */
    @Override
    public synchronized void sortCollection() {
        Collection<Flat> sortedCollection = getCollection().stream()
                .sorted(Comparator.comparing(Flat::getName))
                .collect(Collectors.toList());
        setCollection(sortedCollection);
    }
}
