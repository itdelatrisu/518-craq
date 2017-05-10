# CRAQ
Implementation of the CRAQ system based on the
[paper from USENIX '09](https://www.usenix.org/legacy/event/usenix09/tech/full_papers/terrace/terrace.pdf)
by Jeff Terrace and Michael J. Freedman.

### Building
Requires Java 8 and Maven.
```
$ mvn clean package
```

### Running servers
```
$ java -jar target/craq.jar server <is_cr_mode> <node_index> [<first_ip>:<first_port> ... <last_ip>:<last_port>]
```

For example, this will start a 3-node CRAQ chain locally:
```
$ java -jar target/craq.jar server 0 0 localhost:30001 localhost:30002 localhost:30003 &
$ java -jar target/craq.jar server 0 1 localhost:30001 localhost:30002 localhost:30003 &
$ java -jar target/craq.jar server 0 2 localhost:30001 localhost:30002 localhost:30003 &
```

### Running clients
```
$ java -jar target/craq.jar client <server_ip>:<server_port> <test_method> {<test_args> ...}
```

To list all available test methods, type "help" as the `test_method` argument.
```
$ java -jar target/craq.jar client <server_ip>:<server_port> help
------------------------
Available client methods
------------------------
* benchmarkLatency <size_bytes> <milliseconds> <num_busy_readers> {<busy_read_ip>:<busy_read_port> ...}
    Benchmarks latency of clean/dirty reads and writes under configurable load.
* benchmarkRead <num_clients> <milliseconds> {<additional_ip>:<additional_port> ...}
    Benchmarks read operations.
* benchmarkReadWrite <num_readers> <num_writers> <size_bytes> <min_writes_sec> <max_writes_sec> <rate_step> <milliseconds> [<read_ip>:<read_port> ...]
    Benchmarks read operations as write rate increases.
* benchmarkTestAndSet <size_bytes> <milliseconds>
    Benchmarks test-and-set operations.
* benchmarkWrite <num_clients> <size_bytes> <milliseconds>
    Benchmarks write operations.
* readEventual
    Basic read operation (eventual consistency).
* readEventualBounded <version_bound>
    Basic read operation (eventual bounded consistency).
* readStrong
    Basic read operation (strong consistency).
* testAndSet <value> <expected_version>
    Basic test-and-set operation.
* write <value>
    Basic write operation.
* writeBytes <size_bytes>
    Basic fixed-size write operation.
```

### Generating Thrift sources
Requires installing the [Thrift compiler](https://thrift.apache.org/download).
```
$ thrift --gen java -out src service.thrift
```

### Testing
To run servers:
```
$ ./test.sh [<first_ip>:<first_port> ... <last_ip>:<last_port>]
```
To stop servers:
```
$ ./closetest.sh
```
