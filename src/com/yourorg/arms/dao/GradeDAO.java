package com.yourorg.arms.dao;

import com.yourorg.arms.DatabaseConnector;
import com.yourorg.arms.models.Grade;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GradeDAO {
    public List<Grade> getGradesForStudent(int studentId) throws SQLException {
        String sql = "SELECT id, course_code, course_name, prelim, midterm, finals, remarks FROM grades WHERE student_id = ?";
        List<Grade> list = new ArrayList<>();
        try (Connection c = DatabaseConnector.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Grade g = new Grade();
                    g.setId(rs.getInt("id"));
                    g.setStudentId(studentId);
                    g.setCourseCode(rs.getString("course_code"));
                    g.setCourseName(rs.getString("course_name"));
                    double v;
                    v = rs.getDouble("prelim"); g.setPrelim(rs.wasNull()?null:v);
                    v = rs.getDouble("midterm"); g.setMidterm(rs.wasNull()?null:v);
                    v = rs.getDouble("finals"); g.setFinals(rs.wasNull()?null:v);
                    g.setRemarks(rs.getString("remarks"));
                    list.add(g);
                }
            }
        }
        return list;
    }
}
