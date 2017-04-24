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
	CraqObject read(1:CraqConsistencyModel model),

	/** Writes a new value. */
	bool write(1:CraqObject obj),

	// -------------------------------------------------------------------------
	// Internal methods
	// -------------------------------------------------------------------------
	/** Writes a new value with the given version. */
	oneway void writeVersioned(1:CraqObject obj, 2:i32 version),

	/** Acknowledges that a version number is committed. */
	oneway void ack(1:i32 version),

	/** Returns the latest committed version. */
	i32 versionQuery()
}
