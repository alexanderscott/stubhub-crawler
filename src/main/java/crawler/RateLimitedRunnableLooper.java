package crawler;

import crawler.runnables.EventRecorderReloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * Created by paul on 3/15/15.
 *
 * Maintains a circular linked list of Runnables that are called according to the rate limit specified.  It's probably
 * way lower level than it needs to be - I'm sure there's some lovely guy executor-service-what-not out there that would
 * do this automagically, but I can't find it.
 *
 * It reloads itself
 */
public class RateLimitedRunnableLooper implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(RateLimitedRunnableLooper.class);

    private TimeUnit timeUnit;
    private long timeout;
    private volatile boolean running;
    private final boolean reloadable; // if false, it just stops when the Deque is empty

    private Connection conn;

    Deque<Runnable> runnableDeque = new ConcurrentLinkedDeque<Runnable>();

    public RateLimitedRunnableLooper(TimeUnit timeUnit, long timeout, Connection conn, boolean reloadable) {
        this.timeUnit = timeUnit;
        this.timeout = timeout;
        this.conn = conn;
        this.reloadable = reloadable;
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            log.info("******************************************");

            long start = System.currentTimeMillis();

            if (!runnableDeque.isEmpty()) {
                runnableDeque.pollFirst().run();
            } else {
                if (reloadable) {
                    new EventRecorderReloader(runnableDeque, conn).run();
                } else {
                    break;
                }
            }

            long elapsedMillis = System.currentTimeMillis() - start;
            long sleepTime = timeUnit.toMillis(timeout) - elapsedMillis;
            try {
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                // ain't never gonna happen
            }
        }
    }

    public void stop() {
        running = false;
        try {
            conn.close();
        } catch (SQLException e) {
            log.error("Trouble closing connection", e);
        }
    }

}
