package de.berstanio.ghgparser;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.LinkedList;

public class Block implements Serializable {

    private static final long serialVersionUID = -7231586899183399277L;
    private DayOfWeek day;
    private int blockNr;
    private LinkedList<Course> courses = new LinkedList<>();
    public static final Block EMPTY = new Block();

    static {
        Course emptyCourse = new Course();
        emptyCourse.setCourseName("");
        emptyCourse.setRoom("");
        emptyCourse.setTeacher("");
        emptyCourse.setCancelled(true);
        EMPTY.getCourses().add(emptyCourse);
    }

    public DayOfWeek getDay() {
        return day;
    }

    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    public int getBlockNr() {
        return blockNr;
    }

    public void setBlockNr(int blockNr) {
        this.blockNr = blockNr;
    }

    public LinkedList<Course> getCourses() {
        return courses;
    }

    public void setCoursesL(LinkedList<Course> courses) {
        this.courses = courses;
    }
}
