package de.berstanio.ghgparser;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Representiert den "Standart-Stundenplan", wie er ohne Änderung wäre
 */
public class JahresStundenPlan extends Plan {

    //Eine Liste von möglich wählbaren Kursen
    private ArrayList<CoreCourse> coreCourses;
    private static final long serialVersionUID = -2671162280384342988L;

    public JahresStundenPlan(int year) throws DSBNotLoadableException {
        super(year,0);
    }

    /**
     * Lädt den Plan neu
     * @return Ob es ein Update gab, als boolean
     * @throws DSBNotLoadableException Wenn keine Verbindung zum DSB hergestellt werden kann
     */
    @Override
    public boolean refresh() throws DSBNotLoadableException {
        boolean b = super.refresh();
        if (b || getCoreCourses() == null){
            //Wenn die DayListMap neu geladen wurde, auch die CoreCourses neu laden
            setCoreCourses(loadCoreCourses());
        }
        return b;
    }

    /**
     * Extrahiert alle wählbaren Kurse aus der dayListMap
     * @return Liste der wählbaren Kurse
     */
    public ArrayList<CoreCourse> loadCoreCourses(){
        //Erstmal den Plan im Standardformat holen
        HashMap<DayOfWeek, LinkedList<Block>> dayListMap = getDayListMap();

        //Die Struktur des HTMLs mit den Stundenplan unterscheidet scih ein wenig. Bei dem einen kommt "Raum", "Fach","Lehrer"
        //bei dem anderen lehrer, Fach, Raum. Deshalb müssen Lehrer und Raum hier getauscht werden
        ArrayList<Course> alreadySwapped = new ArrayList<>();
        dayListMap.values().forEach(blocks -> {
            blocks.forEach(block -> {
                block.getCourses().removeIf(course -> {
                    if (!alreadySwapped.contains(course)){
                        String room = course.getRoom();
                        course.setRoom(course.getTeacher());
                        course.setTeacher(room);
                        alreadySwapped.add(course);
                    }
                    return course.getTeacher().isEmpty() && course.getRoom().isEmpty();
                });
            });
        });
        //Ins Standard-Format bringen
        normalize();

        //Kurse, die ich schon hinzugefügt habe(z.B. Kurse die über 2 Stunden gehen, sind 2x drin)
        ArrayList<Course> alreadyAdd = new ArrayList<>();

        //Die generierten Wählbaren Kurse
        ArrayList<CoreCourse> finished = new ArrayList<>();

        //Über alles rüberiterieren
        for (DayOfWeek day : dayListMap.keySet().stream().sorted().collect(Collectors.toList())) {
            LinkedList<Block> blocks = dayListMap.get(day);
            for (Block block : blocks) {
                for (Course course : block.getCourses()) {
                    //Die duplicates Liste besagt, welche Kurse zusammengehören
                    LinkedList<Course> duplicates = new LinkedList<>();
                    if (alreadyAdd.contains(course)) continue;
                    duplicates.add(course);
                    alreadyAdd.add(course);

                    //Geht nochmal über die gesammte Liste rüber und schaut, was für duplicate er findet(ziemlich Perfomancelastig glaube ich)
                    dayListMap.values().forEach(blocksDup -> {
                        blocksDup.forEach(blockDup -> {
                            blockDup.getCourses().forEach(courseDup -> {
                                if (!courseDup.equals(course)) {
                                    if (courseDup.getCourseName().equalsIgnoreCase(course.getCourseName())) {
                                        if (courseDup.getTeacher().equalsIgnoreCase(course.getTeacher())) {
                                            duplicates.add(courseDup);
                                            alreadyAdd.add(courseDup);
                                        }
                                    }
                                }
                            });
                        });
                    });
                    //Wir haben dämliche Einzelfälle, in denen Stunden nicht zu einem Kurs gehören... Die müssen dann getrennt werden.
                    if (course.getCourseName().contains("-vb") || course.getCourseName().contains("DELF")) {
                        duplicates.forEach(course1 -> {
                            CoreCourse coreCourse = new CoreCourse();
                            coreCourse.setCourseName(course.getCourseName());
                            coreCourse.setTeacher(course.getTeacher());
                            coreCourse.getCourses().add(course1);
                            finished.add(coreCourse);
                        });
                        continue;
                    }

                    if (!(duplicates.size() % 2 == 0 && duplicates.size() >= 4)) {
                        if (!course.getTeacher().isEmpty()) {
                            CoreCourse coreCourse = new CoreCourse();
                            coreCourse.setCourseName(course.getCourseName());
                            coreCourse.setTeacher(course.getTeacher());
                            coreCourse.getCourses().addAll(duplicates);
                            finished.add(coreCourse);
                        }
                    } else {
                        while (duplicates.size() != 0) {
                            CoreCourse coreCourse = new CoreCourse();
                            coreCourse.setCourseName(course.getCourseName());
                            coreCourse.setTeacher(course.getTeacher());
                            Course firstOrSecond;
                            Course base = duplicates.get(0);
                            Block baseBlock = dayListMap.get(base.getDay()).get(base.getLesson() - 1);
                            //Um die verschiedenen Kurse zu unterschieden muss man schauen, was für Kurse sonst noch gleichzeitig laufen
                            //Also der eine gkMa hat immer mit dem gleichen gkPhy Unterricht
                            //Deshalb schaue ich mir den 1. oder 2. gleichzeitig laufenden Kurs an und vergleiche das dann.
                            //(den 2. nehme ich, falls) der Kurs als erstes ist, zu dem ich gerade den Partner suche.
                            //Also der eine gkMa hat immer mit dem gleichen gkPhy Unterricht
                            //Deshalb schaue ich mir den 1. oder 2. gleichzeitig laufenden Kurs an und vergleiche das dann.
                            //(den 2. nehme ich, falls) der Kurs als erstes ist, zu dem ich gerade den Partner suche.
                            int i = 0;
                            if (baseBlock.getCourses().indexOf(base) == 0){
                                i = 1;
                            }
                            firstOrSecond = baseBlock.getCourses().get(i);
                            for (Course check : duplicates) {
                                if (check.equals(base)) continue;
                                Course tmp = dayListMap.get(check.getDay()).get(check.getLesson() - 1).getCourses().get(i);
                                if (tmp.getCourseName().equals(firstOrSecond.getCourseName())) {
                                    if (tmp.getTeacher().equals(firstOrSecond.getTeacher())) {
                                        coreCourse.getCourses().add(base);
                                        coreCourse.getCourses().add(check);
                                        finished.add(coreCourse);
                                        break;
                                    }
                                }

                            }
                            duplicates.removeAll(coreCourse.getCourses());
                        }
                    }
                }
            }
        }
        return finished;
    }

    /**
     * Extrahiert aus einem JSON-String das Datum des letzten Plan-Updates
     * @param s Der JSON-String
     * @return Das Datum des letzten Plan-Updates als java.util.Date
     */
    @Override
    public Date getUpdateDate(String s) throws ParseException {
        JSONArray array = new JSONArray(s);
        JSONObject object = (JSONObject) array.get(1);
        String date = (String) object.get("Date");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy kk:mm");
        return simpleDateFormat.parse(date);
    }

    /**
     * Extrahiert aus einem JSON-String den Token des Plans
     * @param s Der JSON-String
     * @return Der Token als String
     */
    @Override
    public String loadToken(String s) throws DSBNotLoadableException {
        JSONArray array = new JSONArray(s);
        // We need this for loop because android replaces the json api with a already provided one where JSONArray doesn't implement Iterable
        // https://stackoverflow.com/questions/57274183/android-issue-using-json-library-in-pure-java-package
        for (int i = 0; i < array.length(); i++) {
            Object object = array.get(i);
            if (object instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) object;
                if (jsonObject.get("Title").toString().contains("Jahresstundenplan")) {
                    return (String) jsonObject.get("Id");
                }
            }
        }
        throw new DSBNotLoadableException("Can't load token for JSP from string: " + s);
    }

    /**
     * Gibt die Kalenderwoche zurück, in der sich der allgemeine Plan befindet
     * @return Die Kalenderwoche als int, in der sich der allgemeine Plan befindet
     */
    @Override
    public int getWeek(){
        try {
            URL connectwat = new URL("https://dsbmobile.de/data/a7f2b46b-4d23-446e-8382-404d55c31f90/" + getToken() + "/data.js");
            HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

            urlConnection.connect();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String s =  bufferedReader.lines().collect(Collectors.joining());

            int i = s.indexOf("weeks: ") + 13;
            s = s.substring(i, i + 2);
            return Integer.parseInt(s);
        }catch (IOException e){
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Gibt die Liste aller wählbaren Kurse zurück
     * @return Die Liste aller wählbaren Kurse
     */
    public ArrayList<CoreCourse> getCoreCourses() {
        return coreCourses;
    }

    /**
     * Setzt die Liste aller wählbaren Kurse
     * @param coreCourses Die Liste aller wählbaren Kurse
     */
    public void setCoreCourses(ArrayList<CoreCourse> coreCourses) {
        this.coreCourses = coreCourses;
    }
}
