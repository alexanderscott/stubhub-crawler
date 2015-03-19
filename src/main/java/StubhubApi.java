
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by paul on 3/15/15.
 */
public class StubhubApi {

    private static final Logger log = LoggerFactory.getLogger(StubhubApi.class);

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static String APP_TOKEN = System.getProperty("stubhub.app.token");

    public Map getListingsForEvent(int eventId) {
        return null;
    }

    public void send() {
        List<NameValuePair> headers = new ArrayList<NameValuePair>();
        headers.add(new BasicNameValuePair("Content-Type", "application/json"));
        headers.add(new BasicNameValuePair("Authorization", "Bearer " + APP_TOKEN));
        headers.add(new BasicNameValuePair("Accept", "application/json"));
        headers.add(new BasicNameValuePair("Accept-Encoding", "application/json"));

        HttpClient httpClient;
    }

    private static Map jsonToMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (IOException e) {
            log.error("Failed to parse into map: " + json, e);
        }
        return null;
    }
}
