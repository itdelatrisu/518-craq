namespace java itdelatrisu.craq.thrift

/** Consistency models. */
enum CraqConsistencyModel { STRONG, EVENTUAL }

/** Object envelope. */
struct CraqObject {
	1: optional binary value;
}

/** CRAQ service. */
service CraqService {
	// -------------------------------------------------------------------------
	// Client-facing methods
	// -------------------------------------------------------------------------
	/** Reads a value with the desired consistency model. */
	CraqObject read(1:CraqConsistencyModel model), // and specify which node?

	/** Writes a new value. */
	bool write(1:CraqObject obj),
	
	/** Performs a test-and-set operation. **/
	bool testAndSet(1:CraqObject obj, 2:CraqObject objExpected),

	// -------------------------------------------------------------------------
	// Internal methods
	// -------------------------------------------------------------------------
	/** Writes a new value with the given version. */
	void writeVersioned(1:CraqObject obj, 2:i32 version),

	/** Returns the latest committed version. */
	i32 versionQuery()
}
