package com.yourorg.arms.dao;

import com.yourorg.arms.DatabaseConnector;
import com.yourorg.arms.models.Student;
import java.sql.*;

public class StudentDAO {
    public Student getStudentById(int id) throws SQLException {
        String sql = "SELECT id, student_number, username, full_name, password, profile_pic FROM students WHERE id = ?";
        try (Connection c = DatabaseConnector.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student s = new Student();
                    s.setId(rs.getInt("id"));
                    s.setStudentNumber(rs.getString("student_number"));
                    s.setUsername(rs.getString("username"));
                    s.setFullName(rs.getString("full_name"));
                    s.setPassword(rs.getString("password"));
                    Blob blob = rs.getBlob("profile_pic");
                    if (blob != null) {
                        s.setProfilePic(blob.getBytes(1, (int) blob.length()));
                    }
                    return s;
                }
            }
        }
        return null;
    }

    public boolean updateProfilePicture(int studentId, byte[] imageBytes) throws SQLException {
        String sql = "UPDATE students SET profile_pic = ? WHERE id = ?";
        try (Connection c = DatabaseConnector.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            if (imageBytes != null) {
                ps.setBlob(1, new javax.sql.rowset.serial.SerialBlob(imageBytes));
            } else {
                ps.setNull(1, Types.BLOB);
            }
            ps.setInt(2, studentId);
            return ps.executeUpdate() > 0;
        }
    }
}
