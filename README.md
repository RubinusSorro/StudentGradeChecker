# ARMS_StudentGradeChecker (Eclipse project)

## Quick start

1. Ensure XAMPP (or MySQL) is running and you have created the database using `sql/create_db.sql`.
2. Open Eclipse (Java 8).
3. Import the project:
   - File → Import → Existing Projects into Workspace → Select the folder `ARMS_StudentGradeChecker`.
4. Add MySQL Connector/J (JDBC driver) to the project build path:
   - Download from: https://dev.mysql.com/downloads/connector/j/
   - Right-click project → Build Path → Add External Archives... → select the `mysql-connector-java-*.jar`
5. Edit `com.yourorg.arms.DatabaseConfig` if your DB credentials differ.
6. Run `com.yourorg.arms.MainApp` (or integrate your existing login to pass the logged-in student id).

## Notes
- Java version: 1.8 (Java 8)
- This project does not include the MySQL Connector/J JAR (you must add it manually).
- SQL file `sql/create_db.sql` contains the database and sample data creation commands.
