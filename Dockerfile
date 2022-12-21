# Build image with tag
# docker build -t messagequeue .
#
# Tag image properly
# docker image tag messagequeue kilemon/message-queue:0.1.4
#
# Push image to remote
# docker push kilemon/message-queue:0.1.4

FROM gradle:7.5.1-jdk17-alpine as builder

WORKDIR /builder

# Copy everything in
COPY ./src ./src
COPY build.gradle.kts .
COPY gradle ./gradle
COPY gradle.properties .
COPY settings.gradle.kts .

# Run gradle build/package/tests
RUN ["gradle", "build", "-x", "test"]

FROM openjdk:17-alpine as runner
WORKDIR /messagequeue

# Copy in artifact from above step
COPY --from=builder /builder/build/libs/messagequeue-*.jar ./messagequeue.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "messagequeue.jar"]
