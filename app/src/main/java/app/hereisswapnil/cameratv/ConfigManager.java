package app.hereisswapnil.cameratv;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    public static final String SDCARD_FILE = "/sdcard/cameras.json";

    public static List<CameraItem> loadCameras(Context context) {
        List<CameraItem> cameras = new ArrayList<>();

        File targetFile = null;
        File f1 = new File(SDCARD_FILE);
        File f2 = context.getExternalFilesDir(null) != null ? new File(context.getExternalFilesDir(null), "cameras.json") : null;
        File f3 = new File(context.getFilesDir(), "cameras.json");

        if (f1.exists() && f1.canRead()) {
            targetFile = f1;
        } else if (f2 != null && f2.exists() && f2.canRead()) {
            targetFile = f2;
        } else if (f3.exists() && f3.canRead()) {
            targetFile = f3;
        }

        if (targetFile != null) {
            try {
                String content = readFile(targetFile);
                cameras = parseJson(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return cameras;
    }

    private static String readFile(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private static List<CameraItem> parseJson(String jsonStr) {
        List<CameraItem> list = new ArrayList<>();
        try {
            if (jsonStr.startsWith("[")) {
                JSONArray arr = new JSONArray(jsonStr);
                parseArray(arr, list);
            } else if (jsonStr.startsWith("{")) {
                JSONObject obj = new JSONObject(jsonStr);
                if (obj.has("cameras")) {
                    JSONArray arr = obj.getJSONArray("cameras");
                    parseArray(arr, list);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static void parseArray(JSONArray arr, List<CameraItem> list) {
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject item = arr.getJSONObject(i);
                String id = item.optString("id", "cam" + (i + 1));
                String name = item.optString("name", "Camera " + (i + 1));
                String url = item.optString("url", "").trim();
                boolean audio = item.optBoolean("audio", true);

                String ip = item.optString("ip", item.optString("host", "")).trim();
                int port = item.optInt("port", 554);
                String username = item.optString("username", item.optString("user", "")).trim();
                String password = item.optString("password", item.optString("pass", "")).trim();
                String path = item.optString("path", item.optString("stream", "")).trim();
                String protocol = item.optString("protocol", "rtsp").trim();

                // Skip entries that provide neither a URL nor an IP/host
                if (url.isEmpty() && ip.isEmpty()) {
                    continue;
                }

                list.add(new CameraItem(id, name, url, audio, ip, port, username, password, path, protocol));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
