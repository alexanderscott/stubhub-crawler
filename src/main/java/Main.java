import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Created by paul on 3/15/15.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            FileInputStream props = new FileInputStream("app.properties");
            Properties properties = new Properties(System.getProperties());
            properties.load(props);
            System.setProperties(properties);
            System.getProperties().list(System.out);
        } catch (Exception e) {
            log.error("Failed to load system properties from app.properties", e);
        }

        Connection conn;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(
                    System.getProperty("db.url"),
                    System.getProperty("db.user"),
                    System.getProperty("db.pass"));
        } catch (Exception e) {
            log.error("Failed to connect to Database", e);
        }

        RateLimitedRunnableLooper looper = new RateLimitedRunnableLooper();
        looper.start();
    }
}
