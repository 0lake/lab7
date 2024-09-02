package com.ollogi.server.commands;

import com.general.command.Command;
import com.general.exceptions.InvalidFormException;
import com.general.exceptions.WrongAmountOfElementsException;
import com.general.managers.CollectionManager;
import com.general.models.User;
import com.general.models.base.Element;
import com.general.network.Request;
import com.general.network.Response;
import com.ollogi.server.data.UserDAO;
import com.ollogi.server.managers.DatabaseManager;
import com.ollogi.server.utility.PasswordHashing;

import javax.management.InstanceAlreadyExistsException;
import java.time.LocalDateTime;

public class Register extends Command {
    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_USERNAME_LENGTH = 50;
    private UserDAO userDAO;

    public Register(UserDAO userDAO) {
        super("register <login> <password>", "команда регистрации в системе");
        this.userDAO = userDAO;
    }

    /**
     * Выполняет команду
     *
     * @return Response с результатом выполнения команды.
     */
    @Override
    public Response execute(Request request) {
        try {
            if (request.getLogin().length() >= MAX_USERNAME_LENGTH)
                throw new InvalidFormException("Username length must be less than " + MAX_USERNAME_LENGTH);

            if (request.getPassword().length() < MIN_PASSWORD_LENGTH)
                throw new InvalidFormException("Password length must be at least " + MIN_PASSWORD_LENGTH);

            if (request.getUserId() != null) throw new InstanceAlreadyExistsException("User already exists");
            String[] cryptoData = PasswordHashing.hashPassword(request.getPassword());
            User user = new User(request.getLogin(),
                    cryptoData[0],
                    cryptoData[1],
                    LocalDateTime.now());
            var id = userDAO.insertUser(DatabaseManager.getConnection(), user);

            if (id < 0) throw new InstanceAlreadyExistsException("User already exists");

            user.setId(id);

            if (!user.validate())
                throw new InvalidFormException("User not registered, user fields are not valid!");

            return new Response(true, "User successfully registered", user.getId());
        } catch (InstanceAlreadyExistsException ex) {
            return new Response(false, ex.getMessage(), null);
        } catch (InvalidFormException invalid) {
            return new Response(false, invalid.getMessage());
        } catch (Exception e) {
            return new Response(false, e.toString(), -1);
        }
    }
}
