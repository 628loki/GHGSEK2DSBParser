package de.berstanio.ghgparser;
//Einfache Enum, welche die Wochentage representiert
public enum DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    public static DayOfWeek of(int dayOfWeek) {
        return DayOfWeek.values()[dayOfWeek - 1];
    }
}
