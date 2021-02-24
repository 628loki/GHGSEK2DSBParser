package de.berstanio.ghgparser;

/**
 * Einfache Enum, welche die Wochentage representiert
 */
public enum DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    /**
     * Gibt zu einem Wochentag als int(intuitive Zählung von 1-7;Mo-So) das dazugehörige DayOfWeek-Objekt
     * @param dayOfWeek Der Wochentag als int(1-7 -&gt; Mo-So)
     * @return Das DayOfWeek-Objekt, was zum {@code dayOfWeek} int gehört
     */
    public static DayOfWeek of(int dayOfWeek) {
        return DayOfWeek.values()[dayOfWeek - 1];
    }
}
