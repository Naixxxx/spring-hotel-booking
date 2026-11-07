FROM maven:3.9.9-eclipse-temurin-17 AS compile
WORKDIR /workspace
COPY . .
ARG MODULE
RUN mvn -B -ntp -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ARG MODULE
COPY --from=compile /workspace/${MODULE}/target/${MODULE}-1.0.0.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
