package de.berstanio.ghgparser;

import org.json.JSONArray;
import org.json.JSONObject;
/*import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;*/

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class JahresStundenPlan extends Plan {

    private ArrayList<CoreCourse> coreCourses;

    public JahresStundenPlan() {
        super(0);
        setCoreCourses(loadCoreCourses());
    }

    public ArrayList<CoreCourse> loadCoreCourses(){
        HashMap<DayOfWeek, LinkedList<Block>> dayListMap = getDayListMap();

        ArrayList<Course> alreadySwapped = new ArrayList<>();
        dayListMap.forEach((dayOfWeek, blocks) -> {
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
        normalize();

        ArrayList<Course> alreadyAdd = new ArrayList<>();

        ArrayList<CoreCourse> finished = new ArrayList<>();


        for (DayOfWeek day : dayListMap.keySet().stream().sorted().collect(Collectors.toList())) {
            LinkedList<Block> blocks = dayListMap.get(day);
            for (Block block : blocks) {
                for (Course course : block.getCourses()) {
                    LinkedList<Course> duplicates = new LinkedList<>();
                    if (alreadyAdd.contains(course)) continue;
                    duplicates.add(course);
                    alreadyAdd.add(course);

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

                    if (course.getCourseName().contains("-vb") || course.getCourseName().contains("DELF")) {
                        duplicates.stream().forEachOrdered(course1 -> {
                            CoreCourse coreCourse = new CoreCourse();
                            coreCourse.setCourseName(course.getCourseName());
                            coreCourse.setTeacher(course.getTeacher());
                            coreCourse.getCourses().add(course1);
                            finished.add(coreCourse);
                        });
                        continue;
                    }

                    if (duplicates.size() == 3) {
                        CoreCourse coreCourse = new CoreCourse();
                        coreCourse.setCourseName(course.getCourseName());
                        coreCourse.setTeacher(course.getTeacher());
                        coreCourse.getCourses().addAll(duplicates);
                        finished.add(coreCourse);
                    } else if (duplicates.size() % 2 == 0) {
                        if (duplicates.size() == 2) {
                            CoreCourse coreCourse = new CoreCourse();
                            coreCourse.setCourseName(course.getCourseName());
                            coreCourse.setTeacher(course.getTeacher());
                            coreCourse.getCourses().addAll(duplicates);
                            finished.add(coreCourse);
                        } else {
                            while (duplicates.size() != 0) {
                                CoreCourse coreCourse = new CoreCourse();
                                coreCourse.setCourseName(course.getCourseName());
                                coreCourse.setTeacher(course.getTeacher());
                                Course firstOrSecond;
                                int i = 0;
                                if (dayListMap.get(duplicates.get(0).getDay()).get(duplicates.get(0).getLesson() - 1).getCourses().indexOf(duplicates.get(0)) == 0){
                                    i = 1;
                                }
                                firstOrSecond = dayListMap.get(duplicates.get(0).getDay()).get(duplicates.get(0).getLesson() - 1).getCourses().get(i);
                                for (Course check : duplicates) {
                                    if (check.equals(duplicates.get(0))) continue;
                                    Course tmp = dayListMap.get(check.getDay()).get(check.getLesson() - 1).getCourses().get(i);

                                    if (tmp.getCourseName().equals(firstOrSecond.getCourseName())) {
                                        if (tmp.getTeacher().equals(firstOrSecond.getTeacher())) {
                                            coreCourse.getCourses().add(duplicates.get(0));
                                            coreCourse.getCourses().add(check);
                                            finished.add(coreCourse);
                                            break;
                                        }
                                    }

                                }
                                duplicates.removeAll(coreCourse.getCourses());
                            }
                        }
                    } else if (duplicates.size() == 1) {
                        if (!course.getTeacher().isEmpty()) {
                            CoreCourse coreCourse = new CoreCourse();
                            coreCourse.setCourseName(course.getCourseName());
                            coreCourse.setTeacher(course.getTeacher());
                            coreCourse.getCourses().addAll(duplicates);
                            finished.add(coreCourse);
                        }
                    }
                }
            }
        }
        return finished;
    }

    @Override
    public Date getUpdateDate(String s) {
        JSONArray array = new JSONArray(s);
        JSONObject object = (JSONObject) array.get(1);
        String date = (String) object.get("Date");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy kk:mm");
        try {
            return simpleDateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }
    }
    @Override
    public String loadToken(String s){
        JSONArray array = new JSONArray(s);
        JSONObject object = (JSONObject) array.get(1);
        return (String) object.get("Id");
    }
    @Override
    public int getWeek(){
        try {
            URL connectwat = new URL("https://light.dsbcontrol.de/DSBlightWebsite/Data/a7f2b46b-4d23-446e-8382-404d55c31f90/" + getToken() + "/data.js");
            HttpsURLConnection urlConnection = (HttpsURLConnection) connectwat.openConnection();

            urlConnection.connect();

            BufferedInputStream bufferedInputStream = new BufferedInputStream(urlConnection.getInputStream());
            char c;
            StringBuilder stringBuilder = new StringBuilder();
            while (((int) (c = (char) bufferedInputStream.read())) != 65535) {
                stringBuilder.append(c);
            }
            stringBuilder.delete(0, stringBuilder.indexOf("weeks: ") + 15);
            stringBuilder.delete(2, stringBuilder.length());
            return Integer.parseInt(stringBuilder.toString());
        }catch (IOException e){
            e.printStackTrace();
            return 0;
        }
    }

    public ArrayList<CoreCourse> getCoreCourses() {
        return coreCourses;
    }

    public void setCoreCourses(ArrayList<CoreCourse> coreCourses) {
        this.coreCourses = coreCourses;
    }
}
