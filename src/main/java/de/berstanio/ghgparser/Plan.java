package de.berstanio.ghgparser;

import org.json.JSONArray;
import org.json.JSONObject;
/*import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;*/
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.ConnectException;
import java.net.URL;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Plan implements Serializable {

    private static final long serialVersionUID = 8022109081476857553L;
    private int week = 0;
    private transient String token;
    private HashMap<DayOfWeek, LinkedList<Block>> dayListMap = null;
    private Date lastUpdate = null;

    public Plan(int week) throws DSBNotLoadableException {
        setWeek(week);
        try {
            refresh();
        } catch (IOException e) {
            throw new DSBNotLoadableException(e);
        }
    }

    public void refresh() throws IOException {
        String jsonData = getJSONString();
        if (jsonData.isEmpty()) return;
        setToken(loadToken(jsonData));
        Date update = getUpdateDate(jsonData);
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
        if (getLastUpdate() != null && update.after(getLastUpdate())){
            setLastUpdate(update);
            String s = download();
            if (s.isEmpty()) return;
            setDayListMap(parse(s));
            setLastUpdate(update);
            savePlan();
        }
        if (getLastUpdate() == null){
            String s = download();
            if (s.isEmpty()) return;
            setDayListMap(parse(s));
            setLastUpdate(update);
            savePlan();
        }
        setLastUpdate(update);
        savePlan();
    }

    public HashMap<DayOfWeek, LinkedList<Block>> parse(String s){
        AtomicReference<String> replacedString = new AtomicReference<>(s);
        GHGParser.getMappings().forEach((toReplace, replacement) -> {
            replacedString.set(replacedString.get().replaceAll(toReplace, replacement));
        });
        s = replacedString.get();

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
                course.setRoom("EXT");
            }
        });
        getDayListMap().get(DayOfWeek.MONDAY).get(6).getCourses().removeIf(course1 -> course1.getCourseName().equalsIgnoreCase("PADD"));
        getDayListMap().get(DayOfWeek.MONDAY).get(6).getCourses().addAll(dayListMap.get(DayOfWeek.MONDAY).get(5).getCourses().stream().filter(course -> course.getCourseName().equalsIgnoreCase("PADD")).collect(Collectors.toList()));
        //Alle leeren Stunden entfernen und doppel STunden entfernen(unterschiedliche Lehrer/gleicher Raum)
        getDayListMap().forEach((dayOfWeek, blocks) -> {
            blocks.removeIf(block -> {
                ArrayList<Course> alreadyRemoved = new ArrayList<>();
                block.getCourses().removeIf(course -> {
                    if (course.getRoom().contains(".")){
                        course.setRoom(course.getRoom().replace(".",""));
                    }
                    if (!alreadyRemoved.contains(course)) {
                        for (Course courseDup : block.getCourses()) {
                            if (courseDup.getRoom().contains(".")){
                                courseDup.setRoom(courseDup.getRoom().replace(".",""));
                            }
                            if (!courseDup.equals(course)) {
                                if (courseDup.getCourseName().equalsIgnoreCase(course.getCourseName())) {
                                    if (courseDup.getRoom().contains(course.getRoom())) {
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
    }

    public void savePlan(){
        File dir = GHGParser.getBasedir();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(dir.getAbsolutePath() + "/plan" + week + ".yml"));
            objectOutputStream.writeObject(this);
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean loadPlan(){
        File dir = GHGParser.getBasedir();
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(dir.getAbsolutePath() + "/plan" + week + ".yml"));
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

    public String download() throws IOException{
        String room = GHGParser.getYear() == 12 ? "c00023" : "c00022";
        URL connectwat = new URL("https://light.dsbcontrol.de/DSBlightWebsite/Data/a7f2b46b-4d23-446e-8382-404d55c31f90/" + getToken() + "/" + getWeek() + "/c/" + room + ".htm");
        HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

        urlConnection.connect();

        BufferedInputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
        char c;
        StringBuilder stringBuilder = new StringBuilder();
        while (((int) (c = (char) bufferedInputStream.read())) != 65535) {
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    public String getJSONString() throws IOException{
        URL connectwat = new URL("https://mobileapi.dsbcontrol.de/dsbtimetables?authid=a7f2b46b-4d23-446e-8382-404d55c31f90");
        HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

        urlConnection.connect();

        BufferedInputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
        char c;
        StringBuilder stringBuilder = new StringBuilder();
        while (((int) (c = (char) bufferedInputStream.read())) != 65535) {
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    public Date getUpdateDate(String s)  {
        JSONArray array = new JSONArray(s);
        JSONObject object = (JSONObject) array.get(0);
        String date = (String) object.get("Date");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy kk:mm");
        try {
            return simpleDateFormat.parse(date);
        } catch (ParseException e) {
            return new Date();
        }
    }

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

    private class Uebertrag {
        private LinkedList<Course> courses = new LinkedList<>();
        private int counter;
        private int colspan;



    }
}