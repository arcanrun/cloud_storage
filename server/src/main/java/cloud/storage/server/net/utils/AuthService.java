package cloud.storage.server.net.utils;

import java.sql.*;

public class AuthService {
    private Connection connection;
    private PreparedStatement loginAndPassStmnt;
    public void connect() {

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:cloudstorage.db");
            this.loginAndPassStmnt = connection.prepareStatement("SELECT login, password from user WHERE login = ? AND password = ?");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public User loginUserByLoginAndPass(String login, int password) {
        try {
            loginAndPassStmnt.setString(1, login);
            loginAndPassStmnt.setInt(2, password);
            ResultSet rs = loginAndPassStmnt.executeQuery();
            while (rs.next()) {
                return  new User(rs.getString(1));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public void disconnect() {
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
