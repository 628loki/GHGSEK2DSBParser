package de.berstanio.ghgparser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class User implements Serializable{

    private ArrayList<CoreCourse> coreCourses = new ArrayList<>();
    private int year;
    public static final long serialVersionUID = -2636346567614007956L;

    public User(ArrayList<CoreCourse> coreCourses, int year){
        setCoreCourses(coreCourses);
        setYear(year);
        saveUser();
    }

    public HashMap<DayOfWeek, LinkedList<Course>> maskPlan(HashMap<DayOfWeek, LinkedList<Block>> dayListMap){
        HashMap<DayOfWeek, LinkedList<Course>> newMap = new HashMap<>();

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

    public static ArrayList<User> loadUsers(){
        File dir = GHGParser.getBasedir();
        ArrayList<User> users = new ArrayList<>();
        if (dir.listFiles() != null) {
            if (dir.listFiles().length == 0) return users;
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

    public void deleteUser(){
        File dir = GHGParser.getBasedir();
        File file = new File(dir.getAbsolutePath() + "/" + hashCode() + ".yml");
        file.delete();
        GHGParser.getUsers().remove(this);
    }

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


    public ArrayList<CoreCourse> getCoreCourses() {
        return coreCourses;
    }

    public void setCoreCourses(ArrayList<CoreCourse> coreCourses) {
        this.coreCourses = coreCourses;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
