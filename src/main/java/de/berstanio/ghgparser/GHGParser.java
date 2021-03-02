package de.berstanio.ghgparser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Die Hauptklasse, welche für die Initialisierung zuständig ist. Die Hauptschnittstelle zu dem Programm
 */
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
    private static final HashMap<String, String> toReplace11 = new HashMap<>();
    //Eine Mapping Tabelle, zum anpassen des Plan-HTMLs, welches runtergeladen wird, auf einen Standard(für die 12.)
    private static final HashMap<String, String> toReplace12 = new HashMap<>();

    /**
     * Die init Methode, welche alles nötige initalisiert(Die Profile, Mappings etc.)
     * @param rawHtmlStream Ein InputStream, durch welches das HTML mit Platzhaltern geladen werden kann
     * @param basedir Der Ordner, in dem das Programm seine Daten speichern kann/soll
     * @throws DSBNotLoadableException Wenn der Jahresstundenplan vom DSB nicht geladen werden kann
     */
    public static void init(InputStream rawHtmlStream, File basedir) throws DSBNotLoadableException {
        setBasedir(basedir);

        readMappings();
        ArrayList<User> users = User.loadUsers();
        setUsers(users);
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(rawHtmlStream))){
            setRawHtml(bufferedReader.lines().collect(Collectors.joining()));
        } catch (IOException e) {
            e.printStackTrace();
        }


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

    /**
     * Die Funktion generiert von einem Profil und einer Woche, einen zugehörigen personalisierten HTML-Plan
     * @param user User, für den das HTML erzeugt werden soll
     * @param week Die Kalenderwoche als int, für die das HTML erzeugt werden soll
     * @return Das generierte HTML als String
     * @throws DSBNotLoadableException Wenn der Plan für die Woche für den Jahrgang des Users nicht geladen werden kann
     */
    public static String generateHtmlFile(User user, int week) throws DSBNotLoadableException {
        return generateHtmlFile(user, new Plan(user.getYear(), week));
    }

    /**
     * Die Funktion generiert von einem Profil und einer Woche, einen zugehörigen personalisierten HTML-Plan
     * @param user User, für den das HTML erzeugt werden soll
     * @param plan Der Plan, der personalisiert werden soll
     * @return Das generierte HTML als String
     */
    //Die Funktion generiert die fertige HTML-Datei.
    public static String generateHtmlFile(User user, Plan plan) {
        String html = getRawHtml();
        html = html.replace("JGPH", user.getYear() + "");
        //bringt den runtergeladenen Plan in eine Standard-Form
        plan.normalize();
        //Maskiert alle Kurse weg, die nicht vom Nutzer belegt wurden
        HashMap<DayOfWeek, LinkedList<Course>> masked = user.maskPlan(plan.getDayListMap());
        //Ein HTML-String, damit ich leichter etwas durchstreichen kann. "con" ist der Platzhalter von dem, was durchgetrichen werden soll
        String strikes = "</font><font color=\"#FF0000\" face=\"Arial\" size=\"1\"><strike>con</strike>";
      //Über alle belegten Kurse in ihrem momentanen Zustand rüberiterieren(Zwei Lambdas(man sieht es schlecht))
        for (LinkedList<Course> courses : masked.values()) {
            for (Course course : courses) {
                for (int i = 0; i < course.getLength(); i++) {
                    //Ein Kurs kann z.B. 2 Stunden lan gehen. Jede Stunde muss aber eingetragen werden. Deshalb wird der Kurs so lange untereinander
                    //eingetragen, wie er lang ist
                    String room = course.getRoom();
                    String name = course.getCourseName();
                    String teacher = course.getTeacher();
                    if (course.getCourseName().isEmpty() && course.getTeacher().isEmpty() && course.getRoom().isEmpty()){

                    }else if (course.isCancelled()) {
                        //Wenn der Kurs ausfällt, ihn durchgestrichen anzeigen
                        room = strikes.replace("con", room);
                        name = strikes.replace("con", name);
                        teacher = strikes.replace("con", teacher);
                      //Wenn es im aktuellen Plan eine Stunde gibt, die den gleichen Lehrer und den gleichen Kursnamen hat, aber unterschiedlichen Raum als im Jahresstundenplan
                      //Dann ist es eine Raumänderung
                    } else if (getJahresStundenPlan(user.getYear()).getDayListMap().get(course.getDay()).get(course.getLesson() - 1)
                            .getCourses().stream().anyMatch(comp -> comp.getCourseName().equalsIgnoreCase(course.getCourseName())
                                    && comp.getTeacher().equalsIgnoreCase(course.getTeacher())
                                    && !comp.getRoom().equalsIgnoreCase(course.getRoom()))) {
                        room = "</font><font color=\"#FF0000\" face=\"Arial\" size=\"1\">" + room;
                    }
                    //Die Platzhalter sind "MO1R" für Montag 1 Stunde Raum aufgebaut. da wird das einfach ersetzt.
                    html = html.replace(course.getDay().name().substring(0, 2) + (course.getLesson() + i) + "R", room)
                            .replace(course.getDay().name().substring(0, 2) + (course.getLesson() + i) + "C", name)
                            .replace(course.getDay().name().substring(0, 2) + (course.getLesson() + i) + "L", teacher);
                }
            }
        }
        return html;
    }

    /**
     * Gibt die Mapping-Map zurück für einen Jahrgang
     * @param year Das Jahr, für welche die Mapping-Map zurück gegeben werden soll
     * @return Die Mapping-Map, zum Anpassen des Stundenplan-HTMLs
     */
    public static HashMap<String, String> getMappings(int year){
        return year == 12 ? toReplace12 : toReplace11;
    }

    //Das lesen der Mappings
    private static void readMappings(){
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(GHGParser.class.getResourceAsStream("/mappings11.txt")))) {
            bufferedReader.lines().forEach(s -> {
                String[] split = s.split(">");
                toReplace11.put(split[0], split[1]);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(GHGParser.class.getResourceAsStream("/mappings12.txt")))) {
            bufferedReader.lines().forEach(s -> {
                String[] split = s.split(">");
                toReplace12.put(split[0], split[1]);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Ist nur eine Test-Mainmethode.
    public static void main(String[] args) throws DSBNotLoadableException {
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
            User user = users.get(0);

            System.out.println(generateHtmlFile(user, week));
            Files.write(Paths.get("out.htm"), generateHtmlFile(user, week).getBytes(StandardCharsets.ISO_8859_1));
        } catch (IOException | DSBNotLoadableException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gibt eine User-Liste zurück mit allen Usern, die im Programm erstellt wurden
     * @return Eine User-Liste zurück mit allen Usern
     */
    public static ArrayList<User> getUsers() {
        return users;
    }

    /**
     * Setzt eine User-Liste mit allen Usern, die im Programm erstellt wurden
     * @param users Eine User-Liste zurück mit allen Usern
     */
    public static void setUsers(ArrayList<User> users) {
        GHGParser.users = users;
    }

    /**
     * Gibt das Plan-HTML mit Platzhaltern zurück
     * @return Das Plan-HTML mit Platzhaltern als String
     */
    public static String getRawHtml() {
        return rawHtml;
    }

    /**
     * Setzt das Plan-HTML mit Platzhaltern
     * @param rawHtml Das Plan-HTML mit Platzhaltern als String
     */
    public static void setRawHtml(String rawHtml) {
        GHGParser.rawHtml = rawHtml;
    }

    /**
     * Gibt den Jahresstundenplan für einen Jahrgang zurück
     * @param year Der Jahrgang, zu dem der Jahresstundenplan zurück gegeben werden soll
     * @return Den Jahresstundenplan für den JAhrgang
     */
    public static JahresStundenPlan getJahresStundenPlan(int year) {
        return year == 12 ? jahresStundenPlan12 : jahresStundenPlan11;
    }

    /**
     * Setzt den Jahresstundenplan, für den passenden Jahrgang
     * @param jahresStundenPlan Der Jahresstundenplan der gesetzt werden soll
     */
    public static void setJahresStundenPlan(JahresStundenPlan jahresStundenPlan) {
        if (jahresStundenPlan.getYear() == 11){
            jahresStundenPlan11 = jahresStundenPlan;
        }else {
            jahresStundenPlan12 = jahresStundenPlan;
        }
    }

    /**
     * Gibt den Ordner zurück, in welchen das Programm speichern soll
     * @return Der Ordner als File, in welchen das Programm speichern soll
     */
    public static File getBasedir() {
        return basedir;
    }

    /**
     * Setzt den Ordner, in welchen das Programm speichern soll
     * @param basedir Der Ordner als File, in welchen das Programm speichern soll
     */
    public static void setBasedir(File basedir) {
        if (!basedir.exists()) basedir.mkdir();
        GHGParser.basedir = basedir;
    }
}
