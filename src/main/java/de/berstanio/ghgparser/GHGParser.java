package de.berstanio.ghgparser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class GHGParser {

    public static void main(String[] args) {
        try {
            Class.forName("de.berstanio.ghgparser.Logger");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Calendar calendar = Calendar.getInstance();
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        ArrayList<User> users = User.loadUsers();
        if (users.isEmpty()) {

            //Plan planThis = new Plan(week);
            //Plan planNext = new Plan(week + 1);
            JahresStundenPlan jahresStundenPlan = new JahresStundenPlan(week);
            System.out.println(jahresStundenPlan.getToken());
            Scanner scanner = new Scanner(System.in);
            ArrayList<String> blockedCourses = new ArrayList<>();
            ArrayList<String> blockedNames = new ArrayList<>();
            ArrayList<CoreCourse> coreCourses = (ArrayList<CoreCourse>) jahresStundenPlan.getCoreCourses().stream().filter(coreCourse -> {
                if (blockedCourses.contains(coreCourse.getCourses().get(0).getDay().name() + coreCourse.getCourses().get(0).getLesson()))
                    return false;
                if (coreCourse.getCourses().get(0).getCourseName().length() > 4) {
                    if (blockedNames.contains(coreCourse.getCourses().get(0).getCourseName().substring(2, 4).toLowerCase()))
                        return false;
                }
                System.out.println(coreCourse.getCourses().get(0));
                String string = scanner.next();
                if (string.equalsIgnoreCase("j")) {
                    for (Course course : coreCourse.getCourses()) {
                        blockedCourses.add(course.getDay().name() + course.getLesson());
                    }
                    if (coreCourse.getCourses().get(0).getCourseName().length() > 4) {
                        blockedNames.add(coreCourse.getCourses().get(0).getCourseName().substring(2, 4).toLowerCase());
                    }
                    return true;
                } else {
                    return false;
                }
            }).collect(Collectors.toList());
            users.add(new User(coreCourses));
        }
        /*users.forEach(user -> {
            HashMap<DayOfWeek, LinkedList<Course>> masked = user.maskPlan(new Plan(week).getDayListMap());
            masked.forEach((dayOfWeek, courses) -> courses.forEach(System.out::println));
        });*/
        users.forEach(User::saveUser);

        try {
            // TODO: 27.11.2020 Schwimmen falsch erkannt
            User user = users.get(0);
            List<String> rawHtmlList = Files.readAllLines(Paths.get("rawPage.htm"), StandardCharsets.UTF_8);
            String rawHtml = String.join("", rawHtmlList);
            AtomicReference<String> rawHtmlReference = new AtomicReference<>();
            rawHtmlReference.set(rawHtml);
            Plan plan = new Plan(week);
            plan.normalize();
            HashMap<DayOfWeek, LinkedList<Course>> masked = user.maskPlan(plan.getDayListMap());
            masked.forEach((dayOfWeek, courses) -> courses.forEach(course -> {
                for (int i = 0; i < course.getLength(); i++) {
                    rawHtmlReference.set(rawHtmlReference.get()
                            .replace(course.getDay().name().substring(0, 2) + (course.getLesson() + i) + "R", course.getRoom())
                            .replace(course.getDay().name().substring(0, 2) + (course.getLesson() + i) + "C", course.getCourseName())
                            .replace(course.getDay().name().substring(0, 2) + (course.getLesson() + i) + "L", course.getTeacher()));
                }
            }));
            Files.write(Paths.get("out.htm"), rawHtmlReference.get().getBytes(StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
