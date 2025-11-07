import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyJDBC {
    private static final String URL = "jdbc:mysql://localhost:3306/banking_simulator";
    private static final String USER = "root";
    private static final String PASSWORD = "gadila@147";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println(" Database connected successfully!");
        } catch (SQLException e) {
            System.out.println(" Database connection failed: " + e.getMessage());
        }
        return conn;
    }
}
