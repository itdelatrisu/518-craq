package itdelatrisu.craq;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CRAQ server node. */
public class CraqNode {
	private static final Logger logger = LoggerFactory.getLogger(CraqNode.class);

	/** Creates a new CRAQ node. */
	public CraqNode() {
		// TODO
	}

	/** Starts the server. */
	public void start(int port) {
		// TODO this is blocking, should be the last thing we call?
		try {
			runServer(port);
		} catch (TTransportException e) {
			logger.error("Failed to start Thrift server.", e);
			return;
		}

		// TODO test code
		try {
			connectToClient("localhost", port);
		} catch (TException e) {
			logger.error("Failed to connect to Thrift client.", e);
		}
	}

	/** Starts the Thrift server. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void runServer(int port) throws TTransportException {
		TServerTransport serverTransport = new TServerSocket(port);
		CraqService.Processor processor = new CraqService.Processor(new CraqServiceImpl());

		TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
		logger.info("Starting Thrift server on port {}...", port);
		server.serve();
	}

	/** Connects to the Thrift clients. */
	private void connectToClient(String host, int port) throws TException {
		// TODO figure out how to use Zookeeper, this is placeholder code
		TTransport transport = new TSocket(host, port);
		transport.open();

		TProtocol protocol = new TBinaryProtocol(transport);
		CraqService.Client client = new CraqService.Client(protocol);

		logger.info("100 + 200 = {}", client.add(100, 200));

		transport.close();
	}
}
