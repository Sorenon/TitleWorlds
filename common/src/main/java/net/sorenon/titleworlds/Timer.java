package net.sorenon.titleworlds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Timer {

    private static final Logger LOGGER = LogManager.getLogger("TitleWorlds Profiling");

    private final boolean enabled;

    private long startTime = 0;
    private long lastTiming = 0;

    public Timer(boolean enabled) {
        this.enabled = enabled;
    }

    public void start() {
        this.startTime = System.nanoTime();
        this.lastTiming = this.startTime;
    }

    public void run(String name) {
        run(name, false);
    }

    public void run(String name, boolean ignoreIfSmall) {
        if (this.enabled) {
            long time = System.nanoTime();

            if (calcMillis(time - lastTiming) > 2 && ignoreIfSmall) {
                LOGGER.warn(name + " took " + calcMillis(time - lastTiming) + "ms");
                LOGGER.warn(name + " " + calcMillis(time - startTime) + "ms since start");
            } else if (!ignoreIfSmall) {
                LOGGER.info(name + " took " + calcMillis(time - lastTiming) + "ms");
                LOGGER.info(name + " " + calcMillis(time - startTime) + "ms since start");
            }

            this.lastTiming = time;
        }
    }

    private long calcMillis(long time) {
        return time / 1000000;
    }
}
