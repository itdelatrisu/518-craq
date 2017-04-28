package itdelatrisu.craq;

import java.util.List;
import java.util.stream.Collectors;

/** Represents a chain of CRAQ nodes. */
public class CraqChain {
	/** List of nodes in this chain, in order. */
	private final List<ChainNode> nodes;

	/** The index of this node. */
	private final int nodeIndex;

	/** Represents a chain node (host, port). */
	public static class ChainNode {
		public final String host;
		public final int port;
		public ChainNode(String host, int port) {
			this.host = host;
			this.port = port;
		}

		@Override
		public String toString() { return String.format("%s:%s", host, port); }
	}

	/** Creates a new chain. */
	public CraqChain(List<ChainNode> nodes, int nodeIndex) {
		if (nodeIndex > nodes.size())
			throw new IllegalArgumentException("nodeIndex > chain length");
		this.nodes = nodes;
		this.nodeIndex = nodeIndex;
	}

	/** Returns whether this node is the head of its chain. */
	public boolean isHead() { return (nodeIndex == 0); }

	/** Returns whether this node is the tail of its chain. */
	public boolean isTail() { return (nodeIndex == nodes.size() - 1); }

	/** Returns the successor (or null if none). */
	public ChainNode getSuccessor() {
		return (isTail()) ? null : nodes.get(nodeIndex + 1);
	}

	/** Returns the tail. */
	public ChainNode getTail() { return nodes.get(nodes.size() - 1); }

	/** Returns the chain node associated with the current node index. */
	public ChainNode getNode() { return nodes.get(nodeIndex); }

	/** Returns the current node index. */
	public int getIndex() { return nodeIndex; }

	/** Returns the size of this chain. */
	public int size() { return nodes.size(); }

	@Override
	public String toString() {
		return String.format(
			"Index [%d] in chain: %s",
			nodeIndex,
			String.join(", ", nodes.stream().map(ChainNode::toString).collect(Collectors.toList()))
		);
	}
}
