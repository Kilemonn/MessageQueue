FROM gradle:7.5.1-jdk11-alpine as builder

#RUN groupadd -r builder.group
#RUN useradd -r -g builder.group builder.user
#USER builder.user

WORKDIR /builder

# Copy everything in
COPY ./src ./src
COPY build.gradle.kts .
COPY gradle ./gradle
COPY gradle.properties .
COPY settings.gradle.kts .

# Run gradle build/package/tests
RUN ["gradle", "build"]

FROM openjdk:11-jre-slim
WORKDIR /messagequeue

# Copy in artifact from above step
COPY --from=builder /builder/build/libs/messagequeue-*.jar ./messagequeue.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "messagequeue.jar"]
