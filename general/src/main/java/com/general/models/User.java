package com.general.models;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Класс {@code User} представляет пользователя системы.
 * Он инкапсулирует информацию о пользователе, включая его идентификатор (ID), имя пользователя,
 * хеш пароля, соль и дату регистрации. Этот класс используется для управления
 * аутентификацией и регистрацией пользователей.
 */
@Getter
public class User {
    private final String username;
    private final String passwordHash;
    private final String salt;
    private final LocalDateTime registrationDate;
    @Setter
    private Integer id;

    /**
     * Создает новый объект пользователя с указанными параметрами.
     *
     * @param username         имя пользователя
     * @param passwordHash     хеш пароля пользователя
     * @param salt             соль, используемая для хеширования пароля
     * @param registrationDate дата регистрации пользователя
     */
    public User(String username, String passwordHash, String salt, LocalDateTime registrationDate) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.registrationDate = registrationDate;
    }

    /**
     * Создает новый объект пользователя с указанными параметрами, включая идентификатор.
     *
     * @param id               идентификатор пользователя
     * @param username         имя пользователя
     * @param passwordHash     хеш пароля пользователя
     * @param salt             соль, используемая для хеширования пароля
     * @param registrationDate дата регистрации пользователя
     */
    public User(Integer id, String username, String passwordHash, String salt, LocalDateTime registrationDate) {
        this(username, passwordHash, salt, registrationDate);
        this.id = id;
    }

    /**
     * Возвращает строковое представление объекта пользователя.
     *
     * @return строковое представление объекта пользователя
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                ", salt='" + salt + '\'' +
                ", registrationDate=" + registrationDate +
                '}';
    }

    /**
     * Проверяет валидность объекта пользователя.
     * Убедитесь, что хеш пароля и соль не являются null и имеют минимальную длину 8 символов,
     * а дата регистрации не является null.
     *
     * @return true, если объект пользователя валиден, иначе false
     */
    public boolean validate() {
        if (passwordHash == null || passwordHash.length() < 8) {
            return false;
        }
        if (salt == null || salt.length() < 8) {
            return false;
        }
        return registrationDate != null;
    }
}
