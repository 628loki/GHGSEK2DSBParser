package de.berstanio.ghgparser;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;

public class GHGParser {

    public static void main(String[] args) {
        Calendar calendar = Calendar.getInstance();
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        //Plan planThis = new Plan(week);
        String token = getToken();

        //planThis.parse(download(token, week));
        System.out.println(token);

        //Plan planNext = new Plan(week);
        StringBuilder stringBuilder = new StringBuilder();
        try {
            Files.readAllLines(Paths.get("c00023.htm"), StandardCharsets.ISO_8859_1).stream().forEachOrdered(stringBuilder::append);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //planNext.parse(download(token, week + 1));
        //planNext.parse(stringBuilder.toString());
        JahresStundenPlan jahresStundenPlan = new JahresStundenPlan(week);
        System.out.println(jahresStundenPlan.getToken());
    }

    public static String download(String token, int week){
        try {
            URL connectwat = new URL("https://light.dsbcontrol.de/DSBlightWebsite/Data/a7f2b46b-4d23-446e-8382-404d55c31f90/" + token + "/" + week + "/c/c00023.htm");
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

    public static String getToken(){
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
}
