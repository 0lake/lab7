package com.ollogi.server.commands;

import com.general.command.Command;
import com.general.exceptions.WrongAmountOfElementsException;
import com.general.managers.CollectionManager;
import com.general.models.User;
import com.general.models.base.Element;
import com.general.network.Request;
import com.general.network.Response;
import com.ollogi.server.data.UserDAO;
import com.ollogi.server.managers.DatabaseManager;

public class Login extends Command {
    private UserDAO userDAO;
    public Login(UserDAO userDAO) {
        super("login <login> <password>", "для входа в систему");
        this.userDAO = userDAO;
    }

    /**
     * Выполняет команду
     * @return Response с результатом выполнения команды.
     */
    @Override
    public Response execute(Request request) {
        try {
            String username = request.getLogin();
            String password = request.getPassword();

            if (!userDAO.verifyUserPassword(username, password)) {
                return new Response(false, "Invalid username or password", null);
            }

            User user = userDAO.getUserByUsername(DatabaseManager.getConnection(), username);

            if (user == null) {
                return new Response(false, "User not found", null);
            }

            if (user.getId() == null) {
                return new Response(false, "User ID is null", null);
            }

            return new Response(true, "You have successfully logged in", user.getUsername());
        } catch (Exception e) {
            System.out.println("Exception during login: " + e); // Debug message
            return new Response(false, e.toString(), null);
        }
    }
}
