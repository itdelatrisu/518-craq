package itdelatrisu.craq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
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
	private static final int CONNECTION_SLEEP_TIME = 1000;

	/** Number of connections to open to each node. */
	private static final int CONNECTION_POOL_SIZE = 50;

	/** Whether this node is running in CR mode (not CRAQ). */
	private final boolean crMode;

	/** The entire chain. */
	private final CraqChain chain;

	/** The Thrift client connection pools. */
	private BlockingQueue<CraqService.Client> tailPool, successorPool;

	/** Current known objects: <version, bytes> */
	private final Map<Integer, CraqObject> objects = new ConcurrentHashMap<>();

	/** The latest known clean object version. */
	private final AtomicInteger latestCleanVersion = new AtomicInteger(-1);

	/** The latest known version (clean or dirty). */
	private final AtomicInteger latestVersion = new AtomicInteger(-1);

	/** Creates a new CRAQ server node. */
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
					connect();
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

	/** Connects to the other nodes in the chain. */
	private void connect() throws InterruptedException {
		// connect to tail
		if (chain.isTail())
			return;
		this.tailPool = createConnectionPool(chain.getTail().host, chain.getTail().port);
		logger.info(
			"[Node {}] Connected to tail at {}:{}",
			chain.getIndex(), chain.getTail().host, chain.getTail().port
		);

		// connect to successor
		if (chain.getIndex() == chain.size() - 2) {
			// is this the node before the tail?
			this.successorPool = tailPool;
			return;
		}
		this.successorPool = createConnectionPool(chain.getSuccessor().host, chain.getSuccessor().port);
		logger.info(
			"[Node {}] Connected to successor at {}:{}",
			chain.getIndex(), chain.getSuccessor().host, chain.getSuccessor().port
		);
	}

	/** Creates a connection pool to the given Thrift server. */
	private BlockingQueue<CraqService.Client> createConnectionPool(String host, int port) throws InterruptedException {
		// connect to first server...
		CraqService.Client client;
		while (true) {
			try {
				client = connectToServer(host, port);
				break;
			} catch (TTransportException e) {
				Thread.sleep(CONNECTION_SLEEP_TIME);
			}
		}

		// create pool of connections
		BlockingQueue<CraqService.Client> queue = new LinkedBlockingQueue<>(CONNECTION_POOL_SIZE);
		queue.offer(client);
		while (queue.remainingCapacity() > 0) {
			try {
				queue.offer(connectToServer(host, port));
			} catch (TTransportException e) {
				logger.error("Failed to create connection pool.", e);
				break;
			}
		}
		return queue;
	}

	/** Connects to the Thrift server. */
	private CraqService.Client connectToServer(String host, int port) throws TTransportException {
		TTransport transport = new TFramedTransport(new TSocket(host, port));
		transport.open();

		TProtocol protocol = new TBinaryProtocol(transport);
		CraqService.Client client = new CraqService.Client(protocol);

		return client;
	}

	/** Starts the Thrift server. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void runServer(int port) throws TTransportException {
		TServerTransport serverTransport = new TServerSocket(port);
		CraqService.Processor processor = new CraqService.Processor(this);

		TServer server = new TThreadPoolServer(
			new TThreadPoolServer
				.Args(serverTransport)
				.processor(processor)
				.inputTransportFactory(new TFramedTransport.Factory())
				.outputTransportFactory(new TFramedTransport.Factory())
		);
		logger.info("Starting Thrift server on port {}...", port);
		server.serve();
	}

	/** Retrieves a connection from the pool. */
	private CraqService.Client getPooledConnection(BlockingQueue<CraqService.Client> pool) throws TException {
		try {
			return pool.take();
		} catch (InterruptedException e) {
			logger.error("Interrupted before retrieving pooled connection!", e);
			throw new TException(e);
		}
	}

	/** Returns a connection to the pool. */
	private void returnPooledConnection(BlockingQueue<CraqService.Client> pool, CraqService.Client client) {
		if (!pool.offer(client))
			logger.error("Failed to return the connection to the pool!");
	}

	@Override
	public CraqObject read(CraqConsistencyModel model, int versionBound) throws TException {
		logger.debug("[Node {}] Received read request from client...", chain.getIndex());

		// node hasn't initialized?
		if (!chain.isTail() && (tailPool == null || successorPool == null))
			return new CraqObject();

		// no objects stored?
		if (objects.isEmpty())
			return new CraqObject();

		// running normal CR: fail if we're not the tail
		if (crMode && !chain.isTail())
			return new CraqObject();

		// strong consistency?
		if (model == CraqConsistencyModel.STRONG) {
			if (latestVersion.get() > latestCleanVersion.get()) {
				// latest known version isn't clean, send a version query
				CraqService.Client tail = getPooledConnection(tailPool);
				int tailVersion = tail.versionQuery();
				returnPooledConnection(tailPool, tail);
				if (tailVersion < 0)
					return new CraqObject();  // no clean version yet
				return objects.get(tailVersion);
			} else if (latestCleanVersion.get() < 0) {
				// no clean version yet
				return new CraqObject();
			} else {
				// latest known version is clean, return it
				return objects.get(latestCleanVersion.get());
			}
		}

		// eventual consistency?
		else if (model == CraqConsistencyModel.EVENTUAL) {
			// return latest known version
			return objects.get(latestVersion.get());
		}

		// bounded eventual consistency?
		else if (model == CraqConsistencyModel.EVENTUAL_BOUNDED) {
			// return latest known version within the given bound
			int boundedVersion = latestCleanVersion.get() + Math.min(versionBound, latestVersion.get() - latestCleanVersion.get());
			return objects.get(boundedVersion);
		}

		throw new TException("Internal error!");
	}

	@Override
	public boolean write(CraqObject obj) throws TException {
		logger.debug("[Node {}] Received write request from client...", chain.getIndex());

		// node hasn't initialized?
		if (tailPool == null || successorPool == null)
			return false;

		// can only write to head
		if (!chain.isHead())
			return false;

		// record new object version
		int newVersion = latestVersion.incrementAndGet();
		objects.put(newVersion, obj);

		// send down chain
		CraqService.Client successor = getPooledConnection(successorPool);
		successor.writeVersioned(obj, newVersion);
		returnPooledConnection(successorPool, successor);

		// update clean version
		int oldCleanVersion = latestCleanVersion.getAndUpdate(x -> x < newVersion ? newVersion : x);
		if (newVersion > oldCleanVersion)
			removeOldVersions(latestCleanVersion.get());

		return true;
	}

	@Override
	public void writeVersioned(CraqObject obj, int version) throws TException {
		logger.debug("[Node {}] Received write with version: {}", chain.getIndex(), version);

		// add new object version
		objects.put(version, obj);

		// update latest version
		latestVersion.getAndUpdate(x -> x < version ? version : x);

		// non-tail: send down chain
		if (!chain.isTail()) {
			CraqService.Client successor = getPooledConnection(successorPool);
			successor.writeVersioned(obj, version);
			returnPooledConnection(successorPool, successor);
		}

		// mark clean
		int oldCleanVersion = latestCleanVersion.getAndUpdate(x -> x < version ? version : x);
		if (version > oldCleanVersion || chain.isTail())
			removeOldVersions(latestCleanVersion.get());
	}

	/** Removes all object versions older than the latest clean one. */
	private synchronized void removeOldVersions(int latestCleanVersion) {
		for (Iterator<Map.Entry<Integer, CraqObject>> iter = objects.entrySet().iterator(); iter.hasNext();) {
			Map.Entry<Integer, CraqObject> entry = iter.next();
			if (entry.getKey() < latestCleanVersion)
				iter.remove();
		}
	}

	@Override
	public int versionQuery() throws TException {
		logger.debug("[Node {}] Received version query...", chain.getIndex());

		// only tail should receive version queries
		if (!chain.isTail())
			return -1;

		// return latest clean version
		return latestCleanVersion.get();
	}

	@Override
	public boolean testAndSet(CraqObject obj, CraqObject objExpected) throws TException {
		// TODO
		return false;
	}

	/** Starts the CRAQ server node. */
	public static void main(String[] args) {
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
