package crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.StopWatch;

import java.sql.Connection;
import java.util.concurrent.TimeUnit;

/**
 * Created by paul on 3/15/15.
 */
public class Crawler {

    private static final Logger log = LoggerFactory.getLogger(Crawler.class);

    public final RateLimitedRunnableLooper looper;

    public static void main(String[] args) {

        Crawler crawler = new Crawler(DBConnFactory.makeConnection());
        crawler.looper.run();

        // look at me being all nice and tidy
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                crawler.looper.stop();
            }
        });
    }

    public Crawler(Connection conn) {
        this.looper = new RateLimitedRunnableLooper(TimeUnit.SECONDS, 7, conn, true);
    }
}
