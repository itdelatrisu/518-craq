package itdelatrisu.craq;

import itdelatrisu.craq.thrift.CraqConsistencyModel;
import itdelatrisu.craq.thrift.CraqObject;
import itdelatrisu.craq.thrift.CraqService;

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

/** CRAQ server node. */
public class CraqNode implements CraqService.Iface {
	private static final Logger logger = LoggerFactory.getLogger(CraqNode.class);

	/** Whether this node is running in CR mode (not CRAQ). */
	private final boolean crMode;

	//can be changed?
	private CraqService.Client tail;
	private CraqService.Client successor;
	
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
	public void start(int port) {
		// TODO this is blocking, should be the last thing we call?
		try {
			// TODO connect to others
			runServer(port);
		} catch (TTransportException e) {
			logger.error("Failed to start Thrift server.", e);
			return;
		}

		// TODO test code
//		try {
//			connectToClient("localhost", port);
//		} catch (TException e) {
//			logger.error("Failed to connect to Thrift client.", e);
//		}
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

	/** Connects to the Thrift clients. */
	private void connectToClient(String host, int port) throws TException {
		// TODO figure out how to use Zookeeper, this is placeholder code
		TTransport transport = new TSocket(host, port);
		transport.open();

		TProtocol protocol = new TBinaryProtocol(transport);
		CraqService.Client client = new CraqService.Client(protocol);

		transport.close();
	}

	@Override
	public CraqObject read(CraqConsistencyModel model) throws TException {
		logger.debug("Received read request from client...");

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
		logger.debug("Propagating write with version: {}", version);

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
		int port = (args.length < 1) ? 9090 : Integer.parseInt(args[1]);
		List<CraqChain.ChainNode> chainNodes = new ArrayList<>();
		boolean crMode = Integer.parseInt(args[0]) == 1;
		
		// <isCrMode?> <my port> <my index> [<first ip> <first port> ...]
		for (int i = 3; i < args.length; i += 2) {
			chainNodes.add(new CraqChain.ChainNode(args[i], Integer.parseInt(args[i+1])));
		}
		
		CraqChain chain = new CraqChain(chainNodes, Integer.parseInt(args[2]));
		CraqNode node = new CraqNode(crMode, chain);
		node.start(port);
	}
}
