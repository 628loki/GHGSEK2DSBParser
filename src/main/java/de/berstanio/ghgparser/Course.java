package de.berstanio.ghgparser;

import java.io.Serializable;
import java.time.DayOfWeek;

public class Course implements Serializable {

    private String courseName;
    private String teacher;
    private String room;
    private int length;
    private DayOfWeek day;
    private int lesson;
    private boolean cancelled;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Course{");
        sb.append("courseName='").append(courseName).append('\'');
        sb.append(", teacher='").append(teacher).append('\'');
        sb.append(", room='").append(room).append('\'');
        sb.append(", length=").append(length);
        sb.append(", day=").append(day);
        sb.append(", lesson=").append(lesson);
        sb.append(", cancelled=").append(cancelled);
        sb.append('}');
        return sb.toString();
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
