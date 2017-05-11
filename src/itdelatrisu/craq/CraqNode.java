package itdelatrisu.craq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
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
	private final Map<Long, CraqObject> objects = new ConcurrentHashMap<>();

	/** Object read-write lock. */
	private final ReadWriteLock objectLock = new ReentrantReadWriteLock(true);

	/** Test-and-set read-write lock. */
	private final ReadWriteLock tasLock = new ReentrantReadWriteLock(true);

	/** The latest known clean object version. */
	private final AtomicLong latestCleanVersion = new AtomicLong(-1);

	/** The latest known version (clean or dirty). */
	private final AtomicLong latestVersion = new AtomicLong(-1);

	/** The garbage manager. */
	private final GarbageManager garbageManager = new GarbageManager();

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

		// start the garbage manager
		garbageManager.start();

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
		TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(port);
		CraqService.Processor processor = new CraqService.Processor(this);

		TServer server = new TThreadedSelectorServer(
			new TThreadedSelectorServer
				.Args(serverTransport)
				.processor(processor)
				.protocolFactory(new TBinaryProtocol.Factory())
				.transportFactory(new TFramedTransport.Factory())
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

	/** Creates a shallow copy of an object. */
	private CraqObject copyObject(CraqObject obj, Boolean dirty) {
		CraqObject copy = new CraqObject();
		copy.setValue(obj.value);
		if (dirty != null)
			copy.setDirty(dirty);
		return copy;
	}

	@Override
	public CraqObject read(CraqConsistencyModel model, long versionBound) throws TException {
		logger.debug("[Node {}] Received read request from client...", chain.getIndex());

		// node hasn't initialized?
		if (!chain.isTail() && (tailPool == null || successorPool == null))
			throw new TException("Chain is not initialized!");

		// running normal CR: fail if we're not the tail
		if (crMode && !chain.isTail())
			throw new TException("Cannot read from non-tail node in CR mode!");

		// no objects stored?
		if (objects.isEmpty())
			return new CraqObject();

		switch (model) {
		case STRONG:
			if (latestVersion.get() > latestCleanVersion.get() && !chain.isTail()) {
				// non-tail: latest known version isn't clean, send a version query
				CraqObject obj = getObjectFromVersionQuery();
				return copyObject(obj, true);
			} else if (latestCleanVersion.get() < 0) {
				// no clean version yet
				return new CraqObject();
			} else if (chain.isTail()) {
				// tail: return the latest known version
				objectLock.readLock().lock();
				try {
					CraqObject obj = objects.get(latestVersion.get());
					if (obj == null)
						throw new TException("Returning null object from the tail!");
					return copyObject(obj, false);
				} finally {
					objectLock.readLock().unlock();
				}
			} else {
				// latest known version is clean, return it
				objectLock.readLock().lock();
				try {
					CraqObject obj = objects.get(latestCleanVersion.get());
					if (obj == null)
						throw new TException("Returning null object from a clean read!");
					return copyObject(obj, false);
				} finally {
					objectLock.readLock().unlock();
				}
			}
		case EVENTUAL:
			// return latest known version
			objectLock.readLock().lock();
			try {
				CraqObject obj = objects.get(latestVersion.get());
				if (obj == null)
					throw new TException("Returning null object for an eventual read!");
				return copyObject(obj, null);
			} finally {
				objectLock.readLock().unlock();
			}
		case EVENTUAL_BOUNDED:
			// return latest known version within the given bound
			objectLock.readLock().lock();
			try {
				long cleanVersion = latestCleanVersion.get();
				long boundedVersion = cleanVersion + Math.min(versionBound, latestVersion.get() - cleanVersion);
				CraqObject obj = objects.get(boundedVersion);
				if (obj == null)
					throw new TException("Returning null object for a bounded eventual read!");
				return copyObject(obj, null);
			} finally {
				objectLock.readLock().unlock();
			}
		case DEBUG:
			// make a version query
			if (chain.isTail())
				return new CraqObject();
			CraqObject obj = getObjectFromVersionQuery();
			return copyObject(obj, true);
		default:
			throw new TException("Internal error!");
		}
	}

	@Override
	public long write(CraqObject obj) throws TException {
		logger.debug("[Node {}] Received write request from client...", chain.getIndex());

		tasLock.readLock().lock();
		try {
			return write(obj, null);
		} finally {
			tasLock.readLock().unlock();
		}
	}

	@Override
	public long testAndSet(CraqObject obj, long expectedVersion) throws TException {
		logger.debug("[Node {}] Received test-and-set request from client...", chain.getIndex());

		tasLock.writeLock().lock();
		try {
			return write(obj, expectedVersion);
		} finally {
			tasLock.writeLock().unlock();
		}
	}

	/** Handles a write. */
	private long write(CraqObject obj, Long expectedVersion) throws TException {
		// node hasn't initialized?
		if (tailPool == null || successorPool == null)
			throw new TException("Chain is not initialized!");

		// can only write to head
		if (!chain.isHead())
			throw new TException("Cannot write to non-head node.");

		// for test-and-set operations...
		if (expectedVersion != null) {
			// reject if latest version is not the expected version or there are uncommitted writes
			if (latestCleanVersion.get() != expectedVersion || latestVersion.get() != latestCleanVersion.get())
				return -1;
		}

		// record new object version
		long newVersion = latestVersion.incrementAndGet();
		objects.put(newVersion, obj);

		// send down chain
		CraqService.Client successor = getPooledConnection(successorPool);
		successor.writeVersioned(obj, newVersion);
		returnPooledConnection(successorPool, successor);

		// update clean version
		long oldCleanVersion = latestCleanVersion.getAndUpdate(x -> x < newVersion ? newVersion : x);
		if (newVersion > oldCleanVersion)
			removeOldVersions(latestCleanVersion.get());

		return newVersion;
	}

	@Override
	public void writeVersioned(CraqObject obj, long version) throws TException {
		logger.debug("[Node {}] Received write with version: {}", chain.getIndex(), version);

		// head should not receive versioned writes
		if (chain.isHead())
			throw new TException("Cannot make a versioned write to the head!");

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
		long oldCleanVersion = latestCleanVersion.getAndUpdate(x -> x < version ? version : x);
		if (version > oldCleanVersion || chain.isTail())
			removeOldVersions(latestCleanVersion.get());
	}

	/** Removes all object versions older than the latest clean one. */
	private void removeOldVersions(long latestCleanVersion) {
		objectLock.writeLock().lock();
		try {
			for (Iterator<Map.Entry<Long, CraqObject>> iter = objects.entrySet().iterator(); iter.hasNext();) {
				Map.Entry<Long, CraqObject> entry = iter.next();
				if (entry.getKey() < latestCleanVersion)
					iter.remove();
			}
		} finally {
			objectLock.writeLock().unlock();
		}
	}

	@Override
	public long versionQuery() throws TException {
		logger.debug("[Node {}] Received version query...", chain.getIndex());

		// only tail should receive version queries
		if (!chain.isTail())
			throw new TException("Cannot make a version query to a non-tail node!");

		// return latest clean version
		return latestCleanVersion.get();
	}

	/** Makes a version query to the tail and returns the appropriate object. */
	private CraqObject getObjectFromVersionQuery() throws TException {
		// send a version query
		CraqService.Client tail = getPooledConnection(tailPool);
		long tailVersion = tail.versionQuery();
		returnPooledConnection(tailPool, tail);

		// no clean version yet?
		if (tailVersion < 0)
			return new CraqObject();

		// retrieve the object
		CraqObject obj = objects.get(tailVersion);
		if (obj == null) {
			// newer version already committed (old one erased), return the latest clean version
			objectLock.readLock().lock();
			try {
				obj = objects.get(latestCleanVersion.get());
			} finally {
				objectLock.readLock().unlock();
			}
		}
		if (obj == null)
			throw new TException("Returning null object after a version query!");
		return obj;
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
