FROM openjdk:13-jdk-alpine
COPY web/target/web-*.jar job-api.jar
EXPOSE 8080
CMD java ${JAVA_OPTS} -jar job-api.jar