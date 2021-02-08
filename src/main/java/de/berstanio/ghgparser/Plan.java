package de.berstanio.ghgparser;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
//Die Klasse soll den Stundenplan representieren, wie er mit Änderung ist.
public class Plan implements Serializable {

    private static final long serialVersionUID = 1288665561481381353L;
    //Die Woche, in Jahreswochen, für die der Plan gilt
    private int week = 0;
    //Der Jahrgang, für den der Plan gilt
    private int year;
    //Der Token, welcher zum laden von Daten genutzt wird
    private transient String token;
    //Die Map, welche jedem Tag seine Stunden zuordnet(index 0 der Liste = 1. Stunde)
    private HashMap<DayOfWeek, LinkedList<Block>> dayListMap = null;
    //Wann die Map zuletzt geupdatet wurde
    private Date lastUpdate = null;

    public Plan(int year, int week) throws DSBNotLoadableException {
        setYear(year);
        setWeek(week);
        try {
            refresh();
        } catch (IOException e) {
            throw new DSBNotLoadableException(e);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    //Die Refresh-Methode, durch die die Plan Daten neu geladen werden
    public void refresh() throws IOException, ParseException {
        String jsonData = getJSONString();
        if (jsonData.isEmpty()) return;
        setToken(loadToken(jsonData));
        Date update = getUpdateDate(jsonData);
        //Falls der Plan das erste mal geladen wird und es keine Kopie auf der Festplatte gibt, lade ihn runter
        if (getDayListMap() == null) {
            if (!loadPlan()) {
                String s = download();
                if (s.isEmpty()) return;
                setDayListMap(parse(s));
                setLastUpdate(update);
                savePlan();
                return;
            }
        }
        //Falls das Datum des Plans älter als das aktuelle Update-Datum ist, mache ein update
        if (getLastUpdate() != null && update.after(getLastUpdate())){
            String s = download();
            if (s.isEmpty()) return;
            setDayListMap(parse(s));
            setLastUpdate(update);
            savePlan();
            return;
        }
        //Falls aus irgendeinem Grund die Datums-Daten weg sind, mache ein update
        if (getLastUpdate() == null){
            String s = download();
            if (s.isEmpty()) return;
            setDayListMap(parse(s));
            setLastUpdate(update);
            savePlan();
            return;
        }
        //Speicher Sicherhaltshalber alles, auch wenn es nicht nötig sein sollte
        setLastUpdate(update);
        savePlan();
    }

    //Das Parsen des geladenen HTMLs in eine Datenstruktur
    //Der Algorithmus und die Struktur des HTMLs ist etwas schwerer zu erklären, weshalb ich noch schaue, wie ich das am besten mache
    public HashMap<DayOfWeek, LinkedList<Block>> parse(String s){
        for (Map.Entry<String, String> entry : GHGParser.getMappings(getYear()).entrySet()) {
            s = s.replaceAll(entry.getKey(), entry.getValue());
        }

        HashMap<DayOfWeek, LinkedList<Block>> dayListMap = new HashMap<>();
        dayListMap.put(DayOfWeek.MONDAY, new LinkedList<>());
        dayListMap.put(DayOfWeek.TUESDAY, new LinkedList<>());
        dayListMap.put(DayOfWeek.WEDNESDAY, new LinkedList<>());
        dayListMap.put(DayOfWeek.THURSDAY, new LinkedList<>());
        dayListMap.put(DayOfWeek.FRIDAY, new LinkedList<>());

        Document document = Jsoup.parse(s);

        Elements columnsLessons =  document.getElementsByAttributeValue("rules", "all").get(0).child(0).children();

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

    //Den Plan weiter auf ein Standard-Format bringen, was mit den Mappings nicht möglich war
    public void normalize(){
        //Dämmlicher EInzellfall, bei einer Stunde fehlt der Raum. Das ist der Fix
        getDayListMap().get(DayOfWeek.MONDAY).get(6).getCourses().removeIf(course -> course.getCourseName().equalsIgnoreCase("PADD"));
        getDayListMap().get(DayOfWeek.MONDAY).get(5).getCourses().forEach(course -> {
            if ((course.getTeacher().isEmpty() && course.getRoom().equalsIgnoreCase("PADD"))){
                course.setTeacher(course.getCourseName());
                course.setCourseName(course.getRoom());
                course.setLength(2);
                course.setRoom("EXT");
            }

            if ((course.getRoom().isEmpty() && course.getCourseName().equalsIgnoreCase("PADD"))){
                course.setLength(2);
                course.setRoom("EXT");
            }
            if (course.getCourseName().equalsIgnoreCase("PADD")){
                getDayListMap().get(DayOfWeek.MONDAY).get(6).getCourses().add(course);

            }
        });

        //Alle leeren Stunden entfernen und doppel STunden entfernen(unterschiedliche Lehrer/gleicher Raum)
        getDayListMap().forEach((dayOfWeek, blocks) -> blocks.removeIf(block -> {
            ArrayList<Course> alreadyRemoved = new ArrayList<>();
            block.getCourses().removeIf(course -> {
                        if (!alreadyRemoved.contains(course)) {
                            for (Course courseDup : block.getCourses()) {
                                //Fixt, dass bei manchen ein Raum mit Punkt am Ende eingetragen ist
                                if (courseDup.getRoom().contains(".")) {
                                    courseDup.setRoom(courseDup.getRoom().replace(".", ""));
                                }
                                //Manche Kurse werden eingetragen als zwei verschiedene Kurse, obwohl es nur 2 Lehrer gibt
                                //Hier wurd das zu einem Kurs zusammengefasst und die Lehrer mit "/" gejoint.
                                if (!courseDup.equals(course)) {
                                    if (courseDup.getCourseName().equalsIgnoreCase(course.getCourseName())) {
                                        if (courseDup.getRoom().contains(course.getRoom())) {
                                            alreadyRemoved.add(course);
                                            alreadyRemoved.add(courseDup);
                                            if (!courseDup.getTeacher().contains("/")) {
                                                courseDup.setTeacher(courseDup.getTeacher() + "/" + course.getTeacher());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        //Wenn Kurse leer sind, entfernen
                        return course.getCourseName().isEmpty();
                    });
                //Wenn Kurse leer sind, entfernen
                return block.getCourses().isEmpty();
            }));
    }

    //Serializierd und speichert den Plan
    public void savePlan(){
        File dir = GHGParser.getBasedir();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(dir.getAbsolutePath() + "/plan" + week + "" + getYear() + ".yml"));
            objectOutputStream.writeObject(this);
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //liest den Plan von der Festplatte und gibt zurück, ob er ihn erfolgreich laden konnte
    public boolean loadPlan(){
        File dir = GHGParser.getBasedir();
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(dir.getAbsolutePath() + "/plan" + week + "" + getYear() + ".yml"));
            Plan plan = (Plan) objectInputStream.readObject();
            objectInputStream.close();
            this.setDayListMap(plan.getDayListMap());
            this.setLastUpdate(plan.getLastUpdate());
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    //Runterladen des HTMLs vom Server
    public String download() throws IOException{
        String room = getYear() == 12 ? "c00023" : "c00022";
        String week = getWeek() + "";
        if (week.length() == 1){
            week = "0" + week;
        }
        if (getWeek() >= 54){
            week = "01";
        }
        URL connectwat = new URL("https://light.dsbcontrol.de/DSBlightWebsite/Data/a7f2b46b-4d23-446e-8382-404d55c31f90/" + getToken() + "/" + week + "/c/" + room + ".htm");
        HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

        urlConnection.connect();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.ISO_8859_1));
        return bufferedReader.lines().collect(Collectors.joining());
    }

    //Den JSON-String holen, mit allen möglichen Infos z.B. token, lastupdate
    public String getJSONString() throws IOException{
        URL connectwat = new URL("https://mobileapi.dsbcontrol.de/dsbtimetables?authid=a7f2b46b-4d23-446e-8382-404d55c31f90");
        HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

        urlConnection.connect();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        return bufferedReader.lines().collect(Collectors.joining());
    }

    //Aus dem JSON String lesen, wann das letzte update war
    public Date getUpdateDate(String s) throws ParseException {
        JSONArray array = new JSONArray(s);
        JSONObject object = (JSONObject) array.get(0);
        String date = (String) object.get("Date");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy kk:mm");
        return simpleDateFormat.parse(date);
    }

    //Aus dem JSON String den Token holen
    public String loadToken(String s){
        JSONArray array = new JSONArray(s);
        JSONObject object = (JSONObject) array.get(0);
        return (String) object.get("Id");
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    private static class Uebertrag {
        private LinkedList<Course> courses = new LinkedList<>();
        private int counter;
        private int colspan;
    }
}