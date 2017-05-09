namespace java itdelatrisu.craq.thrift

/** Version numbers. */
typedef i64 Version

/** Consistency models. */
enum CraqConsistencyModel { STRONG, EVENTUAL, EVENTUAL_BOUNDED }

/** Object envelope. */
struct CraqObject {
	1: optional binary value;
	2: optional bool dirty;
}

/** CRAQ service. */
service CraqService {
	// -------------------------------------------------------------------------
	// Client-facing methods
	// -------------------------------------------------------------------------
	/** Reads a value with the desired consistency model. */
	CraqObject read(1:CraqConsistencyModel model, 2:Version versionBound),

	/** Writes a new value. */
	Version write(1:CraqObject obj),
	
	/** Performs a test-and-set operation. **/
	Version testAndSet(1:Version requestVersion, 2:CraqObject obj),

	// -------------------------------------------------------------------------
	// Internal methods
	// -------------------------------------------------------------------------
	/** Writes a new value with the given version. */
	void writeVersioned(1:CraqObject obj, 2:Version version),

	/** Returns the latest committed version. */
	Version versionQuery()
}
