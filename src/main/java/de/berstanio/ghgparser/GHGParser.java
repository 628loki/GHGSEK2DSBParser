package de.berstanio.ghgparser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class GHGParser {

    //Eine Liste von Nutzer-Profilen
    private static ArrayList<User> users;
    //Eine HTML-Datei, welche Platzhalter hat zum einfachen einsetzen der Stunden
    private static String rawHtml;
    //Der Jahrestundenplan der 11
    private static JahresStundenPlan jahresStundenPlan11;
    //Der Jahrestundenplan der 12
    private static JahresStundenPlan jahresStundenPlan12;
    //Der Ordner, in dem alles gespeichert werden soll
    private static File basedir;
    //Eine Mapping Tabelle, zum anpassen des Plan-HTMLs, welches runtergeladen wird, auf einen Standard(für die 11.)
    private static HashMap<String, String> toReplace11 = new HashMap<>();
    //Eine Mapping Tabelle, zum anpassen des Plan-HTMLs, welches runtergeladen wird, auf einen Standard(für die 12.)
    private static HashMap<String, String> toReplace12 = new HashMap<>();

    //Die init Methode, welche alles nötige initalisiert(Die Profile, Mappings etc.)
    public static void init(InputStream rawHtmlStream, File basedir) throws IOException, DSBNotLoadableException {
        setBasedir(basedir);

        readMappings();
        ArrayList<User> users = User.loadUsers();
        setUsers(users);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(rawHtmlStream));
        String line;
        StringBuilder rawHtmlBuilder = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null){
            rawHtmlBuilder.append(line);
        }
        setRawHtml(rawHtmlBuilder.toString());

        setJahresStundenPlan(new JahresStundenPlan(11));
        setJahresStundenPlan(new JahresStundenPlan(12));
    }

    //Eine Methode, welche eine List von bereis gewählten Kursen als Input krigt und dann alle möglichen Kurse und zurück gibt,
    //welche Kurse noch wählbar sind. Eigentlich wird sie nicht mehr genutzt, wie mir gerade beim schreiben auffällt, weshalb ich sie ausbauen könnte...
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
        return generateHtmlFile(user, new Plan(user.getYear(), week));
    }

    //Die Funktion generiert die fertige HTML-Datei.
    public static String generateHtmlFile(User user, Plan plan) throws DSBNotLoadableException {
        AtomicReference<String> rawHtmlReference = new AtomicReference<>();
        rawHtmlReference.set(rawHtml);

        //bringt den runtergeladenen Plan in eine Standard-Form
        plan.normalize();
        //Maskiert alle Kurse weg, die nicht vom Nutzer belegt wurden
        HashMap<DayOfWeek, LinkedList<Course>> masked = user.maskPlan(plan.getDayListMap());
        //Ein HTML-String, damit ich leichter etwas durchstreichen kann. "con" ist der Platzhalter von dem, was durchgetrichen werden soll
        String strikes = "</font><font color=\"#FF0000\" face=\"Arial\" size=\"1\"><strike>con</strike>";
        //Über alle belegten Kurse in ihrem momentanen Zustand rüberiterieren(Zwei Lambdas(man sieht es schlecht))
        masked.forEach((dayOfWeek, courses) -> courses.forEach(course -> {
            //Ein Kurs kann z.B. 2 Stunden lan gehen. Jede Stunde muss aber eingetragen werden. Deshalb wird der Kurs so lange untereinander
            //eingetragen, wie er lang ist
            for (int i = 0; i < course.getLength(); i++) {
                String room = course.getRoom();
                String name = course.getCourseName();
                String teacher = course.getTeacher();
                //Wenn der Kurs ausfällt, ihn durchgestrichen anzeigen
                if (course.isCancelled()){
                    room = strikes.replace("con", room);
                    name = strikes.replace("con", name);
                    teacher = strikes.replace("con", teacher);
                    //Das soll irgendwie erkennen, ob sich ein Raum geändert hat, ich kann dir aber beim besten willen nicht mehr sagen,
                    //warum das so kompliziert sein musste... Vllt. fällt es mir ja wieder ein.
                }else if (getJahresStundenPlan(user.getYear()).getDayListMap().get(course.getDay()).size() > course.getLesson() - 1
                        && getJahresStundenPlan(user.getYear()).getDayListMap().get(course.getDay()).get(course.getLesson() - 1)
                        .getCourses().stream().anyMatch(comp -> comp.getCourseName().equalsIgnoreCase(course.getCourseName())
                        && comp.getTeacher().equalsIgnoreCase(course.getTeacher())
                        && !comp.getRoom().equalsIgnoreCase(course.getRoom()))){
                    room = "</font><font color=\"#FF0000\" face=\"Arial\" size=\"1\">" + room;
                }
                //Die Platzhalter sind "MO1R" für Montag 1 Stunde Raum aufgebaut. da wird das einfach ersetzt.
                //Dieses AtomicReference Zeug ist vermutlich ziemlich unnötig und ich sollte das anders lösen(kein Lambda z.B.)
                rawHtmlReference.set(rawHtmlReference.get()
                        .replace(course.getDay().name().substring(0, 2) + (course.getLesson() + i) + "R", room)
                        .replace(course.getDay().name().substring(0, 2) + (course.getLesson() + i) + "C", name)
                        .replace(course.getDay().name().substring(0, 2) + (course.getLesson() + i) + "L", teacher));
            }
        }));
        return rawHtmlReference.get();
    }

    public static HashMap<String, String> getMappings(int year){
        return year == 12 ? toReplace12 : toReplace11;
    }

    //Das lesen der Mappings
    private static void readMappings(){
        new BufferedReader(new InputStreamReader(GHGParser.class.getResourceAsStream("/mappings11.txt")))
                .lines().forEach(s -> {
                    String[] split = s.split(">");
                    toReplace11.put(split[0], split[1]);
        });

        new BufferedReader(new InputStreamReader(GHGParser.class.getResourceAsStream("/mappings12.txt")))
                .lines().forEach(s -> {
            String[] split = s.split(">");
            toReplace12.put(split[0], split[1]);
        });

    }

    //Ist nur eine Test-Mainmethode. Kannst du ignorieren.
    public static void main(String[] args) throws IOException, DSBNotLoadableException {
        try {
            Class.forName("de.berstanio.ghgparser.Logger");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        init(GHGParser.class.getResourceAsStream("/rawPage.htm"), new File("user/"));
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
                jahresStundenPlan = new JahresStundenPlan(12);
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
            users.add(new User(choosen, 12));
        }
        /*users.forEach(user -> {
            HashMap<DayOfWeek, LinkedList<Course>> masked = user.maskPlan(new Plan(week).getDayListMap());
            masked.forEach((dayOfWeek, courses) -> courses.forEach(System.out::println));
        });*/
        users.forEach(User::saveUser);
        try {
            setJahresStundenPlan(new JahresStundenPlan(12));
        } catch (DSBNotLoadableException e) {
            e.printStackTrace();
        }
        try {
            // TODO: 27.11.2020 Schwimmen falsch erkannt
            //setYear(11);
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

    public static JahresStundenPlan getJahresStundenPlan(int year) {
        return year == 12 ? jahresStundenPlan12 : jahresStundenPlan11;
    }

    public static void setJahresStundenPlan(JahresStundenPlan jahresStundenPlan) {
        if (jahresStundenPlan.getYear() == 11){
            jahresStundenPlan11 = jahresStundenPlan;
        }else {
            jahresStundenPlan12 = jahresStundenPlan;
        }
    }

    public static File getBasedir() {
        return basedir;
    }

    public static void setBasedir(File basedir) {
        GHGParser.basedir = basedir;
    }
}
