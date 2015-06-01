package crawler.runnables;

import crawler.StubhubApi;
import crawler.models.ListingUpdate;
import crawler.models.NewListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StopWatch;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by paul on 3/15/15.
 */
public class EventRecorder implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(EventRecorder.class);

    private StubhubApi api = new StubhubApi();
    private Connection conn;
    private int eventId;
    private boolean noWrite = false; // true for debugging purposes

    Timestamp timestamp;

    public EventRecorder(Connection conn, int eventId) {
        this.conn = conn;
        this.eventId = eventId;
    }

    @Override
    public void run() {
        try {
            conn.setAutoCommit(false);

            // get existing listings
            StopWatch sw = new StopWatch();
            Map<Integer, ListingUpdate> storedListings = getStoredListings();
            Set<Integer> storedListingIds = storedListings.values().stream()
                    .map(ls -> ls.id)
                    .collect(Collectors.toSet());
            Set<Integer> recentlyDelistedIds = storedListings.values().stream()
                    .filter(ls -> ls.quantity.equals(0))
                    .map(ls -> ls.id)
                    .collect(Collectors.toSet());
            log.info("(" + eventId + ") " + storedListings.size() + " listings found in DB in " + sw);

            // query stubhub
            sw = new StopWatch();
            Map eventMap = api.getListingsForEvent(eventId);
            timestamp = new Timestamp(ZonedDateTime.now().toInstant().toEpochMilli());
            List<Map> currentListingsMaps = (List<Map>) eventMap.get("listing");
            if (currentListingsMaps == null) {
                log.warn("No listings found for " + eventId);
                return;
            }
            Set<Integer> currentListingIds = currentListingsMaps.stream()
                    .map(o -> (int) o.get("listingId"))
                    .collect(Collectors.toSet());
            log.info("(" + eventId + ") " + currentListingIds.size() + " listings found on stubhub in " + sw);

            // match them up and update/insert as needed
            sw = new StopWatch();
            List<NewListing> newListings = new ArrayList<>();
            List<ListingUpdate> firstListingUpdates = new ArrayList<>();
            List<ListingUpdate> listingUpdates = new ArrayList<>();
            int matchedCount = 0;
            for (Map listingMap : currentListingsMaps) {
                try {
                    int listingId = (int) listingMap.get("listingId");

                    if (storedListingIds.contains(listingId)) {
                        matchedCount++;
                        ListingUpdate currentListingUpdate = ListingUpdate.makeFromStubhubMap(listingMap);
                        ListingUpdate oldListingUpdate = storedListings.get(listingId);

                        if (!currentListingUpdate.equals(oldListingUpdate)) { // insert if any changes
                            ListingUpdate listingUpdate = ListingUpdate.makeNewUpdate(currentListingUpdate, oldListingUpdate);
                            if (listingUpdate != null) {
                                listingUpdates.add(listingUpdate);
                            }
                        }
                    } else {
                        // make new listing for insertion
                        NewListing newListing = new NewListing(listingId, (Integer) listingMap.get("sectionId"),
                                (String) listingMap.get("row"), (Integer) listingMap.get("zoneId"), (String) listingMap.get("zoneName"),
                                (String) listingMap.get("sellerSectionName"));
                        newListings.add(newListing);

                        // make first listing updates for insertion
                        ListingUpdate firstListingUpdate = new ListingUpdate(
                                listingId,
                                ((Double) ((Map) listingMap.get("currentPrice")).get("amount")),
                                ListingUpdate.makeSeatsArray((String) listingMap.get("seatNumbers")),
                                (Integer) listingMap.get("quantity")
                        );
                        firstListingUpdates.add(firstListingUpdate);
                    }
                } catch (Exception e) {
                    log.error("Failed to deal with listing: " + listingMap, e);
                }
            }

            // insert new listings
            insertNewListings(newListings);
            // combine "first time" updates with actual updates and insert them all
            List<ListingUpdate> updates = new ArrayList<>();
            updates.addAll(firstListingUpdates);
            updates.addAll(listingUpdates);
            updateListings(updates);

            // mark listings that can't be found anymore as delisted
            Set<Integer> idsToDelist = new HashSet<>(storedListingIds); // amongst all stored listings...
            idsToDelist.removeAll(currentListingIds); // don't mark as delisted if we just saw a live listing...
            idsToDelist.removeAll(recentlyDelistedIds); // or if the most recent update is a "delist" (that would be redundant)
            markDeslisted(idsToDelist);

            log.info("(" + eventId + ") " + newListings.size() + " new listings | " +
                    idsToDelist.size() + " delisted listings | " +
                    listingUpdates.size() + " of " + matchedCount + " matched listings updated in " + sw);

            conn.commit();
            conn.setAutoCommit(true);

        } catch (SQLException e) {
            log.error("Failed to process new listings for event " + eventId, e);
        } finally {

        }

    }

    /**
     * @return a map of listingId -> current listing in the database
     */
    private Map<Integer, ListingUpdate> getStoredListings() throws SQLException {
        PreparedStatement listingsSelect = conn.prepareStatement("SELECT lu.listing_id, lu.update_time, lu.price, lu.quantity, lu.seats " +
                "FROM listing_updates lu " +
                "JOIN listings l ON l.id = lu.listing_id " +
                "WHERE l.event_id = ? " +
//                "AND lu.listing_id NOT IN (SELECT listing_id from listing_updates WHERE quantity = 0) " + // DON'T filter out those that have already been delisted, they may come back again???
                "ORDER BY lu.update_time;");
        listingsSelect.setInt(1, eventId);
        ResultSet resultSet = listingsSelect.executeQuery();

        Map<Integer, ListingUpdate> storedListings = new HashMap<>();
        while (resultSet.next()) {
            int id = resultSet.getInt("listing_id");
            ListingUpdate listingUpdate = storedListings.get(id);
            if (listingUpdate == null) {
                listingUpdate = new ListingUpdate(id, null, null, null);
            }

            Double price = getDoubleOrNull(resultSet, "price");
            Integer quantity = getIntOrNull(resultSet, "quantity");
            Array array = getArrayOrNull(resultSet, "seats");
            Integer[] seats = array == null ? null : (Integer[]) array.getArray();

            // put it in if it's in this row.  by the end (because of ordering in the query),
            // the object will have the most recent values for the listing
            if (price != null)
                listingUpdate.price = price;
            if (quantity != null)
                listingUpdate.quantity = quantity;
            if (seats != null)
                listingUpdate.seats = seats;

            storedListings.put(id, listingUpdate);
        }
        return storedListings;
    }

    /**
     * build and run a query of updates to listings
     */
    private void updateListings(List<ListingUpdate> listingUpdates) throws SQLException {
        if (listingUpdates.isEmpty() || noWrite) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO listing_updates (listing_id, update_time, price, quantity, seats) VALUES ");
        for (int i = 0; i < listingUpdates.size(); i++) {
            if (i != listingUpdates.size() - 1) {
                sb.append("(?, ?, ?, ?, ?), ");
            } else {
                sb.append("(?, ?, ?, ?, ?);");
            }
        }

        PreparedStatement insert = conn.prepareStatement(sb.toString());
        int paramIdx = 1;
        for (ListingUpdate listingUpdate : listingUpdates) {
            insert.setInt(paramIdx++, listingUpdate.id);
            insert.setTimestamp(paramIdx++, timestamp);
            setDoubleOrNull(insert, paramIdx++, listingUpdate.price);
            setIntOrNull(insert, paramIdx++, listingUpdate.quantity);
            setIntArrayOrNull(insert, paramIdx++, listingUpdate.seats);
        }

        insert.execute();
    }

    /**
     * build and run a single query to insert all new listings
     */
    private void insertNewListings(List<NewListing> newListings) throws SQLException {
        if (newListings.isEmpty() || noWrite) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO listings (id, event_id, section_id, rowe, zone_id, zone_name, seller_section_name) VALUES ");
        for (int i = 0; i < newListings.size(); i++) {
            if (i != newListings.size() - 1) {
                sb.append("(?, ?, ?, ?, ?, ?, ?), ");
            } else { // last one
                sb.append("(?, ?, ?, ?, ?, ?, ?);");
            }
        }

        PreparedStatement insert = conn.prepareStatement(sb.toString());
        int paramIdx = 1;
        for (NewListing newListing : newListings) {
            insert.setInt(paramIdx++, newListing.listingId);
            insert.setInt(paramIdx++, eventId);
            setIntOrNull(insert, paramIdx++, newListing.sectionId);
            insert.setString(paramIdx++, newListing.row); // maybe you think this should be an int? you thought wrong!
            setIntOrNull(insert, paramIdx++, newListing.zoneId);
            insert.setString(paramIdx++, newListing.zoneName);
            insert.setString(paramIdx++, newListing.sellerSectionName);
        }

        insert.execute();
    }

    /**
     * build and run query to insert final listing_update for a listing id that no longer exists
     */
    private void markDeslisted(Set<Integer> delistedIds) throws SQLException {
        if (delistedIds.isEmpty() || noWrite) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO listing_updates (listing_id, update_time, quantity) VALUES ");
        for (int i = 0; i < delistedIds.size(); i++) {
            if (i != delistedIds.size() - 1) {
                sb.append("(?, ?, 0), ");
            } else {
                sb.append("(?, ?, 0);");
            }
        }

        PreparedStatement insert = conn.prepareStatement(sb.toString());
        int paramIdx = 1;
        for (Integer id : delistedIds) {
            insert.setInt(paramIdx++, id);
            insert.setTimestamp(paramIdx++, timestamp);
        }

        insert.execute();
    }

    // null get/set utilities because java.sql actually sucks

    private void setIntOrNull(PreparedStatement ps, int paramIdx, Integer integer) throws SQLException {
        if (integer == null) {
            ps.setNull(paramIdx, Types.NULL);
        } else {
            ps.setInt(paramIdx, integer);
        }
    }

    private void setDoubleOrNull(PreparedStatement ps, int paramIdx, Double doub) throws SQLException {
        if (doub == null) {
            ps.setNull(paramIdx, Types.NULL);
        } else {
            ps.setDouble(paramIdx, doub);
        }
    }

    private void setIntArrayOrNull(PreparedStatement ps, int paramIdx, Integer[] arr) throws SQLException {
        if (arr == null) {
            ps.setNull(paramIdx, Types.NULL);
        } else {
            Array array = conn.createArrayOf("smallint", arr);
            ps.setArray(paramIdx, array);
        }
    }

    private Integer getIntOrNull(ResultSet rs, String colName) throws SQLException {
        int val = rs.getInt(colName);
        return rs.wasNull() ? null : val;
    }

    private Double getDoubleOrNull(ResultSet rs, String colName) throws SQLException {
        double val = rs.getDouble(colName);
        return rs.wasNull() ? null : val;
    }

    private Array getArrayOrNull(ResultSet rs, String colName) throws SQLException {
        Array arr = rs.getArray(colName);
        return rs.wasNull() ? null : arr;
    }

}
