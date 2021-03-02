package de.berstanio.ghgparser;

import java.io.Serializable;

/**
 * Representiert eine Unterrichtsstunde
 */
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

    /**
     * Gibt den Kursnamen der Stunde zurück
     * @return Der Kursname der Stunde als String
     */
    public String getCourseName() {
        return courseName;
    }

    /**
     * Setzt den Kursnamen der Stunde
     * @param courseName Der Kursname der Stunde als String
     */
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    /**
     * Gibt das Kürzel des Lehrers zurück, der die Stunde beaufsichtigt
     * @return Das Kürzel des Lehrers als String
     */
    public String getTeacher() {
        return teacher;
    }

    /**
     * Setzt das Kürzel des Lehrers, der die Stunde beaufsichtigt
     * @param teacher Das Kürzel des Lehrers als String
     */
    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    /**
     * Gibt den Raum zurück, in dem die Stunde stattfindet
     * @return Der Raum als String
     */
    public String getRoom() {
        return room;
    }

    /**
     * Setzt den Raum, in dem die Stunde stattfindet
     * @param room Der Raum als String
     */
    public void setRoom(String room) {
        this.room = room;
    }

    /**
     * Gibt die Länge der Stunde zurück(z.B. 2 Blöcke -&gt; 2)
     * @return Die Länge der Stunde als int
     */
    public int getLength() {
        return length;
    }

    /**
     * Setzt die Länge der Stunde(z.B. 2 Blöcke -&gt; 2)
     * @param length Die Länge der Stunde als int
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Gibt zurück, ob die Stunde ausfällt
     * @return Der Ausfall-Status als boolean
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Setzt, ob die Stunde ausfällt
     * @param cancelled Der Ausfall-Status als boolean
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Gibt den Tag an dem die Stunde stattfindet zurück
     * @return Der Tag als DayOfWeek
     */
    public DayOfWeek getDay() {
        return day;
    }

    /**
     * Setzt den Tag an dem die Stunde stattfindet
     * @param day Der Tag als DayOfWeek
     */
    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    /**
     * Gibt die Stunde zurück, in der die Unterrichtsstunde liegt(1. Stunde = 1)
     * @return Die Stunde als int
     */
    public int getLesson() {
        return lesson;
    }

    /**
     * Setzt die Stunde, in der die Unterrichtsstunde liegt(1. Stunde = 1)
     * @param lesson Die Stunde als int
     */
    public void setLesson(int lesson) {
        this.lesson = lesson;
    }
}
