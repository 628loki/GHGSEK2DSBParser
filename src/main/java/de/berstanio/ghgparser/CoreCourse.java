package de.berstanio.ghgparser;

import java.io.Serializable;
import java.util.ArrayList;

public class CoreCourse implements Serializable {

    private String courseName;
    private String teacher;
    private ArrayList<Course> courses = new ArrayList<>();
    public static final long serialVersionUID = -5565522753007317790L;

    public String toString(){
        Course course = courses.get(0);
        return course.getLesson() + ". Stunde am " + course.getDay() + " " + course.getCourseName() + " mit " + course.getTeacher()    ;
    }

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
