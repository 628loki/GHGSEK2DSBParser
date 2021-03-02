package de.berstanio.ghgparser;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Representiert einen Kurs, welcher gewählt werden kann
 */
public class CoreCourse implements Serializable {

    private String courseName;
    private String teacher;
    //Liste aller Stunden, die zum gewählten Kurs gehören
    private ArrayList<Course> courses = new ArrayList<>();
    public static final long serialVersionUID = -5565522753007317790L;

    public String toString(){
        Course course = courses.get(0);
        return course.getLesson() + ". Stunde am " + course.getDay() + " " + course.getCourseName() + " mit " + course.getTeacher();
    }

    /**
     * Gibt den Namen des Kurses zurück
     * @return Der Name des Kurses als String
     */
    public String getCourseName() {
        return courseName;
    }

    /**
     * Setzt den Namen des Kurses
     * @param courseName Der Name des Kurses als String
     */
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    /**
     * Gibt den Lehrer(Kürzel) zurück, welcher den Kurs betreut
     * @return Der Lehrer als String, welcher den Kurs betreut
     */
    public String getTeacher() {
        return teacher;
    }

    /**
     * Setzt den Lehrer(Kürzel), welcher den Kurs betreut
     * @param teacher Der Lehrer als String, welcher den Kurs betreut
     */
    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    /**
     * Gibt eine Liste von Unterrichtsstunden zurück, welche zum gewählten Kurs gehören
     * @return Eine Liste von Unterrichtsstunden zurück, welche zum gewählten Kurs gehören
     */
    public ArrayList<Course> getCourses() {
        return courses;
    }

    /**
     * Setzt eine Liste von Unterrichtsstunden, welche zum gewählten Kurs gehören
     * @param courses Eine Liste von Unterrichtsstunden zurück, welche zum gewählten Kurs gehören
     */
    public void setCourses(ArrayList<Course> courses) {
        this.courses = courses;
    }
}
