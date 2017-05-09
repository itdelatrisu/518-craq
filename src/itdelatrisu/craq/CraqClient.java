package itdelatrisu.craq;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import itdelatrisu.craq.thrift.CraqConsistencyModel;
import itdelatrisu.craq.thrift.CraqObject;
import itdelatrisu.craq.thrift.CraqService;

/** CRAQ client. */
public class CraqClient {
	private static final Logger logger = LoggerFactory.getLogger(CraqClient.class);

	/** The server host. */
	private final String host;

	/** The server port. */
	private final int port;

	/** The Thrift client connections. */
	private CraqService.Client server;

	/** The Thrift transport stream. */
	private TTransport transport;

	/** Creates a new CRAQ client. */
	public CraqClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	/** Connects to the CRAQ server node. */
	public void connect() throws TTransportException {
		this.transport = new TFramedTransport(new TSocket(host, port));
		transport.open();

		TProtocol protocol = new TBinaryProtocol(transport);
		CraqService.Client client = new CraqService.Client(protocol);

		this.server = client;
		logger.debug("Connected to server at {}:{}", host, port);
	}

	/** Writes an object. */
	public boolean write(String value) throws TException {
		ByteBuffer buf = ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
		CraqObject obj = new CraqObject();
		obj.setValue(buf);
		return server.write(obj);
	}

	/** Reads an object. */
	public String read(CraqConsistencyModel model, int versionBound) throws TException {
		CraqObject obj = server.read(model, versionBound);
		String value = null;
		if (obj.isSetValue()) {
			byte[] bytes = new byte[obj.value.remaining()];
			obj.value.get(bytes);
			value = new String(bytes, StandardCharsets.UTF_8);
		}
		return value;
	}

	/** Closes the server connection. */
	public synchronized void close() {
		if (server != null && transport != null) {
			transport.close();
			this.server = null;
			this.transport = null;
		}
	}
}
