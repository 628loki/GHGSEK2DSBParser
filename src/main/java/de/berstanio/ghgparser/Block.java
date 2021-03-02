package de.berstanio.ghgparser;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Representiert einen Stundenblock, mit allen Stunden die er enthält.
 */
public class Block implements Serializable {

    private static final long serialVersionUID = -7231586899183399277L;
    private DayOfWeek day;
    private int blockNr;
    private LinkedList<Course> courses = new LinkedList<>();
    /**
     * Ein leerer Block, welcher als Platzhalter genutzt werden kann
     */
    public static final Block EMPTY = new Block();

    static {
        //Initalisiert den Leeren Block
        Course emptyCourse = new Course();
        emptyCourse.setCourseName("");
        emptyCourse.setRoom("");
        emptyCourse.setTeacher("");
        emptyCourse.setCancelled(true);
        EMPTY.getCourses().add(emptyCourse);
    }

    /**
     * Gibt den Tag zurück, an dem der Block stattfindet
     * @return Der Tag als DayOfWeek, an dem der Block stattfindet
     */
    public DayOfWeek getDay() {
        return day;
    }

    /**
     * Setzt den Tag, an dem der Block stattfindet
     * @param day Der Tag als DayOfWeek, an dem der Block stattfindet
     */
    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    /**
     * Gibt die Stunde zurück, in der der Block liegt
     * @return Die Stunde als int, in der der Block liegt
     */
    public int getBlockNr() {
        return blockNr;
    }

    /**
     * Setzt die Stunde, in der der Block liegt
     * @param blockNr Die Stunde als int, in der der Block liegt
     */
    public void setBlockNr(int blockNr) {
        this.blockNr = blockNr;
    }

    /**
     * Gibt eine Liste aller Stunden zurück, die im Block liegen
     * @return Liste aller Stunden, die im Block liegen
     */
    public LinkedList<Course> getCourses() {
        return courses;
    }

    /**
     * Setzt die Liste aller Stunden, die im Block liegen
     * @param courses Liste aller Stunden, die im Block liegen
     */
    public void setCourses(LinkedList<Course> courses) {
        this.courses = courses;
    }
}
