# CRAQ

Build with Maven and run the packaged JAR.

For server nodes:
```
$ mvn clean package
$ java -jar target/craq.jar <is_cr_mode> <node_index> [<first_ip>:<first_port> ... <last_ip>:<last_port>]
```

For client nodes:
```
$ mvn clean package -DmainClassName=itdelatrisu.craq.CraqClient
$ java -jar target/craq.jar {<host> {<port>}}
```

To generate Thrift sources:
```
$ thrift --gen java -out src service.thrift
```

To run servers:
```
$. test.sh
```
To end servers:
```
$. closetest.sh
```
