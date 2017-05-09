package itdelatrisu.craq;

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

	/** Wrapper class for a read object. */
	public class ReadObject {
		/** The object's value. */
		public final String value;
		/** Whether the read was dirty (true) or clean (false). */
		public final Boolean dirty;
		/** Constructor. */
		public ReadObject(String value, Boolean dirty) {
			this.value = value;
			this.dirty = dirty;
		}
		@Override
		public String toString() {
			if (dirty == null)
				return String.format("%s", value);
			else
				return String.format("%s (%s)", value, dirty ? "DIRTY" : "CLEAN");
		}
	}

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

	/** Writes an object, returning success (true) or failure (false). */
	public boolean write(String value) throws TException {
		CraqObject obj = new CraqObject();
		obj.setValue(value.getBytes(StandardCharsets.UTF_8));
		return server.write(obj);
	}

	/** Reads an object, returning null if the read failed for any reason. */
	public ReadObject read(CraqConsistencyModel model, long versionBound) throws TException {
		CraqObject obj = server.read(model, versionBound);
		if (!obj.isSetValue())
			return null;
		return new ReadObject(
			new String(obj.getValue(), StandardCharsets.UTF_8),
			obj.isSetDirty() ? obj.isDirty() : null
		);
	}
	
	/** Reads an object, returning null if the read failed for any reason. */
	public boolean testAndSet(long requestVersion, String value) throws TException {
		CraqObject obj = new CraqObject();
		obj.setValue(value.getBytes(StandardCharsets.UTF_8));
		return server.testAndSet(requestVersion, obj);
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
