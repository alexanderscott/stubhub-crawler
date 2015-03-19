import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    static AtomicInteger counter = new AtomicInteger(0);

    private static final Logger log = LoggerFactory.getLogger(RateLimitedRunnableLooper.class);

    private TimeUnit timeUnit;
    private long timeout;
    private volatile boolean running;

    private Connection conn;

    Deque<Runnable> runnableDeque = new ConcurrentLinkedDeque<Runnable>();

    public RateLimitedRunnableLooper(TimeUnit timeUnit, long timeout, Connection conn) {
        this.timeUnit = timeUnit;
        this.timeout = timeout;
        this.conn = conn;
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            long start = System.currentTimeMillis();

            if (!runnableDeque.isEmpty()) {
                runnableDeque.pollFirst().run();
            } else {
                new Reloader().run();
            }

            long elapsedMillis = System.currentTimeMillis() - start;

            try {
                Thread.sleep(timeUnit.toMillis(timeout) - elapsedMillis);
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

    private class Reloader implements Runnable {
        @Override
        public void run() {
            log.info("Reloading with 5 new guys");
            for (int i = 0; i < 5; i++) {
                runnableDeque.add(new Runnable() {
                    @Override
                    public void run() {
                        log.info("Here's a runnable: " + counter.incrementAndGet());
                    }
                });
            }

        }
    }

}
