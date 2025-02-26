package resturant;

import java.sql.*;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    // Update these with your MySQL credentials
    private static final String DB_URL = "jdbc:mysql://localhost:3306/restaurant"; // Replace "restaurant" with your database name
    private static final String DB_USER = "root"; // Replace "root" with your MySQL username
    private static final String DB_PASSWORD = "root"; // Replace with your MySQL password

    // Connect to the MySQL database
    public static Connection connect() throws SQLException {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Include it in your library path.");
            e.printStackTrace();
        }

        // Establish the connection with the provided credentials
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

 static void updateOrderStatusInDatabase(int orderId, String newStatus) {
    String updateSql = "UPDATE Orders SET status = ? WHERE orderId = ?";
    try (Connection conn = DriverManager.getConnection(Database.DB_URL, Database.DB_USER, Database.DB_PASSWORD);
         PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

        pstmt.setString(1, newStatus);
        pstmt.setInt(2, orderId);

        pstmt.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
static void deleteOrderFromDatabase(int orderId) {
    // First, delete related items from the OrderItems table
    String deleteItemsSql = "DELETE FROM OrderItems WHERE orderId = ?";
    try (Connection conn = DriverManager.getConnection(Database.DB_URL, Database.DB_USER, Database.DB_PASSWORD);
         PreparedStatement pstmtItems = conn.prepareStatement(deleteItemsSql)) {

        pstmtItems.setInt(1, orderId);
        pstmtItems.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }

    // Then, delete the order from the Orders table
    String deleteOrderSql = "DELETE FROM Orders WHERE orderId = ?";
    try (Connection conn = DriverManager.getConnection(Database.DB_URL, Database.DB_USER, Database.DB_PASSWORD);
         PreparedStatement pstmtOrder = conn.prepareStatement(deleteOrderSql)) {

        pstmtOrder.setInt(1, orderId);
        pstmtOrder.executeUpdate();
    } catch (SQLException e) {
        e.printStackTrace();
    }
}


}
