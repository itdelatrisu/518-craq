package itdelatrisu.craq;

/** CRAQ launcher. */
public class Launcher {
	public static void main(String[] args) {
		int port = (args.length < 1) ? 9090 : Integer.parseInt(args[0]);
		CraqNode node = new CraqNode();
		node.start(port);
	}
}
