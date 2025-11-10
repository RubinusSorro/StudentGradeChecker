package com.yourorg.arms;

public class DatabaseConfig {
    public static final String DB_HOST = "localhost";
    public static final int DB_PORT = 3306;
    public static final String DB_NAME = "student_grade_checker";
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = ""; // leave blank if default XAMPP

    public static String getJdbcUrl() {
        return "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + "?useSSL=false&serverTimezone=UTC";
    }
}