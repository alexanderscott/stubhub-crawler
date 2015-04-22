package crawler.runnables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Created by paul on 3/15/15.
 */
public class EventRecorderReloader implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(EventRecorderReloader.class);

    private static String FIND_EVENTS_QUERY = "SELECT id FROM events WHERE utc_date > now()";

    Deque<Runnable> runnableDeque;
    Connection conn;

    public EventRecorderReloader(Deque<Runnable> runnableDeque, Connection conn) {
        this.runnableDeque = runnableDeque;
        this.conn = conn;
    }

    @Override
    public void run() {
        log.info("Reloading looper...");

        List<Integer> eventIds = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(FIND_EVENTS_QUERY);
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next()) {
                eventIds.add(resultSet.getInt(1));
            }
        } catch (SQLException e) {
            log.error("Failed to query database for events", e);
        }

        eventIds.stream().forEach(id -> runnableDeque.add(new EventRecorder(conn, id)));
        log.info("Reloaded with " + eventIds.size() + " events");
    }
}
