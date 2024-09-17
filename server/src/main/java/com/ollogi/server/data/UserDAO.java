package com.ollogi.server.data;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Properties;

import com.general.models.User;
import com.ollogi.server.managers.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ollogi.server.utility.PasswordHashing.hashPassword;

/**
 * Класс `UserDAO` отвечает за доступ к данным пользователей в базе данных.
 * Он содержит методы для создания таблицы пользователей, добавления,
 * обновления и получения данных пользователей, а также верификации паролей.
 */
public class UserDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDAO.class);

    private final static String CREATE_USERS_TABLE_SQL;
    private final static String SELECT_ALL_USERS_SQL;
    private final static String SELECT_USER_BY_USERNAME_SQL;
    private final static String INSERT_USER_SQL;
    private final static String UPDATE_USER_SQL;
    private final static String SELECT_SALT_BY_USERNAME_SQL;

    static {
        try (InputStream input = UserDAO.class.getClassLoader().getResourceAsStream("queries.properties")) {
            if (input == null) {
                LOGGER.error("Не удалось найти файл свойств запросов");
                throw new RuntimeException("Не удалось найти файл свойств запросов");
            }

            Properties properties = new Properties();
            properties.load(input);

            CREATE_USERS_TABLE_SQL = properties.getProperty("create.users.table");
            SELECT_ALL_USERS_SQL = properties.getProperty("select.all.users");
            SELECT_USER_BY_USERNAME_SQL = properties.getProperty("select.user.by.username");
            INSERT_USER_SQL = properties.getProperty("insert.user");
            UPDATE_USER_SQL = properties.getProperty("update.user");
            SELECT_SALT_BY_USERNAME_SQL = properties.getProperty("select.salt.by.username");

            // Проверка на наличие необходимых значений
            if (CREATE_USERS_TABLE_SQL == null || SELECT_ALL_USERS_SQL == null ||
                SELECT_USER_BY_USERNAME_SQL == null || INSERT_USER_SQL == null ||
                UPDATE_USER_SQL == null || SELECT_SALT_BY_USERNAME_SQL == null) {
                LOGGER.error("Один или несколько SQL-запросов отсутствуют в файле свойств");
                throw new RuntimeException("Один или несколько SQL-запросов отсутствуют в файле свойств");
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка при загрузке файла свойств запросов", e);
            throw new RuntimeException("Ошибка при загрузке файла свойств запросов", e);
        }
    }

    /**
     * Создает таблицу пользователей в базе данных, если она еще не создана.
     *
     * @param connection Соединение с базой данных
     * @throws SQLException Если происходит ошибка при выполнении SQL-запроса
     */
    public void createUsersTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_USERS_TABLE_SQL);
        }
    }

    /**
     * Возвращает всех пользователей из таблицы.
     *
     * @param connection Соединение с базой данных
     * @return Результат запроса в виде ResultSet
     * @throws SQLException Если происходит ошибка при выполнении SQL-запроса
     */
    public ResultSet getAllUsers(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        return statement.executeQuery(SELECT_ALL_USERS_SQL);
    }

    /**
     * Вставляет нового пользователя в базу данных.
     *
     * @param connection Соединение с базой данных
     * @param user Объект пользователя для вставки
     * @return ID нового пользователя, либо -1, если вставка не удалась
     * @throws SQLException Если происходит ошибка при выполнении SQL-запроса
     */
    public int insertUser(Connection connection, User user) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_USER_SQL, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getPasswordHash());
            preparedStatement.setString(3, user.getSalt());
            preparedStatement.setObject(4, user.getRegistrationDate());
            preparedStatement.setObject(5, LocalDateTime.now());

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Возвращает пользователя по его имени.
     *
     * @param connection Соединение с базой данных
     * @param username Имя пользователя
     * @return Объект User, если пользователь найден, или null, если нет
     * @throws SQLException Если происходит ошибка при выполнении SQL-запроса
     */
    public User getUserByUsername(Connection connection, String username) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_USER_BY_USERNAME_SQL)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }
            }
        }
        return null;
    }

    /**
     * Возвращает пользователя по его ID.
     *
     * @param connection Соединение с базой данных
     * @param id ID пользователя
     * @return Объект User, если пользователь найден, или null, если нет
     * @throws SQLException Если происходит ошибка при выполнении SQL-запроса
     */
    public User getUserById(Connection connection, int id) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_USER_BY_USERNAME_SQL)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }
            }
        }
        return null;
    }

    /**
     * Обновляет данные пользователя в базе данных.
     *
     * @param connection Соединение с базой данных
     * @param user Объект пользователя с обновленными данными
     * @return true, если обновление прошло успешно, false в противном случае
     * @throws SQLException Если происходит ошибка при выполнении SQL-запроса
     */
    public boolean updateUser(Connection connection, User user) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_USER_SQL)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getPasswordHash());
            preparedStatement.setInt(3, user.getId());

            int affectedRows = preparedStatement.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Возвращает соль для хэширования пароля по имени пользователя.
     *
     * @param connection Соединение с базой данных
     * @param username Имя пользователя
     * @return Соль для хэширования, либо null, если пользователь не найден
     * @throws SQLException Если происходит ошибка при выполнении SQL-запроса
     */
    public String getSaltByUsername(Connection connection, String username) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SALT_BY_USERNAME_SQL)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("salt");
                }
            }
        }
        return null;
    }

    /**
     * Приватный метод для преобразования данных из ResultSet в объект User.
     *
     * @param resultSet Набор данных из SQL-запроса
     * @return Объект User с данными из ResultSet
     * @throws SQLException Если происходит ошибка при извлечении данных
     */
    private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String username = resultSet.getString("username");
        String passwordHash = resultSet.getString("password_hash");
        String salt = resultSet.getString("salt");

        LocalDateTime registrationDate = resultSet.getTimestamp("registration_date").toLocalDateTime();

        return new User(id, username, passwordHash, salt, registrationDate);
    }

    /**
     * Верифицирует пароль пользователя, сравнивая введенный пароль с сохраненным в базе данных.
     *
     * @param username Имя пользователя
     * @param password Введенный пароль
     * @return true, если пароль совпадает, false в противном случае
     */
    public boolean verifyUserPassword(String username, String password) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_BY_USERNAME_SQL)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String storedPasswordHash = resultSet.getString("password_hash");
                    String storedSalt = resultSet.getString("salt");
                    String enteredPasswordHash = hashPassword(password, storedSalt);
                    return storedPasswordHash.equals(enteredPasswordHash);
                } else {
                    return false; // Пользователь не найден
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Ошибка при верификации пароля пользователя: {}", e.getMessage());
            return false;
        }
    }
}
