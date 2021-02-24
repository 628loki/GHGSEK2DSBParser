package de.berstanio.ghgparser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Representiert ein Nutzerprofil mit gewählten Kursen.
 */
public class User implements Serializable{

    //Die Liste von gewählten Kursen
    private ArrayList<CoreCourse> coreCourses = new ArrayList<>();
    //Für welches Jahr das Profil gilt.
    private int year;
    public static final long serialVersionUID = -2636346567614007956L;

    public User(ArrayList<CoreCourse> coreCourses, int year){
        setCoreCourses(coreCourses);
        setYear(year);
        saveUser();
    }

    /**
     * Maskiert alle Kurse aus einem Plan weg, die der User nicht belegt hat.
     * @param dayListMap Die Map, aus der die nicht belegten Elemente wegmaskiert werden sollen
     * @return Die Map, nach der maskierung
     */
    //
    public HashMap<DayOfWeek, LinkedList<Course>> maskPlan(HashMap<DayOfWeek, LinkedList<Block>> dayListMap){
        HashMap<DayOfWeek, LinkedList<Course>> newMap = new HashMap<>();

        //Erstmal die Map mit leeren Kursen auffüllen
        newMap.put(DayOfWeek.MONDAY, new LinkedList<>(IntStream.rangeClosed(1, 8).mapToObj(value -> {
                                                        Course course = new Course();
                                                        course.setLength(1);
                                                        course.setLesson(value);
                                                        course.setCourseName("");
                                                        course.setTeacher("");
                                                        course.setRoom("");
                                                        course.setDay(DayOfWeek.MONDAY);
                                                        return course;
                                                    }).collect(Collectors.toList())));
        newMap.put(DayOfWeek.TUESDAY, new LinkedList<>(IntStream.rangeClosed(1, 8).mapToObj(value -> {
                                                        Course course = new Course();
                                                        course.setLength(1);
                                                        course.setLesson(value);
                                                        course.setCourseName("");
                                                        course.setTeacher("");
                                                        course.setRoom("");
                                                        course.setDay(DayOfWeek.TUESDAY);
                                                        return course;
                                                    }).collect(Collectors.toList())));
        newMap.put(DayOfWeek.WEDNESDAY, new LinkedList<>(IntStream.rangeClosed(1, 8).mapToObj(value -> {
                                                        Course course = new Course();
                                                        course.setLength(1);
                                                        course.setLesson(value);
                                                        course.setCourseName("");
                                                        course.setTeacher("");
                                                        course.setRoom("");
                                                        course.setDay(DayOfWeek.WEDNESDAY);
                                                        return course;
                                                    }).collect(Collectors.toList())));
        newMap.put(DayOfWeek.THURSDAY, new LinkedList<>(IntStream.rangeClosed(1, 8).mapToObj(value -> {
                                                        Course course = new Course();
                                                        course.setLength(1);
                                                        course.setLesson(value);
                                                        course.setCourseName("");
                                                        course.setTeacher("");
                                                        course.setRoom("");
                                                        course.setDay(DayOfWeek.THURSDAY);
                                                        return course;
                                                    }).collect(Collectors.toList())));
        newMap.put(DayOfWeek.FRIDAY, new LinkedList<>(IntStream.rangeClosed(1, 8).mapToObj(value -> {
                                                        Course course = new Course();
                                                        course.setLength(1);
                                                        course.setLesson(value);
                                                        course.setCourseName("");
                                                        course.setTeacher("");
                                                        course.setRoom("");
                                                        course.setDay(DayOfWeek.FRIDAY);
                                                        return course;
                                                    }).collect(Collectors.toList())));

        //Geht über alle gewählten Kurse rüber und fügt den aktuellenb Stand(das übergebene) in die Map ein
        //Falls er den Kurs in der übergebenen Liste nicht findet, deutet das auf eine Jahrgangsaktivität hin und Infos zu der wären im 1. Element von BLock
        //Deshalb fügt er dann das hinzu
        getCoreCourses().forEach(coreCourse -> {
            coreCourse.getCourses().forEach(course -> {
                Block block = dayListMap.get(course.getDay()).get(course.getLesson() - 1);
                Optional<Course> optionalCourse = block.getCourses().stream().filter(tmp ->
                        tmp.getTeacher().equalsIgnoreCase(course.getTeacher())
                        && tmp.getCourseName().equalsIgnoreCase(course.getCourseName())).findFirst();

                if (optionalCourse.isPresent()){
                    newMap.get(course.getDay()).set(course.getLesson() - 1, optionalCourse.get());
                }else {
                    newMap.get(course.getDay()).set(course.getLesson() - 1, block.getCourses().get(0));
                }

            });
        });
        return newMap;
    }

    /**
     * Lädt die UserProfile von der Festplatte
     * @return Eine Liste von Usern, die auf der Festplatte gespeichert waren
     */
    //Läd User-Daten
    public static ArrayList<User> loadUsers(){
        File dir = GHGParser.getBasedir();
        ArrayList<User> users = new ArrayList<>();
        if (dir.listFiles() != null) {
            if (dir.listFiles().length == 0) return users;
             //Nimmt alle files, welche kein "plan" enthalten, da das User-Files sind
              Arrays.stream(dir.listFiles()).filter(File::isFile).filter(file -> !file.getName().contains("plan")).forEach(file1 -> {
                try {
                    ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file1));
                    User user = (User) objectInputStream.readObject();
                    objectInputStream.close();
                    users.add(user);
                    file1.delete();
                    user.saveUser();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    file1.delete();
                }
            });
        }
        return users;
    }

    /**
     * Löscht den akutellen User
     */
    public void deleteUser(){
        File dir = GHGParser.getBasedir();
        File file = new File(dir.getAbsolutePath() + "/" + hashCode() + ".yml");
        file.delete();
        GHGParser.getUsers().remove(this);
    }

    /**
     * Speichert den aktuellen User
     */
    //Der Dateiname ist der HashCode des Users. Ist das eine schlechte Idee? HashCodes sind ja auf einzigartigkeit ausgelegt
    public void saveUser(){
        try {
            File dir = GHGParser.getBasedir();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(dir.getAbsolutePath() + "/" + hashCode() + ".yml"));
            objectOutputStream.writeObject(this);
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Gibt die Liste an belegten Kursen zurück
     * @return Die Liste an belegten Kursen
     */
    public ArrayList<CoreCourse> getCoreCourses() {
        return coreCourses;
    }

    /**
     * Setzt die Liste an belegten Kursen
     * @param coreCourses Die Liste an belegten Kursen
     */
    public void setCoreCourses(ArrayList<CoreCourse> coreCourses) {
        this.coreCourses = coreCourses;
    }

    /**
     * Gibt den Jahrgang des Profils zurück
     * @return Der Jahrgang als int
     */
    public int getYear() {
        return year;
    }

    /**
     * Setzt den Jahrgang des Profils
     * @param year Der Jahrgang als int
     */
    public void setYear(int year) {
        this.year = year;
    }
}
