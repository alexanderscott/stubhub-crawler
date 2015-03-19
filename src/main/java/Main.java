import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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

        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(
                    System.getProperty("db.url"),
                    System.getProperty("db.user"),
                    System.getProperty("db.pass"));

        } catch (Exception e) {
            log.error("Failed to connect to Database", e);
        }

        final RateLimitedRunnableLooper looper = new RateLimitedRunnableLooper(TimeUnit.SECONDS, 7, conn);
        looper.run();

        // look at me being all nice and tidy
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                looper.stop();
            }
        });
    }
}
