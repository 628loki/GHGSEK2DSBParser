package de.berstanio.ghgparser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.util.*;
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
        ArrayList<CoreCourse> coreCoursesLoaded = loadSelectedCoreCourses();
        User user;
        if (loadSelectedCoreCourses() == null) {

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
            user = new User(coreCourses);
        }else {
            user = new User(coreCoursesLoaded);
        }
        HashMap<DayOfWeek, LinkedList<Course>> masked = user.maskPlan(new Plan(week).getDayListMap());
        masked.forEach((dayOfWeek, courses) -> {
            courses.forEach(course -> {
                System.out.println(course.toString());
            });
        });
        saveSelectedCoreCourses(user.getCoreCourses());
    }

    public static ArrayList<CoreCourse> loadSelectedCoreCourses(){
        if (Files.exists(Paths.get("save.yml"))){
            try {
                List<String> strings =  Files.readAllLines(Paths.get("save.yml"), StandardCharsets.ISO_8859_1);
                String join = String.join("", strings);
                byte[] data = Base64.getDecoder().decode(join);
                ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data));
                ArrayList<CoreCourse> courses = (ArrayList<CoreCourse>) objectInputStream.readObject();
                return courses;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void saveSelectedCoreCourses(ArrayList<CoreCourse> courses){
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(courses);
            objectOutputStream.close();
            byte[] save = Base64.getEncoder().encode(byteArrayOutputStream.toByteArray());
            Files.write(Paths.get("save.yml"), save);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
