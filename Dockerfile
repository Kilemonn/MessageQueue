FROM gradle:3.4-jdk-alpine AS builder
# Copy everything in
# Run gradle build/package/tests

FROM openjdk:11-jre-slim
# Copy in artifact from above step
