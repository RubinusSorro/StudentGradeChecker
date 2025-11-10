CREATE DATABASE IF NOT EXISTS student_grade_checker CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE student_grade_checker;

CREATE TABLE IF NOT EXISTS students (
  id INT AUTO_INCREMENT PRIMARY KEY,
  student_number VARCHAR(50) UNIQUE NOT NULL,
  username VARCHAR(100) NOT NULL,
  full_name VARCHAR(150) NOT NULL,
  profile_pic LONGBLOB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS grades (
  id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL,
  course_code VARCHAR(50) NOT NULL,
  course_name VARCHAR(150),
  prelim DECIMAL(5,2),
  midterm DECIMAL(5,2),
  finals DECIMAL(5,2),
  remarks VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);

INSERT INTO students (student_number, username, full_name)
VALUES ('2025-001', 'rhangel', 'Rhangel Mansilla');
