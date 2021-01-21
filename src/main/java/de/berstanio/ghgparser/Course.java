package de.berstanio.ghgparser;

import java.io.Serializable;

//Representiert eine Stunde. Felder sind selbsterklärend oder?
public class Course implements Serializable {

    private static final long serialVersionUID = 6838224638086699525L;
    private String courseName;
    private String teacher;
    private String room;
    private int length;
    private DayOfWeek day;
    private int lesson;
    private boolean cancelled;

    @Override
    public String toString() {
        return "Course{" + "courseName='" + courseName + '\'' +
                ", teacher='" + teacher + '\'' +
                ", room='" + room + '\'' +
                ", length=" + length +
                ", day=" + day +
                ", lesson=" + lesson +
                ", cancelled=" + cancelled +
                '}';
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

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    public int getLesson() {
        return lesson;
    }

    public void setLesson(int lesson) {
        this.lesson = lesson;
    }
}
