package com.yourorg.arms.models;

public class Grade {
    private int id;
    private int studentId;
    private String courseCode;
    private String courseName;
    private Double prelim;
    private Double midterm;
    private Double finals;
    private String remarks;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public Double getPrelim() { return prelim; }
    public void setPrelim(Double prelim) { this.prelim = prelim; }
    public Double getMidterm() { return midterm; }
    public void setMidterm(Double midterm) { this.midterm = midterm; }
    public Double getFinals() { return finals; }
    public void setFinals(Double finals) { this.finals = finals; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
