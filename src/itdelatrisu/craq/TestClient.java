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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

	/** Basic write operation. */
	public static void write(String host, int port, String[] args) throws TException {
		if (args.length < 1) {
			System.out.printf("write() arguments:\n    <value>\n");
			System.exit(1);
		}
		String value = args[0];

		CraqClient client = new CraqClient(host, port);
		client.connect();
		boolean status = client.write(value);
		logger.info("write(): writing object {} ({})", value, status ? "SUCCESS" : "FAIL");
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
		boolean status = client.write(value);
		logger.info("writeBytes(): writing {}-byte object ({})", numBytes, status ? "SUCCESS" : "FAIL");
		client.close();
	}

	/** Returns a random string with the given number of bytes. */
	private static String getRandomString(int numBytes) {
		byte[] b = new byte[numBytes];
		RANDOM.nextBytes(b);
		return new String(b, StandardCharsets.UTF_8);
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
		int versionBound = Integer.parseInt(args[0]);

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
					client.read(CraqConsistencyModel.STRONG, 0);
					ops++;
				}
				return ops;
			}));
		}
		executor.awaitTermination(ms, TimeUnit.MILLISECONDS);
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
					client.write(value);
					ops++;
				}
				return ops;
			}));
		}
		executor.awaitTermination(ms, TimeUnit.MILLISECONDS);
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
		for (CraqClient client : clients)
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
		boolean status = writers[0].write(value);
		logger.info(
			"benchmarkReadWrite(): wrote initial {}-byte object ({})",
			numBytes, status ? "SUCCESS" : "FAIL"
		);

		// for each write rate...
		logger.info("benchmarkReadWrite(): using {} readers and {} writers...", numReaders, numWriters);
		for (int k = minWriteRate; k <= maxWriteRate; k += rateStep) {
			long writesNeeded = (long) k * ms / 1000 / numWriters;

			// begin execution
			ExecutorService executor = Executors.newFixedThreadPool(numWriters + numReaders);
			List<Future<Long>>
				readFutures = new ArrayList<>(numReaders),
				writeFutures = new ArrayList<>(numWriters);
			long startTime = System.nanoTime();
			for (int i = 0; i < numWriters; i++) {
				CraqClient writer = writers[i];
				writeFutures.add(executor.submit(() -> {
					long writes = 0;
					while (writes < writesNeeded && !Thread.currentThread().isInterrupted()) {
						writer.write(value);
						writes++;
					}
					return writes;
				}));
			}
			for (int i = 0; i < numReaders; i++) {
				CraqClient reader = readers[i];
				readFutures.add(executor.submit(() -> {
					long reads = 0;
					while (!Thread.currentThread().isInterrupted()) {
						reader.read(CraqConsistencyModel.STRONG, 0);
						reads++;
					}
					return reads;
				}));
			}
			executor.awaitTermination(ms, TimeUnit.MILLISECONDS);
			executor.shutdownNow();
			long totalTime = System.nanoTime() - startTime;

			// aggregate results
			long writes = 0, reads = 0;
			for (Future<Long> writeFuture : writeFutures)
				writes += writeFuture.get();
			for (Future<Long> readFuture : readFutures)
				reads += readFuture.get();
			long readsPerSecond = Math.round(reads / (totalTime * 1e-9));
			long writesPerSecond = Math.round(writes / (totalTime * 1e-9));
			logger.info(
				"benchmarkReadWrite(): {} reads over {}ns ({} reads/sec, {} writes/sec [target: {}])",
				reads, totalTime, readsPerSecond, writesPerSecond, k
			);

			Thread.sleep(200);
		}

		for (CraqClient writer : writers)
			writer.close();
		for (CraqClient reader : readers)
			reader.close();
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
