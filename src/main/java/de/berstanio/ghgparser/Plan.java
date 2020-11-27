package de.berstanio.ghgparser;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class Plan {

    private String hash = null;
    private int week = 0;
    private HashMap<DayOfWeek, LinkedList<Block>> dayListMap;

    public Plan(int week){
        setWeek(week);
        refresh();
    }

    public void refresh(){
        String s = download();
        String newHash = DigestUtils.md5Hex(s);
        if (!newHash.equals(getHash())){
            setHash(newHash);
            setDayListMap(parse(s));
        }
    }

    public HashMap<DayOfWeek, LinkedList<Block>> parse(String s){
        s = s.replace("LFR11", "LKFR12").replace("EXT.", "EXT");
        HashMap<DayOfWeek, LinkedList<Block>> dayListMap = new HashMap<>();
        dayListMap.put(DayOfWeek.MONDAY, new LinkedList<>());
        dayListMap.put(DayOfWeek.TUESDAY, new LinkedList<>());
        dayListMap.put(DayOfWeek.WEDNESDAY, new LinkedList<>());
        dayListMap.put(DayOfWeek.THURSDAY, new LinkedList<>());
        dayListMap.put(DayOfWeek.FRIDAY, new LinkedList<>());

        Document document = Jsoup.parse(s);

        Elements tables = document.getElementsByAttributeValue("rules", "all");
        Element table = tables.get(0);
        Elements columnsLessons =  table.child(0).children();

        HashMap<Integer, Uebertrag> uebertragMap = new HashMap<>();

        for (int i = 1, z = 0; i < columnsLessons.size() - 1; i += 2, z++) {
            Element columnLessons = columnsLessons.get(i);
            Elements days = columnLessons.children();
            days.remove(0);

            int colPos = 1;

            Iterator<Element> iterator = days.iterator();
            System.out.println("Zeile " + z);
            while (colPos < 60) {
                if (uebertragMap.get(colPos) != null) {
                    Uebertrag uebertrag = uebertragMap.get(colPos);

                    DayOfWeek day = DayOfWeek.of(Math.floorDiv(colPos, 12) + 1);
                    Block block;
                    if (dayListMap.get(day).size() != z){
                        block = dayListMap.get(day).get(z);
                    }else {
                        block = new Block();
                        dayListMap.get(day).add(block);
                    }
                    block.getCourses().addAll(uebertrag.courses);
                    block.setDay(day);
                    block.setBlockNr(z + 1);
                    uebertrag.counter--;
                    if (uebertrag.counter < 2) {
                        uebertragMap.remove(colPos);
                    }
                    colPos += uebertrag.colspan;
                    System.out.println("(Übertrag) Aktuelle colPos: " + colPos);
                } else {
                    if (iterator.hasNext()) {
                        Element element = iterator.next();
                        DayOfWeek day = DayOfWeek.of(Math.floorDiv(colPos, 12) + 1);

                        if (element.getElementsByTag("tr").size() < 1) {
                            dayListMap.get(day).add(Block.EMPTY);
                        } else {
                            Elements courses = element.getElementsByTag("tr");
                            Block block = new Block();

                            block.setDay(day);
                            block.setBlockNr(z + 1);

                            int row = Integer.parseInt(element.attr("rowspan"));
                            int count = row / 2;
                            for (Element courseHTML : courses) {
                                Elements part = courseHTML.getElementsByTag("td");
                                Course course = new Course();
                                course.setDay(day);
                                course.setLesson(z + 1);
                                course.setLength(count);
                                if (part.size() == 0) {
                                    block.getCourses().add(Block.EMPTY.getCourses().get(0));
                                    continue;
                                } else if (part.size() == 1) {
                                    if (part.get(0).text().chars().allMatch(Character::isDigit)) continue;
                                    course.setCourseName(part.get(0).text());
                                    //System.out.println(course.getCourseName());
                                    course.setTeacher("");
                                    course.setRoom("");
                                    block.getCourses().add(course);
                                    continue;
                                } else if (part.size() == 2) {
                                    course.setCourseName(part.get(0).text());
                                    course.setTeacher(part.get(1).text());
                                    course.setRoom("");
                                    block.getCourses().add(course);
                                    continue;
                                }
                                course.setRoom(part.get(0).text());
                                course.setCourseName(part.get(1).text());
                                course.setTeacher(part.get(2).text());
                                if (courseHTML.getElementsByTag("strike").size() != 0) {
                                    course.setCancelled(true);
                                }
                                block.getCourses().add(course);
                            }

                            if (dayListMap.get(day).size() != z) {
                                dayListMap.get(day).get(z).getCourses().addAll(block.getCourses());
                            } else {
                                dayListMap.get(day).add(block);
                            }
                            if (count > 1) {
                                System.out.println("Übertrag dazu: " + count);
                                Uebertrag uebertrag = new Uebertrag();
                                uebertrag.colspan = Integer.parseInt(element.attr("colspan"));
                                uebertrag.counter = count;
                                uebertrag.courses = (LinkedList<Course>) block.getCourses().clone();
                                uebertragMap.put(colPos, uebertrag);
                            }

                            colPos += Integer.parseInt(element.attr("colspan"));
                            System.out.println("(Elem) Aktuelle colPos: " + colPos);
                        }


                    } else {
                        System.out.println("Fehler!");
                        break;
                    }
                }
            }
        }
        return dayListMap;
    }

    public void normalize(){
        //Alle leeren Stunden entfernen und doppel STunden entfernen(unterschiedliche Lehrer/gleicher Raum)
        getDayListMap().forEach((dayOfWeek, blocks) -> {
            blocks.removeIf(block -> {
                ArrayList<Course> alreadyRemoved = new ArrayList<>();
                block.getCourses().removeIf(course -> {
                    if (!alreadyRemoved.contains(course)) {
                        for (Course courseDup : block.getCourses()) {
                            if (!courseDup.equals(course)) {
                                if (courseDup.getCourseName().equalsIgnoreCase(course.getCourseName())) {
                                    if (courseDup.getRoom().equals(course.getRoom())) {
                                        alreadyRemoved.add(course);
                                        alreadyRemoved.add(courseDup);
                                        if (!courseDup.getTeacher().contains("/")){
                                            courseDup.setTeacher(courseDup.getTeacher() + "/" + course.getTeacher());
                                        }
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                    return course.getCourseName().isEmpty();
                });
                return block.getCourses().isEmpty();
            });
        });
// TODO: 27.11.2020 Hier richtig einfügen.
        //Normal: PADD = courseName; teacher = SHKE
        //JahresStunden: CourseName = SHKE; teacher = PADD
        //NAch Swap: CourseName = SHKE; Room = PADD
        getDayListMap().get(DayOfWeek.MONDAY).get(5).getCourses().forEach(course -> {

            if ((course.getTeacher().isEmpty() && course.getRoom().equalsIgnoreCase("PADD"))){
                course.setTeacher(course.getCourseName());
                course.setCourseName(course.getRoom());
                course.setRoom("EXT");
            }

            if ((course.getRoom().isEmpty() && course.getCourseName().equalsIgnoreCase("PADD"))){
                course.setLength(2);
            }
        });
        getDayListMap().get(DayOfWeek.MONDAY).get(6).getCourses().removeIf(course1 -> course1.getCourseName().equalsIgnoreCase("PADD"));
        getDayListMap().get(DayOfWeek.MONDAY).get(6).getCourses().addAll(dayListMap.get(DayOfWeek.MONDAY).get(5).getCourses().stream().filter(course -> course.getCourseName().equalsIgnoreCase("PADD")).collect(Collectors.toList()));
    }

    public String download(){
        try {
            URL connectwat = new URL("https://light.dsbcontrol.de/DSBlightWebsite/Data/a7f2b46b-4d23-446e-8382-404d55c31f90/" + getToken() + "/" + getWeek() + "/c/c00023.htm");
            HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

            urlConnection.connect();

            BufferedInputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
            char c;
            StringBuilder stringBuilder = new StringBuilder();
            while (((int) (c = (char) bufferedInputStream.read())) != 65535) {
                stringBuilder.append(c);
            }
            return stringBuilder.toString();
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }


    }

    public String getToken(){
        try {
            URL connectwat = new URL("https://mobileapi.dsbcontrol.de/dsbtimetables?authid=a7f2b46b-4d23-446e-8382-404d55c31f90");
            HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

            urlConnection.connect();

            BufferedInputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
            char c;
            StringBuilder stringBuilder = new StringBuilder();
            while (((int) (c = (char) bufferedInputStream.read())) != 65535) {
                stringBuilder.append(c);
            }
            JSONParser jsonParser = new JSONParser();
            JSONArray array = (JSONArray) jsonParser.parse(stringBuilder.toString());
            JSONObject object = (JSONObject) array.get(0);
            return (String) object.get("Id");
        }catch (IOException | ParseException e){
            e.printStackTrace();
            return "";
        }
    }
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public HashMap<DayOfWeek, LinkedList<Block>> getDayListMap() {
        return dayListMap;
    }

    public void setDayListMap(HashMap<DayOfWeek, LinkedList<Block>> dayListMap) {
        this.dayListMap = dayListMap;
    }

    private class Uebertrag {
        private LinkedList<Course> courses = new LinkedList<>();
        private int counter;
        private int colspan;



    }
}

/*        if (duplicates.size() == 3){
                        if (course.getCourseName().substring(0, 2).equalsIgnoreCase("LK")){
                            System.out.println("Für CourseName: " + course.getCourseName() + " mit dem Lehrer: " + course.getTeacher() + " wurde richtig LK erkannt");
                        }else {
                            System.out.println("Für CourseName: " + course.getCourseName() + " mit dem Lehrer: " + course.getTeacher() + " wurde falsch LK erkannt");
                        }
                    }else if(duplicates.size() % 2 == 0){
                        if (course.getCourseName().substring(0, 2).equalsIgnoreCase("gk")
                                || course.getCourseName().substring(0, 2).equalsIgnoreCase("ds")
                                || course.getCourseName().charAt(course.getCourseName().length() - 1) == 'z'
                                || course.getCourseName().equalsIgnoreCase("SPTH")){
                            System.out.println(duplicates.size());
                            System.out.println("Für CourseName: " + course.getCourseName() + " mit dem Lehrer: " + course.getTeacher() + " wurde richtig gk erkannt");
                        }else {
                            System.out.println("Für CourseName: " + course.getCourseName() + " mit dem Lehrer: " + course.getTeacher() + " wurde falsch gk erkannt");

                        }
                    }else if (duplicates.size() == 1){
                        String name = course.getCourseName();
                        if (name.equalsIgnoreCase("PADD")
                                || name.equalsIgnoreCase("LEA")
                                || name.equalsIgnoreCase("FITN")
                                || name.equalsIgnoreCase("BADM")
                                || name.equalsIgnoreCase("VOL")
                                || name.equalsIgnoreCase("HOCK")
                                || name.equalsIgnoreCase("SCHW1")
                                || name.equalsIgnoreCase("TENN")
                                || name.equalsIgnoreCase("VOL-J")
                                || name.equalsIgnoreCase("FUS")
                                || name.equalsIgnoreCase("GYM")
                                || name.equalsIgnoreCase("BASK")
                                || name.equalsIgnoreCase("OS-Orc")
                                || name.equalsIgnoreCase("OS-")){
                            System.out.println("Für CourseName: " + course.getCourseName() + " mit dem Lehrer: " + course.getTeacher() + " wurde richtig Sportkurs/o.ä. erkannt");
                        }else {
                            System.out.println("Für CourseName: " + course.getCourseName() + " mit dem Lehrer: " + course.getTeacher() + " wurde falsch Sportkurs/o.ä. erkannt");
                        }
                    }else if (duplicates.size() == 5){
                        if (course.getCourseName().equalsIgnoreCase("DELF")){
                            System.out.println("Für CourseName: " + course.getCourseName() + " mit dem Lehrer: " + course.getTeacher() + " wurde richtig DELF erkannt");
                        }else {
                            System.out.println("Für CourseName: " + course.getCourseName() + " mit dem Lehrer: " + course.getTeacher() + " wurde falsch DELF erkannt");
                        }
                    }else {
                        System.out.println("Für CourseName: " + course.getCourseName() + " mit dem Lehrer: " + course.getTeacher() + " gabs " + duplicates.size() + " identische");
                    }*/
