package com.ollogi.server.managers;

import com.ollogi.server.data.FlatDAO;
import com.ollogi.server.data.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

import static com.ollogi.server.managers.ConnectionManager.*;


/**
 * Класс {@code DatabaseManager} управляет операциями с базой данных, включая её создание,
 * создание таблиц и управление пользователями.
 *
 * Этот класс обеспечивает взаимодействие с базой данных, гарантируя, что необходимые
 * таблицы созданы, и предоставляет методы для получения соединений с базой данных.
 */
public class DatabaseManager {
    private static final UserDAO userDAO = new UserDAO();
    private static final FlatDAO flatDAO = new FlatDAO();
    private static final Logger logger = LoggerFactory.getLogger("DatabaseManager");
    private static final ThreadLocal<Connection> threadLocalConnection = new ThreadLocal<>();


    /**
     * Возвращает соединение с базой данных для текущего потока.
     * Если соединение не установлено или закрыто, оно создается заново.
     *
     * @return Объект {@link Connection}, представляющий соединение с базой данных.
     * @throws SQLException Если возникает ошибка при попытке установить соединение.
     */
    public static Connection getConnection() throws SQLException {
        Connection connection = threadLocalConnection.get();

        if (connection == null || connection.isClosed()) {
            connection = ConnectionManager.getConnection();
            threadLocalConnection.set(connection);
        }

        return connection;
    }

    /**
     * Создаёт базу данных, если она еще не существует, и инициализирует таблицы.
     * Если база данных уже существует, создаются только отсутствующие таблицы.
     */
    public static void createDatabaseIfNotExists() {
        try (Connection connection = getConnection()) {
            if (connection != null) {
                boolean databaseExists = checkDatabaseExists(connection);
                if (!databaseExists) {
                    executeUpdate(connection, "CREATE DATABASE " + dbName);
                    logger.info("База данных и таблицы успешно созданы.");
                } else {
                    logger.info("База данных уже существует.");
                }
                createTablesIfNotExist(connection);
            } else {
                logger.error("Не удалось установить соединение с базой данных.");
            }
        } catch (SQLException e) {
            logger.error("Ошибка при создании базы данных: {}", e.getMessage());
        }
    }

    /**
     * Проверяет, существует ли база данных.
     *
     * @param connection Соединение с базой данных.
     * @return {@code true}, если база данных существует, иначе {@code false}.
     * @throws SQLException Если возникает ошибка при выполнении SQL-запроса.
     */
    private static boolean checkDatabaseExists(Connection connection) throws SQLException {
        return connection.getMetaData().getCatalogs().next();
    }

    /**
     * Создает необходимые таблицы, если они еще не существуют.
     *
     * @param connection Соединение с базой данных.
     */
    public static void createTablesIfNotExist(Connection connection) {
        if (connection != null) {
            try {
                userDAO.createUsersTable(connection);
                flatDAO.createFlatsTable(connection);
                logger.info("Таблицы успешно созданы (если они не существовали).");
            } catch (SQLException e) {
                throw new RuntimeException("Ошибка при создании таблиц: ", e);
            }
        } else {
            logger.error("Соединение равно null.");
        }
    }

}
