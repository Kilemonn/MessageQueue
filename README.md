# MessageQueue
[![CI Build](https://github.com/Kilemonn/MessageQueue/actions/workflows/gradle.yml/badge.svg)](https://github.com/Kilemonn/MessageQueue/actions/workflows/gradle.yml) [![Coverage](.github/badges/jacoco.svg)](https://github.com/Kilemonn/MessageQueue/actions/workflows/gradle.yml)

## Overview

A message queue service, which can receive, hold and provide messages that are sent between services.
A storage mechanism can be used to persist messages and sub queues can be restricted so only correctly provided credentials
can interact with such queues.

### More detailed documentation can be found in the [Wiki](https://github.com/Kilemonn/MessageQueue/wiki)!

## Quick Start

There is an image hosted in Docker Hub: https://hub.docker.com/r/kilemon/message-queue

By default, the application will be store messages in memory and no queue restriction will be available.
To start the application you can use the following command to pull and run the latest version of the image:

`docker run -p8080:8080 kilemon/message-queue`

Once running the best endpoint to call at the moment is probably: `http://localhost:8080/queue/healthcheck`

The application provides REST APIs to interact with the messages queued within the MultiQueue.

## Rest API Documentation

REST Documentation is provided as Swagger docs from the running application. Once the image is running you can reach the Swagger documentation from the following endpoint: `http://localhost:8080/swagger-ui/index.html`.
