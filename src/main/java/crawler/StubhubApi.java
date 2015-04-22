package crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by paul on 3/15/15.
 */
public class StubhubApi {

    private static final Logger log = LoggerFactory.getLogger(StubhubApi.class);
    private static ObjectMapper objectMapper = new ObjectMapper();

    private static String APP_TOKEN = System.getProperty("app.token");
    private static String baseUrl = "api.stubhub.com";
    private static int maxRows = 10_000;

    /**
     * @return the json response as a map.  Let the caller parse it into Listings and ListingUpdates as needed.
     */
    public Map getListingsForEvent(int eventId) {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("eventId", Integer.toString(eventId)));
        params.add(new BasicNameValuePair("rows", Integer.toString(maxRows)));
        return sendRequest(buildGetWithBodyRequest("/search/inventory/v1", params));

    }

    public Map getEventMetadata(int eventId) {
        URIBuilder builder = new URIBuilder()
                .setScheme("https")
                .setHost(baseUrl)
                .setPath("/catalog/events/v2/" + eventId);

        HttpGet request = null;
        try {
            request = new HttpGet(builder.build());
        } catch (URISyntaxException e) {
            log.error("Error building request to Stubhub: " + builder.toString(), e);
        }
        return sendRequest(addHeadersToRequest(request));
    }

    public Map findEvents(String performerName) {
        List<NameValuePair> params = new ArrayList<>();
//        params.add(new BasicNameValuePair("q", query));
        params.add(new BasicNameValuePair("performerName", performerName));
        params.add(new BasicNameValuePair("limit", Integer.toString(maxRows)));
        return sendRequest(buildGetWithBodyRequest("/search/catalog/events/v2", params));
    }

    private HttpUriRequest buildGetWithBodyRequest(String path, List<NameValuePair> params) {
        URIBuilder builder = new URIBuilder()
                .setScheme("https")
                .setHost(baseUrl)
                .setPath(path)
                .addParameters(params);

        HttpGetWithEntity request = null;
        try {
            request = new HttpGetWithEntity(builder.build());
            request.setEntity(new UrlEncodedFormEntity(params));
        } catch (Exception e) {
            log.error("Error building request to Stubhub: " + builder.toString(), e);
        }
        return addHeadersToRequest(request);
    }

    private HttpUriRequest addHeadersToRequest(HttpUriRequest request) {
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Authorization", "Bearer " + APP_TOKEN);
        request.setHeader("Accept", "application/json");
        request.setHeader("Accept-Encoding", "application/json");
        return request;
    }

    private Map sendRequest(HttpUriRequest request) {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            InputStream stream = response.getEntity().getContent();
            String body = IOUtils.toString(stream);
            return jsonToMap(body);
        } catch (IOException e) {
            log.error("Failed to get response from Stubhub API", e);
            return null;
        }
    }

    private static Map jsonToMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (IOException e) {
            log.error("Failed to parse into map: " + json, e);
        }
        return null;
    }

    /**
     * Wow, this should not be necessary. http://stackoverflow.com/questions/12535016/apache-httpclient-get-with-body
     */
    private static class HttpGetWithEntity extends HttpEntityEnclosingRequestBase {

        public HttpGetWithEntity(URI uri) {
            setURI(uri);
        }

        @Override
        public String getMethod() {
            return "GET";
        }
    }
}
