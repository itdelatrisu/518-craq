package itdelatrisu.craq;

import java.util.Arrays;

/** Simple launcher. */
public class Launcher {
	/** Prints program usage. */
	private static void printUsage() {
		System.out.printf("CRAQ implementation.\n");
		System.out.printf("Run a sever:\n");
		System.out.printf("    server <is_cr_mode> <node_index> [<first_ip>:<first_port> ... <last_ip>:<last_port>]\n");
		System.out.printf("Run a client:\n");
		System.out.printf("    client <server_ip>:<server_port> <test_method> {<test_args> ...}\n");
	}

	/** Launches the program. */
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			printUsage();
			System.exit(1);
		}
		String[] nodeArgs = Arrays.copyOfRange(args, 1, args.length);
		if (args[0].equalsIgnoreCase("server")) {
			if (nodeArgs.length < 4) {
				printUsage();
				System.exit(1);
			}
			CraqNode.main(nodeArgs);
		} else if (args[0].equalsIgnoreCase("client")) {
			if (nodeArgs.length < 2) {
				printUsage();
				System.exit(1);
			}
			TestClient.main(nodeArgs);
		} else {
			printUsage();
			System.exit(1);
		}
	}
}
