# CRAQ

Build with Maven and run the packaged JAR.

### Building
Requires Java 8 and Maven.
```
$ mvn clean package
```

### Running servers
```
$ java -jar target/craq.jar server <is_cr_mode> <node_index> [<first_ip>:<first_port> ... <last_ip>:<last_port>]
```

### Running clients
```
$ java -jar target/craq.jar client <server_host> <server_port> <test_name>
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
To end servers:
```
$ ./closetest.sh
```
