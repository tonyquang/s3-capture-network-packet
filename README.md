# Network Traffic Capture
A Application capture traffic on computer
and publish logs from traffic captured to **Apache Kafka** (A Distributed Streaming Platform)

## Installation & Run

```bash
.\mvnw clean install
```
After run above comment, waiting it done. After that enter folder target and copy CapturingNetworkPacket.jar and run this below command
```bash
java -jar CapturingNetworkPacket.jar -k <IP_KAFKA_SERVER>:<PORT> -t <TOPIC_NAME>
```

#### Port using
```bash
127.0.0.1:9999 - Proxy Server
```
 
