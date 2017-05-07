package itdelatrisu.craq;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import itdelatrisu.craq.thrift.CraqConsistencyModel;

/** CRAQ client tests. */
public class TestClient {
	private static final Logger logger = LoggerFactory.getLogger(TestClient.class);

	/** Basic write operation. */
	public static void write(String host, int port, String[] args) throws TException {
		if (args.length < 1) {
			System.err.printf("write() arguments: <value>");
			System.exit(1);
		}
		String value = args[0];

		CraqClient client = new CraqClient(host, port);
		client.connect();
		boolean status = client.write(value);
		logger.info("write(): writing object {} ({})", value, status ? "SUCCESS" : "FAIL");
		client.close();
	}

	/** Basic read operation (strong consistency). */
	public static void readStrong(String host, int port, String[] args) throws TException {
		CraqClient client = new CraqClient(host, port);
		client.connect();
		String value = client.read(CraqConsistencyModel.STRONG, 0);
		logger.info("readStrong(): read object {}", value);
		client.close();
	}

	/** Basic read operation (eventual consistency). */
	public static void readEventual(String host, int port, String[] args) throws TException {
		CraqClient client = new CraqClient(host, port);
		client.connect();
		String value = client.read(CraqConsistencyModel.EVENTUAL, 0);
		logger.info("readEventual(): read object {}", value);
		client.close();
	}

	/** Basic read operation (eventual bounded consistency). */
	public static void readEventualBounded(String host, int port, String[] args) throws TException {
		if (args.length < 1) {
			System.err.printf("readEventualBounded() arguments: <versionBound>");
			System.exit(1);
		}
		int versionBound = Integer.parseInt(args[0]);

		CraqClient client = new CraqClient(host, port);
		client.connect();
		String value = client.read(CraqConsistencyModel.EVENTUAL_BOUNDED, versionBound);
		logger.info("readStrong(): read object {}", value);
		client.close();
	}

	/** Prints the list of invokable methods. */
	private static void printAvailableMethods() {
		System.out.printf("Available client methods:\n");
		for (Method method : TestClient.class.getMethods()) {
			int modifiers = method.getModifiers();
			if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && !method.getName().equals("main"))
				System.out.printf("    %s\n", method.getName());
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
			method.invoke(null, host, port, testArgs);
		} catch (InvocationTargetException e) {
			logger.error("An error occurred during test execution.", e.getTargetException());
		} catch (Exception e) {
			printAvailableMethods();
		}
	}
}
