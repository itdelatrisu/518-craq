# CRAQ

Build with Maven and run the packaged JAR:
```
$ mvn clean package
$ java -jar target/craq.jar {<port>}
```

To generate Thrift sources:
```
$ thrift --gen java -out src service.thrift
```
