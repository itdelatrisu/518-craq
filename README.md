# CRAQ

Build with Maven and run the packaged JAR.

For server nodes:
```
$ mvn clean package
$ java -jar target/craq.jar {<port>}
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
