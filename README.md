# MessageQueue
[![CI Build](https://github.com/Kilemonn/MessageQueue/actions/workflows/gradle.yml/badge.svg)](https://github.com/Kilemonn/MessageQueue/actions/workflows/gradle.yml) [![Coverage](.github/badges/jacoco.svg)](https://github.com/Kilemonn/MessageQueue/actions/workflows/gradle.yml)

## Overview

A message queue service, which can receive, hold and provide messages that are sent between services.
A storage mechanism can be used to persist messages and sub queues can be restricted so only correctly provided credentials
can interact with such queues.

**More detailed documentation can be found in the Wiki!**

## Quick Start

By default, the application will be store messages in memory and no queue restriction will be available.
To start the application you can use the following command to pull and run the latest version of the image:

`docker run -p8080:8080 kilemon/message-queue`

Once running the best endpoint to call at the moment is probably: `http://localhost:8080/queue/healthcheck`

The application provides a REST API to interact with the messages queued within the Multi Queue.
REST Documentation is provided as Swagger docs from the running application. 
You can simply run the docker image:
> docker run -p8080:8080 kilemon/message-queue

## Rest API Documentation

Once the image is running you can reach the Swagger documentation from the following endpoint: `http://localhost:8080/swagger-ui/index.html`.

---

## HTTPS

By default, the `MessageQueue` does not have HTTPS enabled and is exposed on port `8080`.
To enable HTTPS you'll need to provide your own SSL certificate and extend the current version of the image hosted at: https://hub.docker.com/r/kilemon/message-queue. When extending this image you want to add your own SSL certificate into the container and take note of the generated file location as you'll need to reference it in the environment properties you provide to the `MessageQueue`.
**NOTE: You need to use version 0.1.9 or above of the `MessageQueue` image.**

Below is an example Dockerfile that you could use to generate a self signed certificate.
Dockerfile:
```
FROM kilemon/message-queue:latest

# The generated cert will be placed at /messagequeue/keystore.p12 in the container (refer to path in docker compose file).
RUN ["keytool", "-genkeypair", "-alias", "sslcert", "-keyalg", "RSA", "-keysize", "4096", "-validity", "3650", "-dname", "CN=message-queue", "-keypass", "changeit", "-keystore", "keystore.p12", "-storeType", "PKCS12", "-storepass", "changeit"]

EXPOSE 8443

ENTRYPOINT ["java", "-jar", "messagequeue.jar"]
```

Using docker compose you can reference and build this Dockerfile and pass in the appropriate parameters to enable HTTP on the `MessageQueue` application:

docker-compose.yml:
```yaml
version: "3.9"
services:
  queue:
    container_name: queue
    build: .
    ports:
      - "8443:8443"
    environment:
      MULTI_QUEUE_TYPE: IN_MEMORY
      server.port: 8443 # The port set here must match the health check port below and the exposed port from the Dockerfile
      server.ssl.enabled: true
      server.ssl.key-store-type: PKCS12
      server.ssl.key-store: keystore.p12 # This path is relative to the `messagequeue.jar` location. The full location is /messagequeue/keystore.p12 for this example
      server.ssl.key-store-password: changeit
    healthcheck: # Example simple health check, disabling cert check for this example since it is self-signed
      test: wget --no-check-certificate https://localhost:8443/queue/healthcheck
      start_period: 3s
      interval: 3s
      timeout: 3s
      retries: 5
```

Once this starts up you should be able to access the application using HTTPS on the exposed port `8443`.
