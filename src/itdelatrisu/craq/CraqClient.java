package itdelatrisu.craq;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import itdelatrisu.craq.thrift.CraqConsistencyModel;
import itdelatrisu.craq.thrift.CraqObject;
import itdelatrisu.craq.thrift.CraqService;

/** CRAQ client. */
public class CraqClient {
	private static final Logger logger = LoggerFactory.getLogger(CraqClient.class);

	/** Writes an object. */
	private static boolean testWrite(CraqService.Client client, String wValue) throws TException {
		ByteBuffer wBuf = ByteBuffer.wrap(wValue.getBytes(StandardCharsets.UTF_8));
		CraqObject wObj = new CraqObject();
		wObj.setValue(wBuf);
		return client.write(wObj);
	}

	/** Reads an object. */
	private static String testRead(CraqService.Client client, CraqConsistencyModel model, int versionBound) throws TException {
		CraqObject rObj = client.read(CraqConsistencyModel.STRONG, versionBound);
		String rValue = null;
		if (rObj.isSetValue()) {
			byte[] rArr = new byte[rObj.value.remaining()];
			rObj.value.get(rArr);
			rValue = new String(rArr, StandardCharsets.UTF_8);
		}
		return rValue;
	}

	private static void testStrong(CraqService.Client client, CraqConsistencyModel model) throws TException {
		// basic write and read
		logger.info("Test strong consistency");
		// write something
		String wValue = "asdfasdf";
		logger.info("Wrote object {}: {}", wValue, testWrite(client, wValue) ? "SUCCESS" : "FAIL");
		// read it back
		logger.info("Read object: {}: ", testRead(client, model, 0).equals(wValue) ? "SUCCESS" : "FAIL");
	}

	private static void testEventual(CraqService.Client client, CraqConsistencyModel model) throws TException {
		// basic write and read
		logger.info("Test eventual consistency");
		// write something
		String wValue = "asdfasdf";
		logger.info("Wrote object {}: {}", wValue, testWrite(client, wValue) ? "SUCCESS" : "FAIL");
		// read it back
		logger.info("Read object: {}: ", testRead(client, model, 0).equals("asdf") ? "SUCCESS" : "FAIL");
	}

	private static void testEventualBounded(CraqService.Client client, CraqConsistencyModel model) throws TException {
		// basic write and read
		logger.info("Test eventual bounded consistency");
		// write something
		String wValue = "asdfasdf";
		logger.info("Wrote object {}: {}", wValue, testWrite(client, wValue) ? "SUCCESS" : "FAIL");
		// read it back
		String read = testRead(client, model, 0);
		// TODO change test case
		logger.info("Read object {}: {} ", read, read.equals(wValue) ? "SUCCESS" : "FAIL");
	}

	/** Runs the client. */
	public static void main(String[] args) throws TException {
		String host = "localhost";
		int port = 8080;
		if (args.length >= 1) {
			String[] s = args[0].split(":");
			host = s[0];
			port = Integer.parseInt(s[1]);
		}
		String function = (args.length < 3) ? "readEventualBounded" : args[1];

		CraqConsistencyModel strong = CraqConsistencyModel.STRONG;
		CraqConsistencyModel eventual = CraqConsistencyModel.EVENTUAL;
		CraqConsistencyModel eventual_bounded = CraqConsistencyModel.EVENTUAL_BOUNDED;

		// connect to the sever node
		TTransport transport = new TSocket(host, port);
		transport.open();
		TProtocol protocol = new TBinaryProtocol(transport);
		CraqService.Client client = new CraqService.Client(protocol);
		logger.info("Connected to server at {}:{}", host, port);

		switch (function) {
		case "write":
			testWrite(client, "asdf");
			break;
		case "readStrong":
			testStrong(client, strong);
			break;
		case "readEventual":
			testEventual(client, eventual);
			break;
		case "readEventualBounded":
			testEventualBounded(client, eventual_bounded);
			break;
		}

		transport.close();
	}
}
