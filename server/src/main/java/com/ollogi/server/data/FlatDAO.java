package com.ollogi.server.data;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.general.models.Coordinates;
import com.general.models.Flat;
import com.general.models.House;
import com.general.models.View;
import com.ollogi.server.managers.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс для работы с данными о квартирах в базе данных.
 * Включает методы для создания таблицы, вставки, удаления, обновления и получения данных о квартирах.
 */
public class FlatDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlatDAO.class);

    // SQL-запрос для создания таблицы "flats"
    private static final String CREATE_FLATS_TABLE_SQL = "CREATE TABLE IF NOT EXISTS flats (" +
            "id BIGSERIAL PRIMARY KEY, " +
            "name VARCHAR NOT NULL, " +
            "coordinates_x BIGINT NOT NULL, " +
            "coordinates_y FLOAT CHECK (coordinates_y > -519) NOT NULL, " +
            "creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "area INT CHECK (area > 0), " +
            "number_of_rooms INT NOT NULL, " +
            "height INT NOT NULL, " +
            "kitchen_area FLOAT CHECK (kitchen_area IS NULL OR kitchen_area > 0), " +
            "view VARCHAR(20) CHECK (view IN ('STREET', 'YARD', 'BAD', 'GOOD')), " +
            "house_name VARCHAR NOT NULL, " +
            "house_year INT CHECK (house_year IS NULL OR house_year > 0), " +
            "house_number_of_flats_on_floor BIGINT CHECK (house_number_of_flats_on_floor IS NULL OR house_number_of_flats_on_floor > 0), " +
            "house_number_of_lifts BIGINT CHECK (house_number_of_lifts > 0), " +
            "username VARCHAR(50), " +
            "FOREIGN KEY (username) REFERENCES users(username)" +
            ");";

    // SQL-запрос для получения всех квартир
    private static final String SELECT_ALL_FLATS_SQL = "SELECT * FROM flats";

    // SQL-запрос для вставки новой квартиры
    private static final String INSERT_FLAT_SQL = "INSERT INTO flats (" +
            "name, coordinates_x, coordinates_y, creation_date, area, number_of_rooms, height, kitchen_area, view, " +
            "house_name, house_year, house_number_of_flats_on_floor, house_number_of_lifts, username) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    // SQL-запрос для удаления квартиры по ID
    private static final String REMOVE_FLAT_SQL = "DELETE FROM flats WHERE id = ?";

    // SQL-запрос для обновления данных о квартире
    private static final String UPDATE_FLAT_SQL = "UPDATE flats SET " +
            "name = ?, coordinates_x = ?, coordinates_y = ?, creation_date = ?, area = ?, number_of_rooms = ?, " +
            "height = ?, kitchen_area = ?, view = ?, house_name = ?, house_year = ?, " +
            "house_number_of_flats_on_floor = ?, house_number_of_lifts = ?, username = ? " +
            "WHERE id = ?";

    // SQL-запрос для получения квартиры по ID
    private static final String SELECT_FLAT_BY_ID_SQL = "SELECT * FROM flats WHERE id = ?";

    // SQL-запрос для получения всех квартир пользователя по имени пользователя
    private static final String SELECT_FLATS_BY_USER_ID_SQL = "SELECT * FROM flats WHERE username = ?";

    /**
     * Метод для создания таблицы "flats" в базе данных.
     * @param connection Подключение к базе данных
     * @throws SQLException Если возникает ошибка при выполнении SQL-запроса
     */
    public void createFlatsTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_FLATS_TABLE_SQL);
        }
    }

    /**
     * Метод для получения всех квартир из базы данных.
     * @param connection Подключение к базе данных
     * @return ResultSet с данными о всех квартирах
     * @throws SQLException Если возникает ошибка при выполнении SQL-запроса
     */
    public ResultSet getAllFlats(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeQuery(SELECT_ALL_FLATS_SQL);
    }

    /**
     * Метод для получения всех квартир как списка объектов Flat.
     * @return Список квартир
     */
    public List<Flat> getAllFlats() {
        List<Flat> flats = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_FLATS_SQL);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Flat flat = mapResultSetToFlat(resultSet);
                flats.add(flat);
            }

        } catch (Exception e) {
            // Логируем ошибку и выбрасываем исключение или обрабатываем его соответствующим образом
            LOGGER.error("Ошибка при получении всех квартир из базы данных", e);
        }

        return flats;
    }

    /**
     * Метод для вставки новой квартиры в базу данных.
     * @param flat Объект Flat с данными о квартире
     * @return ID вставленной квартиры или -1 в случае ошибки
     */
    public long insertFlat(Flat flat) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_FLAT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            // Устанавливаем параметры запроса
            preparedStatement.setString(1, flat.getName());
            preparedStatement.setLong(2, flat.getCoordinates().getX());
            preparedStatement.setFloat(3, flat.getCoordinates().getY());
            preparedStatement.setTimestamp(4, Timestamp.valueOf(flat.getCreationDate().atStartOfDay()));
            preparedStatement.setInt(5, flat.getArea());
            preparedStatement.setInt(6, flat.getNumberOfRooms());
            preparedStatement.setInt(7, flat.getHeight());
            preparedStatement.setFloat(8, flat.getKitchenArea());
            preparedStatement.setString(9, flat.getView().toString());
            preparedStatement.setString(10, flat.getHouse().getName());
            preparedStatement.setInt(11, flat.getHouse().getYear());
            preparedStatement.setLong(12, flat.getHouse().getNumberOfFlatsOnFloor());
            preparedStatement.setLong(13, flat.getHouse().getNumberOfLifts());
            preparedStatement.setString(14, flat.getUsername());

            // Выполняем запрос и получаем ID новой записи
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                }
            }
        } catch (Exception e) {
            // Логируем ошибку
            LOGGER.error("Ошибка при вставке новой квартиры в базу данных", e);
        }
        return -1;
    }

    /**
     * Метод для удаления квартиры по ID.
     * @param id ID квартиры
     * @return true, если квартира была успешно удалена, иначе false
     * @throws SQLException Если возникает ошибка при выполнении SQL-запроса
     */
    public boolean removeFlatById(long id) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(REMOVE_FLAT_SQL)) {

            preparedStatement.setLong(1, id);
            int affectedRows = preparedStatement.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Метод для обновления данных о квартире.
     * @param flat Объект Flat с обновленными данными
     * @return true, если данные были успешно обновлены, иначе false
     * @throws SQLException Если возникает ошибка при выполнении SQL-запроса
     */
    public boolean updateFlat(Flat flat) throws SQLException {
        Connection connection = DatabaseManager.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_FLAT_SQL);
        preparedStatement.setString(1, flat.getName());
        preparedStatement.setLong(2, flat.getCoordinates().getX());
        preparedStatement.setFloat(3, flat.getCoordinates().getY());
        preparedStatement.setTimestamp(4, Timestamp.valueOf(flat.getCreationDate().atStartOfDay()));
        preparedStatement.setInt(5, flat.getArea());
        preparedStatement.setInt(6, flat.getNumberOfRooms());
        preparedStatement.setInt(7, flat.getHeight());
        preparedStatement.setFloat(8, flat.getKitchenArea());
        preparedStatement.setString(9, flat.getView().toString());
        preparedStatement.setString(10, flat.getHouse().getName());
        preparedStatement.setInt(11, flat.getHouse().getYear());
        preparedStatement.setLong(12, flat.getHouse().getNumberOfFlatsOnFloor());
        preparedStatement.setLong(13, flat.getHouse().getNumberOfLifts());
        preparedStatement.setString(14, flat.getUsername());
        preparedStatement.setLong(15, flat.getId());

        int affectedRows = preparedStatement.executeUpdate();
        return affectedRows > 0;
    }

    // Метод для получения квартиры по ID
    public Flat getFlatById(int id) throws SQLException {
        Connection connection = DatabaseManager.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_FLAT_BY_ID_SQL);
        preparedStatement.setInt(1, id);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()) {
            return mapResultSetToFlat(resultSet);
        }
        return null;
    }

    // Метод для получения всех квартир пользователя
    public ResultSet getFlatsByUserId(int userId) throws SQLException {
        Connection connection = DatabaseManager.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_FLATS_BY_USER_ID_SQL);
        preparedStatement.setInt(1, userId);
        return preparedStatement.executeQuery();
    }

    // Приватный метод для маппинга ResultSet в объект Flat
    private Flat mapResultSetToFlat(ResultSet resultSet) throws SQLException {
        // Извлекаем данные из ResultSet
        long id = resultSet.getInt("id");
        String name = resultSet.getString("name");

        Long coordinatesX = resultSet.getLong("coordinates_x");
        Float coordinatesY = resultSet.getFloat("coordinates_y");
        Coordinates coordinates = new Coordinates(coordinatesX, coordinatesY);

        LocalDate creationDate = resultSet.getTimestamp("creation_date").toLocalDateTime().toLocalDate();

        Integer area = resultSet.getInt("area");
        int numberOfRooms = resultSet.getInt("number_of_rooms");
        int height = resultSet.getInt("height");

        Float kitchenArea = resultSet.getFloat("kitchen_area");
        if (resultSet.wasNull()) {  // Проверяем, было ли значение null
            kitchenArea = null;
        }

        String viewString = resultSet.getString("view");
        View view = viewString != null ? View.valueOf(viewString) : null;

        String houseName = resultSet.getString("house_name");

        Integer houseYear = resultSet.getInt("house_year");
        if (resultSet.wasNull()) {
            houseYear = null;
        }

        Long houseNumberOfFlatsOnFloor = resultSet.getLong("house_number_of_flats_on_floor");
        if (resultSet.wasNull()) {
            houseNumberOfFlatsOnFloor = null;
        }

        long houseNumberOfLifts = resultSet.getLong("house_number_of_lifts");

        House house = new House(houseName, houseYear, houseNumberOfFlatsOnFloor, houseNumberOfLifts);

        String username = resultSet.getString("username");
        if (resultSet.wasNull()) {
            username = null;
        }

        // Создаем объект Flat и возвращаем его
        Flat flat = new Flat(name, coordinates, creationDate, area, numberOfRooms, height, kitchenArea, view, house);
        flat.setUsername(username);
        flat.setId(id);  // Если у Flat есть метод setId, чтобы установить идентификатор

        return flat;
    }

}
