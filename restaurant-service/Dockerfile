FROM eclipse-temurin:17-jdk
VOLUME /tmp
COPY target/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
