package com.yourorg.arms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
	private static final String URL = "jdbc:mysql://127.0.0.1:3306/student_grade_checker?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root"; // XAMPP default user
    private static final String PASSWORD = ""; // Leave blank unless you set one in phpMyAdmin

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // MySQL connector driver
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}