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

	/** Runs the client. */
	public static void main(String[] args) throws TException {
		String host = (args.length < 1) ? "localhost" : args[0];
		int port = (args.length < 2) ? 9090 : Integer.parseInt(args[1]);

		// connect to the sever node
		TTransport transport = new TSocket(host, port);
		transport.open();
		TProtocol protocol = new TBinaryProtocol(transport);
		CraqService.Client client = new CraqService.Client(protocol);
		logger.info("Connected to server at {}:{}", host, port);

		// write something
		String wValue = "test_12345";
		ByteBuffer wBuf = ByteBuffer.wrap(wValue.getBytes(StandardCharsets.UTF_8));
		CraqObject wObj = new CraqObject();
		wObj.setValue(wBuf);
		boolean written = client.write(wObj);
		logger.info("Wrote object {}: {}", wValue, written ? "SUCCESS" : "FAIL");

		// read it back
		CraqObject rObj = client.read(CraqConsistencyModel.EVENTUAL);
		String rValue = null;
		if (rObj.isSetValue()) {
			byte[] rArr = new byte[rObj.value.remaining()];
			rObj.value.get(rArr);
			rValue = new String(rArr, StandardCharsets.UTF_8);
		}
		logger.info("Read object: {}", rValue);

		transport.close();
	}
}
