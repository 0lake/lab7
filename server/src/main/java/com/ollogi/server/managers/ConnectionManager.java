package com.ollogi.server.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 *  Класс {@code ConnectionManager} управляет подключениями к базе данных и выполнением SQL-запросов.
 * <p>
 * Этот класс предоставляет методы для получения и закрытия соединений, выполнения SQL-запросов и обработки ошибок.
 */
public class ConnectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ConnectionManager");
    private static String dbUrl;
    public static final String dbName;
    private static String user;
    private static String password;

    static {
        InputStream input = null;
        try {
            input = ConnectionManager.class.getClassLoader().getResourceAsStream("database.properties");
            if (input == null) {
                LOGGER.error("Не удалось найти файл свойств");
                throw new RuntimeException("Не удалось найти файл свойств");
            }

            Properties properties = new Properties();
            properties.load(input);

            dbUrl = properties.getProperty("db.url.alt");
            dbName = properties.getProperty("db.name");
            user = properties.getProperty("db.user");
            password = properties.getProperty("db.password");

            // Проверка на наличие необходимых значений
            if (dbUrl == null || dbName == null || user == null || password == null) {
                LOGGER.error("Один или несколько параметров подключения отсутствуют в файле свойств");
                throw new RuntimeException("Один или несколько параметров подключения отсутствуют в файле свойств");
            }
        } catch (IOException e) {
            LOGGER.error("Ошибка при загрузке файла свойств", e);
            throw new RuntimeException("Ошибка при загрузке файла свойств", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.error("Ошибка при закрытии файла свойств", e);
                }
            }
        }
    }

    /**
     * Получает соединение с базой данных.
     *
     * @return Объект {@link Connection}, представляющий соединение с базой данных, или {@code null}, если соединение не удалось установить.
     */
    public static Connection getConnection() {
        try {
            // Возвращаем соединение с базой данных с использованием указанных учетных данных.
            return DriverManager.getConnection(dbUrl + dbName, user, password);
        } catch (SQLException e) {
            logError("Не удалось установить соединение", e);
            return null;
        }
    }

    /**
     * Закрывает соединение с базой данных.
     *
     * @param connection Соединение с базой данных, которое нужно закрыть.
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logError("Ошибка при закрытии соединения", e);
            }
        }
    }

    /**
     * Логгирует сообщение об ошибке с возможностью указания исключения {@link SQLException}.
     *
     * @param message Сообщение об ошибке.
     * @param e       Исключение, вызвавшее ошибку (может быть {@code null}).
     */
    private static void logError(String message, SQLException e) {
        if (e == null) {
            LOGGER.error(message);
        } else {
            LOGGER.error("{}: {}", message, e.getMessage());
        }
    }

    /**
     * Создает SQL-запрос с помощью указанного соединения.
     *
     * @param connection Соединение с базой данных.
     * @return Объект {@link Statement}, представляющий SQL-запрос, или {@code null}, если не удалось создать запрос.
     */
    private static Statement createStatement(Connection connection) {
        if (connection == null) {
            logError("Соединение равно null", null);
            return null;
        }
        try {
            return connection.createStatement();
        } catch (SQLException e) {
            logError("Ошибка при создании запроса", e);
            return null;
        }
    }

    /**
     * Выполняет SQL-операцию обновления (например, INSERT, UPDATE, DELETE) с использованием указанного запроса.
     *
     * @param statement SQL-запрос для выполнения.
     * @param sql       SQL-команда для выполнения.
     */
    public static void executeUpdate(Statement statement, String sql) {
        if (statement == null) {
            logError("Запрос равен null", null);
            return;
        }
        try {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            logError("Ошибка при выполнении обновления", e);
        }
    }

    /**
     * Выполняет SQL-операцию обновления с использованием указанного соединения.
     *
     * @param connection Соединение с базой данных.
     * @param sql        SQL-команда для выполнения.
     */
    public static void executeUpdate(Connection connection, String sql) {
        Statement statement = createStatement(connection);
        executeUpdate(statement, sql);
    }

    /**
     * Выполняет SQL-операцию обновления с использованием подготовленного SQL-запроса ({@link PreparedStatement}).
     *
     * @param statement Подготовленный SQL-запрос для выполнения.
     * @return Результат выполнения запроса (количество затронутых строк), или -1 в случае ошибки.
     */
    public static int executePrepareUpdate(PreparedStatement statement) {
        if (statement == null) {
            logError("Запрос равен null", null);
            return -1;
        } else {
            try {
                return statement.executeUpdate();
            } catch (SQLException e) {
                logError("Ошибка при выполнении подготовленного обновления", e);
                return -1;
            }
        }
    }
}
