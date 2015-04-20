package util;

/**
 * Created by paul on 4/19/15.
 */
public class StopWatch {

    private final long startTime;

    public StopWatch() {
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return Long.toString(System.currentTimeMillis() - startTime) + " ms";
    }
}
