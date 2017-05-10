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

	/** Basic write operation. */
	public static void write(String host, int port, String[] args) throws TException {
		if (args.length < 1) {
			System.out.printf("write() arguments:\n    <value>\n");
			System.exit(1);
		}
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

	/** Basic write operation. */
	public static void writeBytes(String host, int port, String[] args) throws TException {
		if (args.length < 1) {
			System.out.printf("writeBytes() arguments:\n    <size_bytes>\n");
			System.exit(1);
		}
		int numBytes = Integer.parseInt(args[0]);
		String value = getRandomString(numBytes);

		CraqClient client = new CraqClient(host, port);
		client.connect();
		long newVersion = client.write(value);
		logger.info(
			"writeBytes(): writing {}-byte object ({})",
			numBytes, newVersion >= 0 ? "version " + newVersion : "FAIL"
		);
		client.close();
	}

	/** Basic test-and-set operation. */
	public static void testAndSet(String host, int port, String[] args) throws TException {
		if (args.length < 2) {
			System.out.printf("testAndSet() arguments:\n    <value> <expectedVersion>\n");
			System.exit(1);
		}
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

	/** Basic read operation (strong consistency). */
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

	/** Basic read operation (eventual consistency). */
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

	/** Basic read operation (eventual bounded consistency). */
	public static void readEventualBounded(String host, int port, String[] args) throws TException {
		if (args.length < 1) {
			System.out.printf("readEventualBounded() arguments:\n    <version_bound>\n");
			System.exit(1);
		}
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

	/** Benchmarks read operations. */
	public static void benchmarkRead(String host, int port, String[] args)
		throws TTransportException, InterruptedException, ExecutionException {
		if (args.length < 2) {
			System.out.printf("benchmarkRead() arguments:\n    <num_clients> <milliseconds> {<additional_ip>:<additional_port> ...}\n");
			System.exit(1);
		}
		int numClients = Integer.parseInt(args[0]);
		int ms = Integer.parseInt(args[1]);
		int numServers = 1 + args.length - 2;
		String[] hosts = new String[numServers];
		hosts[0] = host;
		int[] ports = new int[numServers];
		ports[0] = port;
		for (int i = 0; i < numServers - 1; i++) {
			String[] s = args[i + 2].split(":");
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
		logger.info(
			"benchmarkRead(): {} ops over {}ns using {} clients ({} ops/sec)",
			ops, totalTime, numClients, opsPerSecond
		);

		// clean up
		for (CraqClient client : clients)
			client.close();
	}

	/** Benchmarks write operations. */
	public static void benchmarkWrite(String host, int port, String[] args)
		throws TTransportException, InterruptedException, ExecutionException {
		if (args.length < 3) {
			System.out.printf("benchmarkWrite() arguments:\n    <num_clients> <size_bytes> <milliseconds>\n");
			System.exit(1);
		}
		int numClients = Integer.parseInt(args[0]);
		int numBytes = Integer.parseInt(args[1]);
		int ms = Integer.parseInt(args[2]);
		String value = getRandomString(numBytes);

		// connect to servers
		CraqClient[] clients = new CraqClient[numClients];
		for (int i = 0; i < numClients; i++) {
			clients[i] = new CraqClient(host, port);
			clients[i].connect();
		}

		// begin execution
		ExecutorService executor = Executors.newFixedThreadPool(numClients);
		List<Future<Long>> futures = new ArrayList<>(numClients);
		long startTime = System.nanoTime();
		for (int i = 0; i < numClients; i++) {
			CraqClient client = clients[i];
			futures.add(executor.submit(() -> {
				long ops = 0;
				while (!Thread.currentThread().isInterrupted()) {
					if (client.write(value) >= 0)
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
		logger.info(
			"benchmarkWrite(): {} ops over {}ns using {} clients ({} ops/sec)",
			ops, totalTime, numClients, opsPerSecond
		);

		// clean up
		for (CraqClient client : clients)
			client.close();
	}

	/** Benchmarks test-and-set operations. */
	public static void benchmarkTestAndSet(String host, int port, String[] args)
		throws TException, InterruptedException, ExecutionException {
		if (args.length < 2) {
			System.out.printf("benchmarkTestAndSet() arguments:\n    <size_bytes> <milliseconds>\n");
			System.exit(1);
		}
		int numBytes = Integer.parseInt(args[0]);
		int ms = Integer.parseInt(args[1]);
		String value = getRandomString(numBytes);

		// connect to servers
		CraqClient client = new CraqClient(host, port);
		client.connect();

		// write initial object (to get the current version)
		long newVersion = client.write(value);
		logger.info(
			"benchmarkTestAndSet(): wrote initial {}-byte object ({})",
			numBytes, newVersion >= 0 ? "version " + newVersion : "FAIL"
		);
		if (newVersion < 0)
			return;

		// begin execution
		ExecutorService executor = Executors.newFixedThreadPool(1);
		long startTime = System.nanoTime();
		Future<Long> future = executor.submit(() -> {
			long ops = 0, version = newVersion;
			while (!Thread.currentThread().isInterrupted()) {
				long v = client.testAndSet(value, version);
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
		logger.info(
			"benchmarkTestAndSet(): {} ops over {}ns ({} ops/sec)",
			ops, totalTime, opsPerSecond
		);

		// clean up
		client.close();
	}

	/** Benchmarks read operations as write rate increases.  */
	public static void benchmarkReadWrite(String host, int port, String[] args)
		throws TException, InterruptedException, ExecutionException {
		if (args.length < 8) {
			System.out.printf(
				"benchmarkReadWrite() arguments:\n    " +
				"<num_readers> <num_writers> <size_bytes> " +
				"<min_writes_sec> <max_writes_sec> <rate_step> " +
				"<milliseconds> [<read_ip>:<read_port> ...]\n"
			);
			System.exit(1);
		}
		int numReaders = Integer.parseInt(args[0]);
		int numWriters = Integer.parseInt(args[1]);
		int numBytes = Integer.parseInt(args[2]);
		int minWriteRate = Integer.parseInt(args[3]);
		int maxWriteRate = Integer.parseInt(args[4]);
		int rateStep = Integer.parseInt(args[5]);
		int ms = Integer.parseInt(args[6]);
		String value = getRandomString(numBytes);
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
		long newVersion = writers[0].write(value);
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
						if (writer.write(value) >= 0)
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

	/** Benchmarks read operations as write rate increases.  */
	public static void benchmarkLatency(String host, int port, String[] args)
		throws TException, InterruptedException, ExecutionException {
		if (args.length < 3 || (Integer.parseInt(args[2]) > 0 && args.length < 4)) {
			System.out.printf(
				"benchmarkLatency() arguments:\n    " +
				"<size_bytes> <milliseconds> <num_busy_readers> {<busy_read_ip>:<busy_read_port> ...}\n"
			);
			System.exit(1);
		}
		int numBytes = Integer.parseInt(args[0]);
		int ms = Integer.parseInt(args[1]);
		int numBusyReaders = Integer.parseInt(args[2]);
		String value = getRandomString(numBytes);
		int numBusyReadServers = args.length - 3;
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
		long newVersion = client.write(value);
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
			Thread.sleep(1000);  // wait before benchmarking
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
				long version = client.write(value);
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
		double avg = latencies.stream().mapToLong(Long::longValue).average().getAsDouble();
		logger.info(
			"benchmarkLatency(): performed {} {} over {}ms ({}ms avg. latency)",
			latencies.size(), desc, ms, String.format("%.3f", avg * 1e-6)
		);
	}

	/** Returns a random string with the given number of bytes. */
	private static String getRandomString(int numBytes) {
		byte[] b = new byte[numBytes];
		RANDOM.nextBytes(b);
		return new String(b, StandardCharsets.UTF_8);
	}

	/** Prints the list of invokable methods. */
	private static void printAvailableMethods() {
		List<String> methods = new ArrayList<>();
		for (Method method : TestClient.class.getMethods()) {
			int modifiers = method.getModifiers();
			if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && !method.getName().equals("main"))
				methods.add(method.getName());
		}
		Collections.sort(methods);
		System.out.printf("Available client methods:\n");
		for (String name : methods)
			System.out.printf("    %s\n", name);
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
			method.invoke(null, host, port, testArgs);
		} catch (InvocationTargetException e) {
			logger.error("An error occurred during test execution.", e.getTargetException());
		} catch (Exception e) {
			printAvailableMethods();
		}
	}
}
