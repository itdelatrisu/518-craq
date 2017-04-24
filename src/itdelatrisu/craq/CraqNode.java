package itdelatrisu.craq;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

	// TODO: predecessor, successor, or whole chain?

	/** Whether this node is running in CR mode (not CRAQ). */
	private final boolean crMode;
	private final boolean head;
	private final boolean tail;
	
	//can be changed?
	private CraqNode predecessor;
	private CraqNode successor;
	
	private LinkedList<Integer> versions;

	/** Current known objects: <version, bytes> */
	//private final Map<Integer, CraqObject> objects = new HashMap<>();

	/** Synchronization aids for unacknowledged versions. */
	//private final Map<Integer, CountDownLatch> syncMap = new HashMap<>();

	/** The latest known clean object version. */
	private int latestCleanVersion = -1;

	/** The latest known version (clean or dirty). */
	private int latestVersion = -1;

	/** Creates a new CRAQ node. */
	public CraqNode(boolean crMode, boolean head, boolean tail, CraqNode predecessor, CraqNode successor) {
		this.crMode = crMode;
		this.head = head;
		this.tail = tail;
		this.predecessor = predecessor;
		this.successor = successor;
		
		//if tail, it has only one element
		this.versions = new LinkedList<Integer>();
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

	/** Returns whether this node is the head of its chain. */
	private boolean isHead() { 
		return this.head; 
	}

	/** Returns whether this node is the tail of its chain. */
	private boolean isTail() { 
		return this.tail; 
	}

	@Override
	public CraqObject read(CraqConsistencyModel model) throws TException {
		logger.debug("Received read request from client...");

		// no objects stored?
		if (versions.isEmpty())
			return new CraqObject(); // return null

		// running normal CR: fail if we're not the tail
		if (crMode && !isTail())
			return new CraqObject();

		// strong consistency?
		if (model == CraqConsistencyModel.STRONG) {
			if (latestVersion > latestCleanVersion) {
				// latest known version isn't clean
				// TODO send version query
				//int tailVersion = tail.versionQuery();
				//return objects.get(tailVersion);
			} else {
				// latest known version is clean, return it
				// TODO I don't konw how to put the return value in
				CraqObject return_obj = new CraqObject(versions.get(latestVersion));
				return return_obj;
			}
		}

		// eventual consistency?
		else if (model == CraqConsistencyModel.EVENTUAL) {
			// return latest known version				
			CraqObject return_obj = new CraqObject(versions.get(latestVersion));
			return return_obj;
		}

		// TODO bounded eventual consistency?

		logger.error("!! read() shouldn't get here !!");
		return new CraqObject();
	}

	@Override
	public boolean write(CraqObject obj) throws TException {
		logger.debug("Received write request from client...");

		// can only write to head
		if (!isHead())
			return false;

		int newVersion;
		synchronized (this) {
			// record new object version
			newVersion = latestVersion + 1;
			versions.add(obj.return_value);//TODO obj.value?
//			objects.put(newVersion, obj);
			latestVersion++;

			// TODO send down chain
			// TODO multicast optimization
			//successor.writeVersioned(obj, newVersion);
		}

		//we decided to use the return value as ack
		// wait for acknowledgement before returning to the client
//		CountDownLatch signal = new CountDownLatch(1);
//		syncMap.put(newVersion, signal);
//		try {
//			//signal.await();
//			// TODO: timeout here for testing, delete it later
//			return signal.await(3, TimeUnit.SECONDS);
//		} catch (InterruptedException e) {
//			logger.error("Interrupted before receiving acknowledgement for version: {}", newVersion);
//			return false;
//		} finally {
//			syncMap.remove(newVersion);
//		}

		return true;
	}

	@Override
	public void writeVersioned(CraqObject obj, int version) throws TException {
		logger.debug("Propagating write with version: {}", version);

		// add new object version
		versions.add(obj.return_value);

		// update latest version
		synchronized (this) {
			if (version > latestVersion)
				latestVersion = version;
		}

		if (isTail()) {
			if (version > latestCleanVersion)
				latestCleanVersion = version;

			// TODO send acknowledgement up chain
			// TODO: multicast optimization
			//predecessor.ack(version);
		} else {
			// TODO send down chain
			//successor.writeVersioned(obj, version);
		}
	}

//	@Override
//	public void ack(int version) throws TException {
//		logger.debug("Received acknowledgement for version: {}", version);
//
//		// tail should not receive acks
//		if (isTail())
//			return;
//
//		// record clean version
//		if (version > latestCleanVersion)
//			latestCleanVersion = version;
//
//		if (isHead()) {
//			// head: notify blocked write() thread
//			CountDownLatch signal = syncMap.get(version);
//			if (signal != null)
//				signal.countDown();
//		} else {
//			// TODO send up chain
//			//predecessor.ack(version);
//		}
//	}

	@Override
	public int versionQuery() throws TException {
		logger.debug("Received version query...");

		// only tail should receive version queries
		if (!isTail())
			return -1;

		// return latest clean version
		return latestCleanVersion;
	}

	/** Starts the CRAQ server node. */
	public static void main(String[] args) {
		int port = (args.length < 1) ? 9090 : Integer.parseInt(args[0]);
		boolean crMode = false;  // TODO command line param?
		boolean head = false;
		boolean tail = false;
		CraqNode predecessor = null;
		CraqNode successor = null;
		CraqNode node = new CraqNode(crMode, head, tail, predecessor, successor);
		node.start(port);
	}

	@Override
	public int test_and_set(CraqObject obj) throws TException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void ack(int version) throws TException {
		// TODO Auto-generated method stub
		
	}
}
