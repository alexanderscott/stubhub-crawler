package crawler.runnables;

import crawler.StubhubApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by paul on 3/28/15.
 */
public class EventAdder implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(EventAdder.class);
    private static SimpleDateFormat RFC822 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private StubhubApi api;
    private Connection conn;
    private int eventId;

    public EventAdder(StubhubApi api, Connection conn, int eventId) {
        this.api = api;
        this.conn = conn;
        this.eventId = eventId;
    }

    @Override
    public void run() {
        Map eventMetadata = api.getEventMetadata(eventId);

        List<Integer> existingVenueIds = null;
        try {
            existingVenueIds = getExistingVenueIds();
        } catch (SQLException e) {
            log.error("Cannot find existings venues in DB", e);
        }

        Map venueMap = (Map) eventMetadata.get("venue");
        int venueId = (int) venueMap.get("id");
        if (!existingVenueIds.contains(venueId)) {
            try {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO venues (id, name, address1, city, state, zipcode, country) VALUES \n" +
                        "(?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, venueId);
                ps.setString(2, ((String) venueMap.get("name")));
                ps.setString(3, ((String) venueMap.get("address1")));
                ps.setString(4, ((String) venueMap.get("city")));
                ps.setString(5, ((String) venueMap.get("state")));
                ps.setString(6, ((String) venueMap.get("zipCode")));
                ps.setString(7, ((String) venueMap.get("country")));
                ps.execute();
            } catch (SQLException e) {
                log.error("Failed to store venue in DB", e);
            }
        }

        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO events (id, title, description, status, local_date, utc_date, venue_id) VALUES\n" +
                    "(?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, eventId);
            ps.setString(2, ((String) eventMetadata.get("title")));
            ps.setString(3, ((String) eventMetadata.get("description")));
            ps.setInt(4, (Integer) ((Map)eventMetadata.get("status")).get("statusId"));

            // good lord, this should not be that hard
            String dateLocalString = (String) eventMetadata.get("eventDateLocal");
            String dateUTCString = (String) eventMetadata.get("eventDateUTC");
            String cleanLocal = dateLocalString.replaceAll("([\\+\\-]\\d\\d):(\\d\\d)", "$1$2");
            String cleanUTC = dateUTCString.replaceAll("([\\+\\-]\\d\\d):(\\d\\d)", "$1$2");
            ps.setTimestamp(5, new Timestamp(RFC822.parse(cleanLocal).getTime()));
            ps.setTimestamp(6, new Timestamp(RFC822.parse(cleanUTC).getTime()));

            ps.setInt(7, venueId);

            ps.execute();
        } catch (SQLException e) {
            log.error("Failed to store event in DB", e);
        } catch (ParseException e) {
            log.error("Can't parse the timestamp", e);
        }

        log.info("Event " + eventId + " stored in DB");
    }

    public List<Integer> getExistingVenueIds() throws SQLException {
        List<Integer> existingVenueIds = new ArrayList<>();
        PreparedStatement ps = conn.prepareStatement("SELECT id FROM venues");

        ResultSet resultSet = ps.executeQuery();
        while (resultSet.next()) {
            existingVenueIds.add(resultSet.getInt("id"));
        }

        ps.close();
        return existingVenueIds;
    }
}
