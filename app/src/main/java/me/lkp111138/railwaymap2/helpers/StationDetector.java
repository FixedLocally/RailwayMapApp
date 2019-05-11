package me.lkp111138.railwaymap2.helpers;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class StationDetector {
    private static List<Station> stations;

    /**
     * @param filename the local file to load the json from
     * @throws IOException if we cant read the file
     * @throws JSONException if the json is invalid
     */
    public static void load(String filename) throws IOException, JSONException {
        stations = new ArrayList<>();
        String contents = getStringFromFile(filename);
        JSONArray arr = new JSONArray(contents);
        int length = arr.length();
        for (int i = 0; i < length; i++) {
            JSONObject station = arr.getJSONObject(i);
            String id = station.getString("id");
            String name = station.getString("name");
            JSONArray coords = station.getJSONArray("coords");
            stations.add(new Station(id, name, coords));
        }
    }

    /**
     * @param x x-coordinate clicked in the image
     * @param y y-coordinate clicked in the image
     * @return the station being clicked, null if none is clicked
     */
    @Nullable
    public static Station fromCoords(double x, double y) {
        for (Station station : stations) {
            if (between(x, station.minX, station.maxX)) {
                if (between(y, station.minY, station.maxY)) {
                    // we found it
                    return station;
                }
            }
        }
        return null;
    }

    /**
     * @param d the number to be checked
     * @param min lower bound
     * @param max upper bound
     * @return true if d is between min and max, false otherwise
     */
    private static boolean between(double d, double min, double max) {
        return d >= min && d <= max;
    }

    /**
     * @param file_path file path
     * @return a string containing the file's content
     * @throws IOException if theres an error reading the file
     */
    private static String getStringFromFile (String file_path) throws IOException {
        File fl = new File(file_path);
        FileInputStream fis = new FileInputStream(fl);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        fis.close();
        return sb.toString();
    }

    public static class Station {
        final public String id; // in a real app we search stations by id so this needs to be public
        final public String name;
        final int minX;
        final int maxX;
        final int minY;
        final int maxY;

        Station(String id, String name, JSONArray coords) throws JSONException {
            this.id = id;
            this.name = name;
            this.minX = coords.getInt(3);
            this.maxX = coords.getInt(2);
            this.minY = coords.getInt(1);
            this.maxY = coords.getInt(0);
        }

        @Override
        public String toString() {
            return "Station{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", minX=" + minX +
                    ", maxX=" + maxX +
                    ", minY=" + minY +
                    ", maxY=" + maxY +
                    '}';
        }
    }
}
