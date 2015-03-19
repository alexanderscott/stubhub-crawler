package models;

import java.time.LocalDate;

/**
 * Created by paul on 3/18/15.
 */
public class Event {
    public int id;
    public String title;
    public String description;
    public EventStatus status;
    public LocalDate localDate;
    public LocalDate utcDate;
    public Venue venue;

    public enum EventStatus {
        Active,
        Contingent,
        Cancelled,
        Completed,
        Postponed,
        Scheduled
    }

    public Event(int id, String title, String description, EventStatus status, LocalDate localDate, LocalDate utcDate, Venue venue) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.localDate = localDate;
        this.utcDate = utcDate;
        this.venue = venue;
    }
}
