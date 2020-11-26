package de.berstanio.ghgparser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.StringJoiner;

public class CoreCourse implements Serializable {

    private String courseName;
    private String teacher;
    private ArrayList<Course> courses = new ArrayList<>();

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public ArrayList<Course> getCourses() {
        return courses;
    }

    public void setCourses(ArrayList<Course> courses) {
        this.courses = courses;
    }
}
