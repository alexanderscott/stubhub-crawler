package crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by paul on 4/20/15.
 */
public class EventFinder {

    private static final Logger log = LoggerFactory.getLogger(EventFinder.class);

    private final StubhubApi api;

    public EventFinder() {
        this.api = new StubhubApi();
    }

    private void findEvents() {
        Map events = api.findEvents("Chicago Cubs");
        List<Map> eventsJson = ((List<Map>) events.get("events"));

        try {
            PrintWriter pw = new PrintWriter("found_events.txt");
            for (Map map : eventsJson) {
                Integer id = (Integer) map.get("id");
                String title = (String) map.get("title");
                pw.append(id + "|" + title + "\n");
            }
            pw.close();
        } catch (FileNotFoundException e) {
            log.error("Cannot write events to file: ", e);
        }
    }

    public static void main(String[] args) {
        new EventFinder().findEvents();
    }
}
