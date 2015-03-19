package models;

/**
 * Created by paul on 3/18/15.
 */
public class Venue {
    public int id;
    public String name;
    public String address1;
    public String city;
    public String state;
    public String zipcode;
    public String country;

    public Venue(int id, String name, String address1, String city, String state, String zipcode, String country) {
        this.id = id;
        this.name = name;
        this.address1 = address1;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
        this.country = country;
    }
}
