package itdelatrisu.craq;

import java.util.List;

public class CraqChain {
	List<ChainNode> nodes;
	int nodeIndex;
	
	public static class ChainNode {
		public final String host;
		public final int port;
		public ChainNode(String host, int port) {
			this.host = host;
			this.port = port;
		}
	}
	
	public CraqChain(List<ChainNode> nodes, int nodeIndex) {
		this.nodes = nodes;
		this.nodeIndex = nodeIndex;
	}
	
	/** Returns whether this node is the head of its chain. */
	public boolean isHead() {
		return (nodeIndex == 0);
	}

	/** Returns whether this node is the tail of its chain. */
	public boolean isTail() {
		return (nodeIndex == nodes.size() - 1);
	}
	
	public ChainNode getSuccessor() {
		if (!isTail()) {
			return nodes.get(nodeIndex + 1); 
		}
		else {
			return null;
		}
	}

	public ChainNode getTail() {
		return nodes.get(nodes.size() - 1);
	}
}
