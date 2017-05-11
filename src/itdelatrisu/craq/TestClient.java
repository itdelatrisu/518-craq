package itdelatrisu.craq;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import itdelatrisu.craq.CraqClient.ReadObject;
import itdelatrisu.craq.thrift.CraqConsistencyModel;

/** CRAQ client tests. */
public class TestClient {
	private static final Logger logger = LoggerFactory.getLogger(TestClient.class);

	/** Number of additional trials to run in benchmarks, with this number of
	    initial trials discarded (to avoid skew from JIT optimization). */
	private static final int WARMUP_TRIALS = 5;

	/** RNG instance. */
	private static final Random RANDOM = new Random();

	/** Read statistics wrapper. */
	private static class ReadStats {
		/** Read counters. */
		private final long succeeded, clean, dirty;
		/** Constructor. */
		public ReadStats(long succeeded, long clean, long dirty) {
			this.succeeded = succeeded;
			this.clean = clean;
			this.dirty = dirty;
		}
		/** Returns the number of successful reads. */
		public long getTotalReads() { return succeeded; }
		/** Returns the number of clean reads. */
		public long getCleanReads() { return clean; }
		/** Returns the number of dirty reads. */
		public long getDirtyReads() { return dirty; }
	}

	@TestMethod(desc="Basic write operation.", minArgs=1, params="<value>")
	public static void write(String host, int port, String[] args) throws TException {
		String value = args[0];

		CraqClient client = new CraqClient(host, port);
		client.connect();
		long newVersion = client.write(value);
		logger.info(
			"write(): writing object {} ({})",
			value, newVersion >= 0 ? "version " + newVersion : "FAIL"
		);
		client.close();
	}

	@TestMethod(desc="Basic fixed-size write operation.", minArgs=1, params="<size_bytes>")
	public static void writeBytes(String host, int port, String[] args) throws TException {
		int numBytes = Integer.parseInt(args[0]);

		CraqClient client = new CraqClient(host, port);
		client.connect();
		long newVersion = client.write(getRandomString(numBytes));
		logger.info(
			"writeBytes(): writing {}-byte object ({})",
			numBytes, newVersion >= 0 ? "version " + newVersion : "FAIL"
		);
		client.close();
	}

	@TestMethod(desc="Basic test-and-set operation.", minArgs=2, params="<value> <expected_version>")
	public static void testAndSet(String host, int port, String[] args) throws TException {
		String value = args[0];
		long expectedVersion = Long.parseLong(args[1]);

		CraqClient client = new CraqClient(host, port);
		client.connect();
		long newVersion = client.testAndSet(value, expectedVersion);
		logger.info(
			"testAndSet(): writing object {} for expected version {} ({})",
			value, expectedVersion, newVersion >= 0 ? "now version " + newVersion : "FAIL"
		);
		client.close();
	}

	@TestMethod(desc="Basic read operation (strong consistency).")
	public static void readStrong(String host, int port, String[] args) throws TException {
		CraqClient client = new CraqClient(host, port);
		client.connect();
		ReadObject obj = client.read(CraqConsistencyModel.STRONG, 0);
		if (obj == null)
			logger.info("readStrong(): null object");
		else
			logger.info("readStrong(): read object {}", obj);
		client.close();
	}

	@TestMethod(desc="Basic read operation (eventual consistency).")
	public static void readEventual(String host, int port, String[] args) throws TException {
		CraqClient client = new CraqClient(host, port);
		client.connect();
		ReadObject obj = client.read(CraqConsistencyModel.EVENTUAL, 0);
		if (obj == null)
			logger.info("readEventual(): null object");
		else
			logger.info("readEventual(): read object {}", obj);
		client.close();
	}

	@TestMethod(desc="Basic read operation (eventual bounded consistency).", minArgs=1, params="<version_bound>")
	public static void readEventualBounded(String host, int port, String[] args) throws TException {
		long versionBound = Long.parseLong(args[0]);

		CraqClient client = new CraqClient(host, port);
		client.connect();
		ReadObject obj = client.read(CraqConsistencyModel.EVENTUAL_BOUNDED, versionBound);
		if (obj == null)
			logger.info("readEventualBounded(): null object");
		else
			logger.info("readEventualBounded(): read object {}", obj);
		client.close();
	}

	@TestMethod(desc="Benchmarks read operations.", minArgs=3,
	            params="<num_clients> <milliseconds> <trials> {<additional_ip>:<additional_port> ...}")
	public static void benchmarkRead(String host, int port, String[] args)
		throws TException, InterruptedException, ExecutionException {
		int numClients = Integer.parseInt(args[0]);
		int ms = Integer.parseInt(args[1]);
		int trials = Integer.parseInt(args[2]);
		int numServers = 1 + args.length - 3;
		String[] hosts = new String[numServers];
		hosts[0] = host;
		int[] ports = new int[numServers];
		ports[0] = port;
		for (int i = 0; i < numServers - 1; i++) {
			String[] s = args[i + 3].split(":");
			hosts[i + 1] = s[0];
			ports[i + 1] = Integer.parseInt(s[1]);
		}

		// connect to servers
		CraqClient[] clients = new CraqClient[numClients];
		for (int i = 0; i < numClients; i++) {
			int serverIndex = i % numServers;
			clients[i] = new CraqClient(hosts[serverIndex], ports[serverIndex]);
			clients[i].connect();
		}

		// check if any object is written...
		if (clients[0].read(CraqConsistencyModel.STRONG, 0) == null) {
			logger.info("benchmarkRead(): could not read object.");
			return;
		}

		// run trials...
		List<Long> throughputs = new ArrayList<>(trials);
		for (int run = 0; run < trials + WARMUP_TRIALS; run++) {
			// begin execution
			ExecutorService executor = Executors.newFixedThreadPool(numClients);
			List<Future<Long>> futures = new ArrayList<>(numClients);
			long startTime = System.nanoTime();
			for (int i = 0; i < numClients; i++) {
				CraqClient client = clients[i];
				futures.add(executor.submit(() -> {
					long ops = 0;
					while (!Thread.currentThread().isInterrupted()) {
						if (client.read(CraqConsistencyModel.STRONG, 0) != null)
							ops++;
					}
					return ops;
				}));
			}
			Thread.sleep(ms);
			executor.shutdownNow();
			long totalTime = System.nanoTime() - startTime;

			// aggregate results
			long ops = 0;
			for (Future<Long> future : futures)
				ops += future.get();
			long opsPerSecond = Math.round(ops / (totalTime * 1e-9));
			if (run < WARMUP_TRIALS) {
				logger.info("benchmarkRead(): ({}/{}) running warm-up trial...", run + 1, WARMUP_TRIALS);
				continue;
			}
			throughputs.add(opsPerSecond);
			logger.info(
				"benchmarkRead(): ({}/{}) {} ops over {}ns using {} clients ({} ops/sec)",
				run + 1 - WARMUP_TRIALS, trials, ops, totalTime, numClients, opsPerSecond
			);
		}
		Collections.sort(throughputs);
		logger.info(
			"benchmarkRead(): [avg] {} ops/sec, [1st] {} ops/sec, [med] {} ops/sec, [99th] {} ops/sec",
			String.format("%.2f", throughputs.stream().mapToLong(Long::longValue).average().getAsDouble()),
			getPercentile(throughputs, 1),
			getPercentile(throughputs, 50),
			getPercentile(throughputs, 99)
		);

		// clean up
		for (CraqClient client : clients)
			client.close();
	}

	@TestMethod(desc="Benchmarks write operations.", minArgs=4,
	            params="<num_clients> <size_bytes> <milliseconds> <trials>")
	public static void benchmarkWrite(String host, int port, String[] args)
		throws TTransportException, InterruptedException, ExecutionException {
		int numClients = Integer.parseInt(args[0]);
		int numBytes = Integer.parseInt(args[1]);
		int ms = Integer.parseInt(args[2]);
		int trials = Integer.parseInt(args[3]);

		// connect to servers
		CraqClient[] clients = new CraqClient[numClients];
		for (int i = 0; i < numClients; i++) {
			clients[i] = new CraqClient(host, port);
			clients[i].connect();
		}

		// run trials...
		List<Long> throughputs = new ArrayList<>(trials);
		for (int run = 0; run < trials + WARMUP_TRIALS; run++) {
			// begin execution
			ExecutorService executor = Executors.newFixedThreadPool(numClients);
			List<Future<Long>> futures = new ArrayList<>(numClients);
			long startTime = System.nanoTime();
			for (int i = 0; i < numClients; i++) {
				CraqClient client = clients[i];
				futures.add(executor.submit(() -> {
					long ops = 0;
					while (!Thread.currentThread().isInterrupted()) {
						if (client.write(getRandomString(numBytes)) >= 0)
							ops++;
					}
					return ops;
				}));
			}
			Thread.sleep(ms);
			executor.shutdownNow();
			long totalTime = System.nanoTime() - startTime;

			// aggregate results
			long ops = 0;
			for (Future<Long> future : futures)
				ops += future.get();
			long opsPerSecond = Math.round(ops / (totalTime * 1e-9));
			if (run < WARMUP_TRIALS) {
				logger.info("benchmarkWrite(): ({}/{}) running warm-up trial...", run + 1, WARMUP_TRIALS);
				continue;
			}
			throughputs.add(opsPerSecond);
			logger.info(
				"benchmarkWrite(): ({}/{}) {} ops over {}ns using {} clients ({} ops/sec)",
				run + 1 - WARMUP_TRIALS, trials, ops, totalTime, numClients, opsPerSecond
			);
		}
		Collections.sort(throughputs);
		logger.info(
			"benchmarkWrite(): [avg] {} ops/sec, [1st] {} ops/sec, [med] {} ops/sec, [99th] {} ops/sec",
			String.format("%.2f", throughputs.stream().mapToLong(Long::longValue).average().getAsDouble()),
			getPercentile(throughputs, 1),
			getPercentile(throughputs, 50),
			getPercentile(throughputs, 99)
		);

		// clean up
		for (CraqClient client : clients)
			client.close();
	}

	@TestMethod(desc="Benchmarks test-and-set operations.", minArgs=3,
	            params="<size_bytes> <milliseconds> <trials>")
	public static void benchmarkTestAndSet(String host, int port, String[] args)
		throws TException, InterruptedException, ExecutionException {
		int numBytes = Integer.parseInt(args[0]);
		int ms = Integer.parseInt(args[1]);
		int trials = Integer.parseInt(args[2]);

		// connect to servers
		CraqClient client = new CraqClient(host, port);
		client.connect();

		// run trials...
		List<Long> throughputs = new ArrayList<>(trials);
		for (int run = 0; run < trials + WARMUP_TRIALS; run++) {
			// write initial object (to get the current version)
			long newVersion = client.write(getRandomString(numBytes));
			if (newVersion < 0) {
				logger.info("benchmarkTestAndSet(): failed to write a {}-byte object.", numBytes);
				return;
			}

			// begin execution
			ExecutorService executor = Executors.newFixedThreadPool(1);
			long startTime = System.nanoTime();
			Future<Long> future = executor.submit(() -> {
				long ops = 0, version = newVersion;
				while (!Thread.currentThread().isInterrupted()) {
					long v = client.testAndSet(getRandomString(numBytes), version);
					if (v >= 0) {
						ops++;
						version = v;
					}
				}
				return ops;
			});
			Thread.sleep(ms);
			executor.shutdownNow();
			long totalTime = System.nanoTime() - startTime;

			// aggregate results
			long ops = future.get();
			long opsPerSecond = Math.round(ops / (totalTime * 1e-9));
			if (run < WARMUP_TRIALS) {
				logger.info("benchmarkTestAndSet(): ({}/{}) running warm-up trial...", run + 1, WARMUP_TRIALS);
				continue;
			}
			throughputs.add(opsPerSecond);
			logger.info(
				"benchmarkTestAndSet(): ({}/{}) {} ops over {}ns ({} ops/sec)",
				run + 1 - WARMUP_TRIALS, ops, totalTime, opsPerSecond
			);
		}
		Collections.sort(throughputs);
		logger.info(
			"benchmarkTestAndSet(): [avg] {} ops/sec, [1st] {} ops/sec, [med] {} ops/sec, [99th] {} ops/sec",
			String.format("%.2f", throughputs.stream().mapToLong(Long::longValue).average().getAsDouble()),
			getPercentile(throughputs, 1),
			getPercentile(throughputs, 50),
			getPercentile(throughputs, 99)
		);

		// clean up
		client.close();
	}

	@TestMethod(desc="Benchmarks read operations as write rate increases.", minArgs=8,
	            params="<num_readers> <num_writers> <size_bytes> " +
	                   "<min_writes_sec> <max_writes_sec> <rate_step> " +
	                   "<milliseconds> [<read_ip>:<read_port> ...]")
	/** Benchmarks read operations as write rate increases.  */
	public static void benchmarkReadWrite(String host, int port, String[] args)
		throws TException, InterruptedException, ExecutionException {
		int numReaders = Integer.parseInt(args[0]);
		int numWriters = Integer.parseInt(args[1]);
		int numBytes = Integer.parseInt(args[2]);
		int minWriteRate = Integer.parseInt(args[3]);
		int maxWriteRate = Integer.parseInt(args[4]);
		int rateStep = Integer.parseInt(args[5]);
		int ms = Integer.parseInt(args[6]);
		int numReadServers = args.length - 7;
		String[] hosts = new String[numReadServers];
		int[] ports = new int[numReadServers];
		for (int i = 0; i < numReadServers; i++) {
			String[] s = args[i + 7].split(":");
			hosts[i] = s[0];
			ports[i] = Integer.parseInt(s[1]);
		}

		// connect to servers
		CraqClient[] writers = new CraqClient[numWriters];
		for (int i = 0; i < numWriters; i++) {
			writers[i] = new CraqClient(host, port);
			writers[i].connect();
		}
		CraqClient[] readers = new CraqClient[numReaders];
		for (int i = 0; i < numReaders; i++) {
			int readServerIndex = i % numReadServers;
			readers[i] = new CraqClient(hosts[readServerIndex], ports[readServerIndex]);
			readers[i].connect();
		}

		// write initial object
		long newVersion = writers[0].write(getRandomString(numBytes));
		logger.info(
			"benchmarkReadWrite(): wrote initial {}-byte object ({})",
			numBytes, newVersion >= 0 ? "version " + newVersion : "FAIL"
		);
		if (newVersion < 0)
			return;

		// for each write rate...
		logger.info("benchmarkReadWrite(): using {} readers and {} writers...", numReaders, numWriters);
		for (int k = minWriteRate; k <= maxWriteRate; k += rateStep) {
			long writesNeeded = (long) k * ms / 1000 / numWriters;

			// begin execution
			ExecutorService executor = Executors.newFixedThreadPool(numWriters + numReaders);
			List<Future<Long>> writeFutures = new ArrayList<>(numWriters);
			List<Future<ReadStats>> readFutures = new ArrayList<>(numReaders);
			long startTime = System.nanoTime();
			for (int i = 0; i < numWriters; i++) {
				CraqClient writer = writers[i];
				writeFutures.add(executor.submit(() -> {
					long writes = 0;
					while (writes < writesNeeded && !Thread.currentThread().isInterrupted()) {
						if (writer.write(getRandomString(numBytes)) >= 0)
							writes++;
					}
					return writes;
				}));
			}
			for (int i = 0; i < numReaders; i++) {
				CraqClient reader = readers[i];
				readFutures.add(executor.submit(() -> {
					long reads = 0, clean = 0, dirty = 0;
					while (!Thread.currentThread().isInterrupted()) {
						ReadObject obj = reader.read(CraqConsistencyModel.STRONG, 0);
						if (obj != null) {
							reads++;
							if (obj.dirty != null) {
								if (obj.dirty)
									dirty++;
								else
									clean++;
							}
						}
					}
					return new ReadStats(reads, clean, dirty);
				}));
			}
			Thread.sleep(ms);
			executor.shutdownNow();
			long totalTime = System.nanoTime() - startTime;

			// aggregate results
			long writes = 0, reads = 0, clean = 0, dirty = 0;
			for (Future<Long> writeFuture : writeFutures)
				writes += writeFuture.get();
			for (Future<ReadStats> readFuture : readFutures) {
				ReadStats stats = readFuture.get();
				reads += stats.getTotalReads();
				clean += stats.getCleanReads();
				dirty += stats.getDirtyReads();
			}
			long writesPerSecond = Math.round(writes / (totalTime * 1e-9));
			long readsPerSecond = Math.round(reads / (totalTime * 1e-9));
			long cleanReadsPerSecond = Math.round(clean / (totalTime * 1e-9));
			long dirtyReadsPerSecond = Math.round(dirty / (totalTime * 1e-9));
			logger.info(
				"benchmarkReadWrite(): {} reads over {}ns ({} reads/sec [{} clean, {} dirty], {} writes/sec [target: {}])",
				reads, totalTime, readsPerSecond, cleanReadsPerSecond, dirtyReadsPerSecond, writesPerSecond, k
			);

			System.gc();
			Thread.sleep(200);
		}

		// clean up
		for (CraqClient writer : writers)
			writer.close();
		for (CraqClient reader : readers)
			reader.close();
	}

	@TestMethod(desc="Benchmarks latency of clean/dirty reads and writes under configurable load.", minArgs=3,
	            params="<size_bytes> <milliseconds> <num_busy_readers> {<busy_read_ip>:<busy_read_port> ...}")
	/** Benchmarks read operations as write rate increases.  */
	public static void benchmarkLatency(String host, int port, String[] args)
		throws TException, InterruptedException, ExecutionException {
		int numBytes = Integer.parseInt(args[0]);
		int ms = Integer.parseInt(args[1]);
		int numBusyReaders = Integer.parseInt(args[2]);
		int numBusyReadServers = args.length - 3;
		if (numBusyReaders > 0 && numBusyReadServers < 1) {
			logger.info("benchmarkLatency(): no busy read servers specified.");
			return;
		}
		String[] hosts = new String[numBusyReadServers];
		int[] ports = new int[numBusyReadServers];
		for (int i = 0; i < numBusyReadServers; i++) {
			String[] s = args[i + 3].split(":");
			hosts[i] = s[0];
			ports[i] = Integer.parseInt(s[1]);
		}

		// connect to servers
		CraqClient client = new CraqClient(host, port);
		client.connect();
		CraqClient[] readers = new CraqClient[numBusyReaders];
		for (int i = 0; i < numBusyReaders; i++) {
			int readServerIndex = i % numBusyReadServers;
			readers[i] = new CraqClient(hosts[readServerIndex], ports[readServerIndex]);
			readers[i].connect();
		}

		// write initial object
		long newVersion = client.write(getRandomString(numBytes));
		logger.info(
			"benchmarkLatency(): wrote initial {}-byte object ({})",
			numBytes, newVersion >= 0 ? "version " + newVersion : "FAIL"
		);
		if (newVersion < 0)
			return;

		// begin busy reading
		ExecutorService busyExecutor = null;
		if (numBusyReaders > 0) {
			logger.info("benchmarkLatency(): spawning {} busy readers...", numBusyReaders);
			busyExecutor = Executors.newFixedThreadPool(numBusyReaders);
			for (int i = 0; i < numBusyReaders; i++) {
				CraqClient reader = readers[i];
				busyExecutor.execute(() -> {
					try {
						while (!Thread.currentThread().isInterrupted())
							reader.read(CraqConsistencyModel.STRONG, 0);
					} catch (Exception e) {
						if (!Thread.currentThread().isInterrupted())
							logger.error("Error while busy reading!", e);
					}
				});
			}
			Thread.sleep(500);  // wait before benchmarking
		}

		// start benchmarking
		benchmarkLatency("clean reads", client, ms, () -> {
			List<Long> latencies = new ArrayList<Long>();
			while (!Thread.currentThread().isInterrupted()) {
				long start = System.nanoTime();
				ReadObject obj = client.read(CraqConsistencyModel.STRONG, 0);
				long end = System.nanoTime();
				if (obj != null && obj.dirty != null && !obj.dirty)
					latencies.add(end - start);
			}
			return latencies;
		});
		benchmarkLatency("dirty reads", client, ms, () -> {
			List<Long> latencies = new ArrayList<Long>();
			while (!Thread.currentThread().isInterrupted()) {
				long start = System.nanoTime();
				ReadObject obj = client.read(CraqConsistencyModel.DEBUG, 0);
				long end = System.nanoTime();
				if (obj != null && obj.dirty != null && obj.dirty)
					latencies.add(end - start);
			}
			return latencies;
		});
		benchmarkLatency("writes", client, ms, () -> {
			List<Long> latencies = new ArrayList<Long>();
			while (!Thread.currentThread().isInterrupted()) {
				long start = System.nanoTime();
				long version = client.write(getRandomString(numBytes));
				long end = System.nanoTime();
				if (version >= 0)
					latencies.add(end - start);
			}
			return latencies;
		});

		// clean up
		if (busyExecutor != null)
			busyExecutor.shutdownNow();
		client.close();
		for (CraqClient reader : readers)
			reader.close();
	}

	/** Runs a latency benchmark using the given task. */
	private static void benchmarkLatency(String desc, CraqClient client, int ms, Callable<List<Long>> task)
		throws InterruptedException, ExecutionException {
		// begin execution
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<List<Long>> future = executor.submit(task);
		Thread.sleep(ms);
		executor.shutdownNow();

		// aggregate results
		List<Long> latencies = future.get();
		Collections.sort(latencies);
		logger.info(
			"benchmarkLatency(): performed {} {} over {}ms ([avg] {}ms, [med] {}ms, [95th] {}ms, [99th] {}ms)",
			latencies.size(), desc, ms,
			String.format("%.3f", latencies.stream().mapToLong(Long::longValue).average().getAsDouble() * 1e-6),
			String.format("%.3f", getPercentile(latencies, 50) * 1e-6),
			String.format("%.3f", getPercentile(latencies, 95) * 1e-6),
			String.format("%.3f", getPercentile(latencies, 99) * 1e-6)
		);
	}

	/** Returns a random string with the given number of bytes. */
	private static String getRandomString(int numBytes) {
		byte[] b = new byte[numBytes];
		RANDOM.nextBytes(b);
		return new String(b, StandardCharsets.UTF_8);
	}

	/** Returns a value at the given percentile from the sorted list. */
	private static <T> T getPercentile(List<T> list, int percentile) {
		int index = (int) Math.ceil((percentile / 100f) * list.size()) - 1;
		return list.get(index);
	}

	/** Prints the list of invokable methods. */
	private static void printAvailableMethods() {
		List<Method> methods = new ArrayList<>();
		for (Method method : TestClient.class.getMethods()) {
			int modifiers = method.getModifiers();
			if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && !method.getName().equals("main"))
				methods.add(method);
		}
		Collections.sort(methods, (m1, m2) -> m1.getName().compareTo(m2.getName()));
		System.out.printf("------------------------\n");
		System.out.printf("Available client methods\n");
		System.out.printf("------------------------\n");
		for (Method method : methods) {
			TestMethod ann = method.getAnnotation(TestMethod.class);
			System.out.printf("* %s %s\n", method.getName(), ann == null ? "" : ann.params());
			if (ann != null && !ann.desc().isEmpty())
				System.out.printf("    %s\n", ann.desc());
		}
	}

	/** Runs a test. */
	public static void main(String[] args) {
		// parse CLI arguments
		String[] s = args[0].split(":");
		String host = s[0];
		int port = Integer.parseInt(s[1]);
		String testName = args[1];
		String[] testArgs = Arrays.copyOfRange(args, 2, args.length);

		// run the test
		try {
			Method method = TestClient.class.getDeclaredMethod(testName, String.class, int.class, String[].class);
			TestMethod ann = method.getAnnotation(TestMethod.class);
			if (ann != null && testArgs.length < ann.minArgs())
				System.out.printf("%s() arguments:\n    %s\n", method.getName(), ann.params());
			else
				method.invoke(null, host, port, testArgs);
		} catch (InvocationTargetException e) {
			logger.error("An error occurred during test execution.", e.getTargetException());
		} catch (Exception e) {
			printAvailableMethods();
		}
	}
}
