package itdelatrisu.craq;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Garbage collection manager. */
public class GarbageManager {
	private static final Logger logger = LoggerFactory.getLogger(GarbageManager.class);

	/** The scheduler service. */
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	/** Baseline memory used by the JVM (in bytes). */
	private long baselineMemoryUsed = 0;

	/** Minimum memory used by the JVM (in bytes) before running garbage collection. */
	private final long gcMemoryThreshold;

	/** Creates the garbage manager with the default garbage collection threshold. */
	public GarbageManager() { this(256 * 1_000_000L); /* 256MB */ }

	/** Creates the garbage manager with the given garbage collection threshold. */
	public GarbageManager(long gcMemoryThreshold) {
		this.gcMemoryThreshold = gcMemoryThreshold;
	}

	/** Starts periodic garbage management in a separate thread with default timing parameters. */
	public void start() { start(500, 2000, TimeUnit.MILLISECONDS); }

	/** Starts periodic garbage management in a separate thread. */
	public void start(long initialDelay, long period, TimeUnit unit) {
		scheduler.scheduleAtFixedRate(this::gc, initialDelay, period, unit);
	}

	/** Invokes the garbage collector. */
	private void gc() {
		long usedMemory = getUsedMemory();
		if (baselineMemoryUsed > 0 && usedMemory - baselineMemoryUsed < gcMemoryThreshold)
			return;

		System.gc();
		baselineMemoryUsed = getUsedMemory();
		logger.debug("Freed {}.", bytesToString(usedMemory - baselineMemoryUsed));
	}

	/** Returns the amount memory used by the JVM (in bytes). */
	private long getUsedMemory() {
		Runtime r = Runtime.getRuntime();
		return r.totalMemory() - r.freeMemory();
	}

	/**
	 * Returns a human-readable representation of a given number of bytes.
	 * @param bytes the number of bytes
	 * @return the string representation
	 * @author aioobe (http://stackoverflow.com/a/3758880)
	 */
	private String bytesToString(long bytes) {
		if (bytes < 1024)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(1024));
		char pre = "KMGTPE".charAt(exp - 1);
		return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
	}
}
