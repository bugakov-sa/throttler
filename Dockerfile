FROM openjdk:16-alpine3.13
WORKDIR /app
COPY /build/libs/throttling-0.1.jar /
EXPOSE 8082
ENTRYPOINT java -jar /throttling-0.1.jar