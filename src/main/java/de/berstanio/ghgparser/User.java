package de.berstanio.ghgparser;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class User {

    private ArrayList<CoreCourse> coreCourses = new ArrayList<>();

    public User(ArrayList<CoreCourse> coreCourses){
        setCoreCourses(coreCourses);
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

    public ArrayList<CoreCourse> getCoreCourses() {
        return coreCourses;
    }

    public void setCoreCourses(ArrayList<CoreCourse> coreCourses) {
        this.coreCourses = coreCourses;
    }
}
