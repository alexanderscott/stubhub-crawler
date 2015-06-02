package crawler;

import crawler.runnables.EventAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by paul on 3/26/15.
 *
 * Takes a bunch of event ids as args, queries and sets them up in the database appropriately for the crawler.  In theory,
 * you should be able to run this while the crawler is running, and the crawler should pick up on new events for its next
 * reload.
 */
public class EventCreator {

    private static final Logger log = LoggerFactory.getLogger(EventCreator.class);

    private final StubhubApi api;
    private final Connection conn;
    public final RateLimitedRunnableLooper looper;

    public static void main(String[] args) {
        List<Integer> eventIds = new ArrayList<>();
        try {
            Files.lines(Paths.get("found_events.txt")).forEach(l -> eventIds.add(Integer.valueOf(l.split("\\|")[0])));
        } catch (IOException e) {
            log.error("Failed to load found_events.txt: ", e);
        }
        log.info("eventIds: " + eventIds);

        EventCreator eventCreator = new EventCreator(DBConnFactory.makeConnection());
        try {
            eventCreator.loadAdders(eventIds);
            eventCreator.looper.run();
        } catch (Exception e) {
            log.error("Error creating events", e);
        }
        eventCreator.looper.stop();
    }

    public EventCreator(Connection conn) {
        this.conn = conn;
        this.api = new StubhubApi();
        this.looper = new RateLimitedRunnableLooper(TimeUnit.SECONDS, 7, conn, false);
    }

    public void loadAdders(List<Integer> eventIdsToAdd) throws Exception {
        List<Integer> existingEventIds = getExistingVenueIds();
        for (Integer eventId : eventIdsToAdd) {
            looper.runnableDeque.add(new EventAdder(api, conn, eventId, existingEventIds));
        }
    }


}
