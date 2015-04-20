package crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Created by paul on 3/26/15.
 */
public class DBConnFactory {

    private static final Logger log = LoggerFactory.getLogger(DBConnFactory.class);

    public static Connection makeConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(
                    System.getProperty("db.url"),
                    System.getProperty("db.user"),
                    System.getProperty("db.pass"));

        } catch (Exception e) {
            log.error("Failed to connect to Database", e);
            return null;
        }
    }
}
