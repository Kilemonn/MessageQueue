# Build image with tag
# docker build -t kilemon/message-queue:0.1.5 .
# docker tag kilemon/message-queue:0.1.5 kilemon/message-queue:latest
#
# Push image to remote
# docker push kilemon/message-queue:0.1.5

FROM gradle:8.12.1-jdk17-alpine AS builder

WORKDIR /builder

# Copy everything in
COPY ./src ./src
COPY build.gradle.kts .
COPY gradle ./gradle
COPY gradle.properties .
COPY settings.gradle.kts .

# Run gradle build/package/tests
RUN ["gradle", "build", "-x", "test"]

FROM amazoncorretto:17-alpine3.21-jdk AS runner
WORKDIR /messagequeue

# Copy in artifact from above step
COPY --from=builder /builder/build/libs/messagequeue-*.jar ./messagequeue.jar

EXPOSE 8080
EXPOSE 8443

ENTRYPOINT ["java", "-jar", "messagequeue.jar"]
