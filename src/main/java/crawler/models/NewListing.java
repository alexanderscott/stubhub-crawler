package crawler.models;

/**
 * Created by paul on 4/11/15.
 */
public class NewListing {
    public int listingId;
    public Integer sectionId;
    public String row;
    public Integer zoneId;
    public String zoneName;
    public String sellerSectionName;

    public NewListing(int listingId, Integer sectionId, String row, Integer zoneId, String zoneName, String sellerSectionName) {
        this.listingId = listingId;
        this.sectionId = sectionId;
        this.row = row;
        this.zoneId = zoneId;
        this.zoneName = zoneName;
        this.sellerSectionName = sellerSectionName;
    }

    @Override
    public String toString() {
        return "NewListing{" +
                "listingId=" + listingId +
                ", sectionId=" + sectionId +
                ", row='" + row + '\'' +
                ", zoneId=" + zoneId +
                ", zoneName='" + zoneName + '\'' +
                ", sellerSectionName='" + sellerSectionName + '\'' +
                '}';
    }
}
