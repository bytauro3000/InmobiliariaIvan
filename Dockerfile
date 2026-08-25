FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/InmobiliariaIVAN-1.0.0.jar app.jar

EXPOSE 8081

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-Xmx256m", "-Xms96m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=128m", "-XX:ReservedCodeCacheSize=48m", "-XX:MaxDirectMemorySize=32m", "-jar", "app.jar"]
