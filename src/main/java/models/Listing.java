package models;

import java.util.List;

/**
 * Created by paul on 3/18/15.
 */
public class Listing {
    public int id;
    public int eventId;
    public List<Integer> seats;
    public int row;
    public int zoneId;
    public String zoneName;
    public String sellerSectionName;

    public Listing(int id, int eventId, List<Integer> seats, int row, int zoneId, String zoneName, String sellerSectionName) {
        this.id = id;
        this.eventId = eventId;
        this.seats = seats;
        this.row = row;
        this.zoneId = zoneId;
        this.zoneName = zoneName;
        this.sellerSectionName = sellerSectionName;
    }
}
