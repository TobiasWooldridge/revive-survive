package tw.tobias.reviveandsurvive.client;

import android.location.Location;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

public class CrashStatsClient {
    private static final String TAG = "CrashStatsClient";
    private final Gson gson = new Gson();
    private static final Type collectionType = new TypeToken<Response>(){}.getType();

    private static class Response {
        int crashes;
    }

    int parse(InputStream in) {
        Response response = gson.fromJson(
                new InputStreamReader(in, Charset.defaultCharset()),
                collectionType
        );

        Log.d(TAG, response.crashes + " crashes loaded");

        return response.crashes;
    }


    public int getNumAccidents(Location location, double radius) throws IOException {
        URL url = new URL(String.format("http://54.210.25.223/risk.php?latitude=%s&longitude=%s&radius=%s", location.getLatitude(), location.getLongitude(), radius));

        InputStream in = null;
        try {
            in = url.openStream();
            return parse(in);
        } catch (JsonIOException e) {
            Log.d(TAG, e.toString());
            throw new IOException(e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
