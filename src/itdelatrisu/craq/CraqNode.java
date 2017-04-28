package itdelatrisu.craq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

import itdelatrisu.craq.thrift.CraqConsistencyModel;
import itdelatrisu.craq.thrift.CraqObject;
import itdelatrisu.craq.thrift.CraqService;

/** CRAQ server node. */
public class CraqNode implements CraqService.Iface {
	private static final Logger logger = LoggerFactory.getLogger(CraqNode.class);

	/** Time to wait before retrying a node connection (in ms). */
	private static final int CONNECTION_SLEEP_TIME = 200;

	/** Whether this node is running in CR mode (not CRAQ). */
	private final boolean crMode;

	/** The client connections. */
	private CraqService.Client tail, successor;

	/** The entire chain. */
	private CraqChain chain;

	/** Current known objects: <version, bytes> */
	private final Map<Integer, CraqObject> objects = new ConcurrentHashMap<>();

	/** The latest known clean object version. */
	private int latestCleanVersion = -1;

	/** The latest known version (clean or dirty). */
	private int latestVersion = -1;

	/** Creates a new CRAQ node. */
	public CraqNode(boolean crMode, CraqChain chain) {
		this.crMode = crMode;
		this.chain = chain;
	}

	/** Starts the server. */
	public void start() {
		// connect to tail/successor
		new Thread() {
			@Override
			public void run() {
				try {
					connectToClients();
				} catch (InterruptedException e) {
					logger.error("Failed to connect to Thrift clients.", e);
				}
			}
		}.start();

		// run the server
		try {
			runServer(chain.getNode().port);
		} catch (TTransportException e) {
			logger.error("Failed to start Thrift server.", e);
			return;
		}
	}

	/** Starts the Thrift server. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void runServer(int port) throws TTransportException {
		TServerTransport serverTransport = new TServerSocket(port);
		CraqService.Processor processor = new CraqService.Processor(this);

		TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
		logger.info("Starting Thrift server on port {}...", port);
		server.serve();
	}

	/** Connects to the other nodes in the chain. */
	private void connectToClients() throws InterruptedException {
		// connect to tail
		if (chain.isTail())
			return;
		while (true) {
			try {
				this.tail = connectToClient(chain.getTail().host, chain.getTail().port);
				break;
			} catch (TTransportException e) {
				Thread.sleep(CONNECTION_SLEEP_TIME);
			}
		}
		logger.info("Connected to tail at {}:{}", chain.getTail().host, chain.getTail().port);

		// connect to successor
		if (chain.getIndex() == chain.size() - 2) {
			// is this the node before the tail?
			this.successor = this.tail;
			return;
		}
		while (true) {
			try {
				this.successor = connectToClient(chain.getSuccessor().host, chain.getSuccessor().port);
				break;
			} catch (TTransportException e) {
				Thread.sleep(CONNECTION_SLEEP_TIME);
			}
		}
		logger.info("Connected to successor at {}:{}", chain.getSuccessor().host, chain.getSuccessor().port);
	}

	/** Connects to the Thrift clients. */
	private CraqService.Client connectToClient(String host, int port) throws TTransportException {
		TTransport transport = new TSocket(host, port);
		transport.open();

		TProtocol protocol = new TBinaryProtocol(transport);
		CraqService.Client client = new CraqService.Client(protocol);

		return client;
	}

	@Override
	public CraqObject read(CraqConsistencyModel model) throws TException {
		logger.debug("Received read request from client...");

		// node hasn't initialized?
		if (!chain.isTail() && (tail == null || successor == null))
			return new CraqObject();

		// no objects stored?
		if (objects.isEmpty())
			return new CraqObject();

		// running normal CR: fail if we're not the tail
		if (crMode && !chain.isTail())
			return new CraqObject();

		// strong consistency?
		if (model == CraqConsistencyModel.STRONG) {
			if (latestVersion > latestCleanVersion) {
				// latest known version isn't clean, send a version query
				int tailVersion = tail.versionQuery();
				return objects.get(tailVersion);
			} else {
				// latest known version is clean, return it
				return objects.get(latestCleanVersion);
			}
		}

		// eventual consistency?
		else if (model == CraqConsistencyModel.EVENTUAL) {
			// return latest known version
			return objects.get(latestVersion);
		}

		// TODO bounded eventual consistency?

		logger.error("!! read() shouldn't get here !!");
		return new CraqObject();
	}

	@Override
	public boolean write(CraqObject obj) throws TException {
		logger.debug("Received write request from client...");

		// node hasn't initialized?
		if (tail == null || successor == null)
			return false;

		// can only write to head
		if (!chain.isHead())
			return false;

		// record new object version
		int newVersion;
		synchronized (this) {
			newVersion = latestVersion + 1;
			objects.put(newVersion, obj);
			latestVersion = newVersion;
		}

		// send down chain
		successor.writeVersioned(obj, newVersion);

		// update clean version
		synchronized (this) {
			if (newVersion > latestCleanVersion) {
				latestCleanVersion = newVersion;
				removeOldVersions(latestCleanVersion);
			}
		}

		return true;
	}

	@Override
	public void writeVersioned(CraqObject obj, int version) throws TException {
		logger.debug("Received write with version: {}", version);

		// add new object version
		objects.put(version, obj);

		// update latest version
		synchronized (this) {
			if (version > latestVersion)
				latestVersion = version;

			// tail: mark clean
			if (chain.isTail()) {
				if (version > latestCleanVersion) {
					latestCleanVersion = version;
					removeOldVersions(latestCleanVersion);
				}
			}
		}

		// non-tail: send down chain
		if (!chain.isTail())
			successor.writeVersioned(obj, version);
	}

	/** Removes all object versions older than the latest clean one. */
	private void removeOldVersions(int latestCleanVersion) {
		for (Iterator<Map.Entry<Integer, CraqObject>> iter = objects.entrySet().iterator(); iter.hasNext();) {
			Map.Entry<Integer, CraqObject> entry = iter.next();
			if (entry.getKey() < latestCleanVersion)
				iter.remove();
		}
	}

	@Override
	public int versionQuery() throws TException {
		logger.debug("Received version query...");

		// only tail should receive version queries
		if (!chain.isTail())
			return -1;

		// return latest clean version
		return latestCleanVersion;
	}

	@Override
	public boolean testAndSet(CraqObject obj, CraqObject objExpected) throws TException {
		// TODO
		return true;
	}

	/** Starts the CRAQ server node. */
	public static void main(String[] args) {
		// parse CLI arguments
		if (args.length < 4) {
			System.out.printf("arguments: <is_cr_mode> <node_index> [<first_ip>:<first_port> ... <last_ip>:<last_port>]");
			System.exit(1);
		}
		boolean crMode = Integer.parseInt(args[0]) == 1;
		int nodeIndex = Integer.parseInt(args[1]);
		List<CraqChain.ChainNode> chainNodes = new ArrayList<>();
		for (int i = 2; i < args.length; i++) {
			String[] s = args[i].split(":");
			chainNodes.add(new CraqChain.ChainNode(s[0], Integer.parseInt(s[1])));
		}
		CraqChain chain = new CraqChain(chainNodes, nodeIndex);

		// start the server
		CraqNode node = new CraqNode(crMode, chain);
		node.start();
	}
}
