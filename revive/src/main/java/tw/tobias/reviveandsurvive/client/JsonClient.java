package tw.tobias.reviveandsurvive.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;

public class JsonClient {
    private final Gson gson = new Gson();
    private static final Type collectionType
            = new TypeToken<Collection<PitStop>>(){}.getType();

    Collection<PitStop> parse(InputStream in) {
        return gson.fromJson(
                new InputStreamReader(in, Charset.defaultCharset()),
                collectionType
        );
    }

    public Collection<PitStop> getStops(double lat, double lon, double radius) throws IOException {
        URL url = new URL(String.format("http://vertex.xyz/?latitude=%s&longitude=%s&radius=%s", lat, lon, radius));

        InputStream in = null;
        try {
            in = url.openStream();
            return parse(in);
        } catch (JsonIOException e) {
            throw new IOException(e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
