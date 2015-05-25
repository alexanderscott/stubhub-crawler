package crawler.models;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by paul on 3/18/15.
 *
 * The DB stores more metadata on the event, but this the only stuff we care about
 * for manipulation in the code.  In the other words, this is the non-static data that we
 * check for changes on each update.
 */
public class ListingUpdate {

    static double priceEpsilon = 0.001d;

    public int id;
    public Double price;
    public Integer[] seats;
    public Integer quantity;

    public ListingUpdate(int id, Double price, Integer[] seats, Integer quantity) {
        this.id = id;
        this.price = price;
        this.seats = seats;
        this.quantity = quantity;
    }

    /**
     * @param listingMap map deserialized directly from stubhub JSON
     */
    public static ListingUpdate makeFromStubhubMap(Map listingMap) {
        int id = (int) listingMap.get("listingId");
        Double price = (Double) ((Map) listingMap.get("currentPrice")).get("amount");
        Integer quantity = (Integer) listingMap.get("quantity");
        Integer[] seats = makeSeatsArray((String) listingMap.get("seatNumbers"));

        return new ListingUpdate(id, price, seats, quantity);
    }

    /**
     * ugh, this is all so obnoxious, but that's what i get for using a postgres array
     */
    public static Integer[] makeSeatsArray(String s) {
        Integer[] seatsArray;
        try {
            String[] splitString = s.split(",");
            seatsArray = new Integer[splitString.length];
            for (int i = 0; i < splitString.length; i++) {
                seatsArray[i] = Integer.valueOf(splitString[i]);
            }
        } catch (NumberFormatException e) { // if we can't parse seat numbers, it's probably something weird like general admission; null is fine
            seatsArray = null;
        } catch (NullPointerException e) { // there is
            seatsArray = null;
        }
        return seatsArray;
    }

    /**
     * @param currentListingUpdate listing as it is currently
     * @param oldListingUpdate listing pieced together from database
     * @return a {@link crawler.models.ListingUpdate} that has values where there are changes, and nulls
     * where there are not
     */
    public static ListingUpdate makeNewUpdate(ListingUpdate currentListingUpdate, ListingUpdate oldListingUpdate) {
        Double price = null;
        Integer quantity = null;
        Integer[] seats = null;

        if (!samePrices(currentListingUpdate, oldListingUpdate)) {
            price = currentListingUpdate.price;
        }
        if (!currentListingUpdate.quantity.equals(oldListingUpdate.quantity)) {
            quantity = currentListingUpdate.quantity;
        }
        if (currentListingUpdate.seats != null &&
                !currentListingUpdate.seats.equals(oldListingUpdate.seats)) {
            seats = currentListingUpdate.seats;
        }

        if (price != null || quantity != null || seats != null) {
            return new ListingUpdate(currentListingUpdate.id, price, seats, quantity);
        } else {
            return null;
        }
    }

    private static boolean samePrices(ListingUpdate currentListingUpdate, ListingUpdate oldListingUpdate) {
        if (currentListingUpdate.price == null || oldListingUpdate.price == null)
            return false;

        double currPrice = currentListingUpdate.price;
        double oldPrice = oldListingUpdate.price;
        return Math.abs(currPrice - oldPrice) < priceEpsilon;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListingUpdate that = (ListingUpdate) o;

        if (id != that.id) return false;
        if (price != null ? !price.equals(that.price) : that.price != null) return false;
        if (quantity != null ? !quantity.equals(that.quantity) : that.quantity != null) return false;
        if (!Arrays.equals(seats, that.seats)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (price != null ? price.hashCode() : 0);
        result = 31 * result + (seats != null ? Arrays.hashCode(seats) : 0);
        result = 31 * result + (quantity != null ? quantity.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ListingUpdate{" +
                "id=" + id +
                ", price=" + price +
                ", seats=" + Arrays.toString(seats) +
                ", quantity=" + quantity +
                '}';
    }
}
