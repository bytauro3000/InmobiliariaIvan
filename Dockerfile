FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/InmobiliariaIVAN-1.0.0.jar app.jar

EXPOSE 8081

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-Xmx512m", "-Xms128m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=192m", "-XX:ReservedCodeCacheSize=64m", "-XX:MaxDirectMemorySize=64m", "-jar", "app.jar"]
