#FROM openjdk:11.0-jdk-slim as builder
#VOLUME /tmp
#COPY . .
#RUN ./gradlew build

FROM openjdk:11.0-jdk-slim as builder
VOLUME /tmp
COPY . .
RUN apt-get update && apt-get install -y dos2unix
RUN dos2unix gradlew
RUN ./gradlew build

FROM openjdk:11.0-jre-slim
WORKDIR /app
# Copy .jar file (aka, builder)
COPY --from=builder build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080


