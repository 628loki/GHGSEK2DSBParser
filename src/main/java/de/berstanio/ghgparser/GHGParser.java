package de.berstanio.ghgparser;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

public class GHGParser {

    public static void main(String[] args) {
        Calendar calendar = Calendar.getInstance();
        int week = calendar.get(Calendar.WEEK_OF_YEAR);

        //Plan planThis = new Plan(week);
        //Plan planNext = new Plan(week + 1);
        JahresStundenPlan jahresStundenPlan = new JahresStundenPlan(week);
        System.out.println(jahresStundenPlan.getToken());
        Scanner scanner = new Scanner(System.in);
        ArrayList<String> blockedCourses = new ArrayList<>();
        ArrayList<String> blockedNames = new ArrayList<>();
        ArrayList<CoreCourse> coreCourses = (ArrayList<CoreCourse>) jahresStundenPlan.getCoreCourses().stream().filter(coreCourse -> {
            if (blockedCourses.contains(coreCourse.getCourses().get(0).getDay().name() + coreCourse.getCourses().get(0).getLesson())) return false;
            if (coreCourse.getCourses().get(0).getCourseName().length() > 4) {
                if (blockedNames.contains(coreCourse.getCourses().get(0).getCourseName().substring(2, 4).toLowerCase()))
                    return false;
            }

            System.out.println(coreCourse.getCourses().get(0));
            String string = scanner.next();
            if (string.equalsIgnoreCase("j")){
                for (Course course : coreCourse.getCourses()){
                    blockedCourses.add(course.getDay().name() + course.getLesson());
                }
                if (coreCourse.getCourses().get(0).getCourseName().length() > 4) {
                    blockedNames.add(coreCourse.getCourses().get(0).getCourseName().substring(2, 4).toLowerCase());
                }
                return true;
            }else {
                return false;
            }
        }).collect(Collectors.toList());
        User user = new User(coreCourses);
        HashMap<DayOfWeek, LinkedList<Course>> masked = user.maskPlan(new Plan(week).getDayListMap());
        masked.forEach((dayOfWeek, courses) -> {
            courses.forEach(course -> {
                System.out.println(course.toString());
            });
        });
    }

}
