
## Dev

```bash
docker-compose up
./mvnw install -DskipTests && ./mvnw spring-boot:run -pl web
```

### Check events

```bash
docker-compose exec schema-registry sh -c "kafka-avro-console-consumer --bootstrap-server kafka:29092 --topic job-log --from-beginning --property print.key=true --key-deserializer=org.apache.kafka.common.serialization.StringDeserializer"
```

## TODO

* optimize kafka settings
* redirect to api docs on root path
* dedicated aggregate topic?