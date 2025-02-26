
package resturant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Login {

    // Validate user credentials using the database
    public boolean validateCredentials(String username, String password, String role) {
        System.out.println("Working Directory: " + System.getProperty("user.dir"));

        String query = "SELECT * FROM Users WHERE username = ? AND password = ? AND role = ?";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password); // Passwords are stored in plain text for now
            pstmt.setString(3, role);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Returns true if a matching user is found
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Register a new user (for future use if required)
    public boolean registerUser(String username, String password, String role) {
        String insertQuery = "INSERT INTO Users (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = Database.connect(); PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password); // Use hashPassword() for added security
            pstmt.setString(3, role);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // Returns true if a row is inserted
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}



