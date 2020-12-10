package de.berstanio.ghgparser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class GHGParser {

    private static ArrayList<User> users;
    private static String rawHtml;
    private static JahresStundenPlan jahresStundenPlan;
    private static File basedir;

    public static void init(InputStream rawHtmlStream, File basedir) throws IOException, DSBNotLoadableException {
        setBasedir(basedir);
        ArrayList<User> users = User.loadUsers();
        setUsers(users);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(rawHtmlStream));
        String line;
        StringBuilder rawHtmlBuilder = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null){
            rawHtmlBuilder.append(line);
        }
        setRawHtml(rawHtmlBuilder.toString());
        Calendar calendar = Calendar.getInstance();
        int week = calendar.get(Calendar.WEEK_OF_YEAR);

        setJahresStundenPlan(new JahresStundenPlan());
    }

    public static ArrayList<CoreCourse> remainingCoreCourses(ArrayList<CoreCourse> choosen, ArrayList<CoreCourse> remaining){
        ArrayList<String> blockedCourses = new ArrayList<>();
        ArrayList<String> blockedNames = new ArrayList<>();

        choosen.forEach(coreCourse -> {
            for (Course course : coreCourse.getCourses()) {
                blockedCourses.add(course.getDay().name() + course.getLesson());
            }
            if (coreCourse.getCourseName().length() > 4) {
                blockedNames.add(coreCourse.getCourseName().substring(2, 4).toLowerCase());
            }
        });

        remaining.removeIf(coreCourse -> {
            if (blockedCourses.contains(coreCourse.getCourses().get(0).getDay().name() + coreCourse.getCourses().get(0).getLesson()))
                return true;
            if (coreCourse.getCourses().get(0).getCourseName().length() > 3) {
                return blockedNames.contains(coreCourse.getCourseName().substring(2, 4).toLowerCase());
            }
            return false;
        });
        return remaining;

    }

    public static String generateHtmlFile(User user, int week) throws DSBNotLoadableException {
        AtomicReference<String> rawHtmlReference = new AtomicReference<>();
        rawHtmlReference.set(rawHtml);

        Plan plan = new Plan(week);
        plan.normalize();
        HashMap<DayOfWeek, LinkedList<Course>> masked = user.maskPlan(plan.getDayListMap());
        String strikes = "</font><font color=\"#FF0000\" face=\"Arial\" size=\"1\"><strike>con</strike>";
        masked.forEach((dayOfWeek, courses) -> courses.forEach(course -> {
            for (int i = 0; i < course.getLength(); i++) {
                String room = course.getRoom();
                String name = course.getCourseName();
                String teacher = course.getTeacher();
                if (course.isCancelled()){
                    room = strikes.replace("con", room);
                    name = strikes.replace("con", name);
                    teacher = strikes.replace("con", teacher);
                }else if (getJahresStundenPlan().getDayListMap().get(course.getDay()).size() > course.getLesson() - 1 && getJahresStundenPlan().getDayListMap().get(course.getDay()).get(course.getLesson() - 1).getCourses().stream().anyMatch(comp -> comp.getCourseName().equalsIgnoreCase(course.getCourseName())
                        && comp.getTeacher().equalsIgnoreCase(course.getTeacher())
                        && !comp.getRoom().equalsIgnoreCase(course.getRoom()))){
                    room = "</font><font color=\"#FF0000\" face=\"Arial\" size=\"1\">" + room;
                }
                rawHtmlReference.set(rawHtmlReference.get()
                        .replace(course.getDay().name().substring(0, 2) + (course.getLesson() + i) + "R", room)
                        .replace(course.getDay().name().substring(0, 2) + (course.getLesson() + i) + "C", name)
                        .replace(course.getDay().name().substring(0, 2) + (course.getLesson() + i) + "L", teacher));
            }
        }));
        return rawHtmlReference.get();
    }

    public static void close(){
        getUsers().forEach(User::saveUser);
    }

    public static void main(String[] args)  {
        try {
            Class.forName("de.berstanio.ghgparser.Logger");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        setBasedir(new File("user/"));
        Calendar calendar = Calendar.getInstance();
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        ArrayList<User> users = User.loadUsers();
        //jahresStundenPlan2.getCoreCourses().forEach(coreCourse -> System.out.println(coreCourse.getCourseName() + "  " + coreCourse.getTeacher()));
        if (users.isEmpty()) {

            //Plan planThis = new Plan(week);
            //Plan planNext = new Plan(week + 1);
            JahresStundenPlan jahresStundenPlan = null;
            try {
                jahresStundenPlan = new JahresStundenPlan();
            } catch (DSBNotLoadableException e) {
                e.printStackTrace();
            }
            System.out.println(jahresStundenPlan.getToken());
            Scanner scanner = new Scanner(System.in);
            ArrayList<CoreCourse> coursesTmp = jahresStundenPlan.getCoreCourses();
            ArrayList<CoreCourse> choosen = new ArrayList<>();
            while (coursesTmp.size() != 0){
                System.out.println(coursesTmp.get(0).getCourses().get(0).toString());
                String string = scanner.next();
                if (string.equalsIgnoreCase("j")){
                    choosen.add(coursesTmp.get(0));
                    coursesTmp = remainingCoreCourses(choosen, coursesTmp);
                }else {
                    coursesTmp.remove(0);
                    coursesTmp.trimToSize();
                }
            }
            users.add(new User(choosen));
        }
        /*users.forEach(user -> {
            HashMap<DayOfWeek, LinkedList<Course>> masked = user.maskPlan(new Plan(week).getDayListMap());
            masked.forEach((dayOfWeek, courses) -> courses.forEach(System.out::println));
        });*/
        users.forEach(User::saveUser);
        try {
            setJahresStundenPlan(new JahresStundenPlan());
        } catch (DSBNotLoadableException e) {
            e.printStackTrace();
        }
        try {
            // TODO: 27.11.2020 Schwimmen falsch erkannt
            User user = users.get(0);
            List<String> rawHtmlList = Files.readAllLines(Paths.get("rawPage.htm"), StandardCharsets.UTF_8);
            String rawHtml = String.join("", rawHtmlList);

            setRawHtml(rawHtml);
            System.out.println(generateHtmlFile(user, week));
            Files.write(Paths.get("out.htm"), generateHtmlFile(user, week).getBytes(StandardCharsets.ISO_8859_1));
        } catch (IOException | DSBNotLoadableException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<User> getUsers() {
        return users;
    }

    public static void setUsers(ArrayList<User> users) {
        GHGParser.users = users;
    }

    public static String getRawHtml() {
        return rawHtml;
    }

    public static void setRawHtml(String rawHtml) {
        GHGParser.rawHtml = rawHtml;
    }

    public static JahresStundenPlan getJahresStundenPlan() {
        return jahresStundenPlan;
    }

    public static void setJahresStundenPlan(JahresStundenPlan jahresStundenPlan) {
        GHGParser.jahresStundenPlan = jahresStundenPlan;
    }

    public static File getBasedir() {
        return basedir;
    }

    public static void setBasedir(File basedir) {
        GHGParser.basedir = basedir;
    }
}
