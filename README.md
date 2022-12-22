# MessageQueue
[![Java CI with Gradle](https://github.com/KyleGonzalez/MessageQueue/actions/workflows/gradle.yml/badge.svg)](https://github.com/KyleGonzalez/MessageQueue/actions/workflows/gradle.yml)

## Overview

A message queue service, which can receive, hold and provide messages that are sent between services.
The message storage mechanisms supported are:
- In-memory (default)
- Redis (stand alone and sentinel support)
- SQL Database (MySQL, PostgreSQL)

With plans to add support for the following mechanisms:
- NoSQL

## Rest API Documentation

The application provides a REST API to interact with the messages queued within the Multi Queue.
REST Documentation is provided as Swagger docs from the running application. 
You can simply run the docker image:
> docker run -p8080:8080 kilemon/message-queue:0.1.5

Once the image is running you can reach the Swagger documentation from the following endpoint: `http://localhost:8080/swagger-ui/index.html`.

## Quick Start

## In-Memory

The `In-Memory` configuration is the default and requires no further configuration.
Steps to run the In-Memory Multi Queue is as follows:
- `docker run -p8080:8080 kilemon/message-queue:0.1.5`
- Once running the best endpoint to call at the moment is probably: `http://localhost:8080/queue/keys`

If you really like you can provide an environment variable to the application to explicitly set the application into `In-Memory` mode: `MULTI_QUEUE_TYPE=IN_MEMORY`.

## Redis

The `Redis` setup requires some additional environmental configuration.
Firstly to set the application into `Redis` mode you need to provide the following environment variable with the appropriate value: `MULTI_QUEUE_TYPE=REDIS`.
Once this is set you will need to provide further environment variable in order to correctly configure the redis standalone or sentinel configuration (depending on your setup).

### Redis Environment Properties

Below are the optional and required properties for the `Redis` configuration.

#### REDIS_USE_SENTINELS

By default, this property is set to `false` (defaulting to direct connection to a single Redis instance).

This flag indicates whether the `MultiQueue` should connect directly to the redis instance or connect via one or more sentinel instances.
If set to `true` the `MultiQueue` will create a sentinel pool connection instead of a direct connection to a single redis node.

#### REDIS_PREFIX

By default, this property is set to `""` the application will apply no prefix.

For each defined "sub-queue" or "queue type", the application will create a single set entry into redis. This prefix can be used to remove/reduce the likelihood of any collisions if this is being used in an existing redis instance.

The defined value will be used as a prefix used for all redis entry keys that the application will create.
E.g. if the initial value for the redis entry key is `my-key` and no prefix is defined the entries would be stored under `my-key`.
Using the same scenario if the prefix is `prefix` then the prefix and entry key will be concatenated and the resultant key would be `prefixmy-key`.

#### REDIS_ENDPOINT

By default, this property is set to `127.0.0.1` (the application will auto append the default redis port if it is not defined).

The input endpoint string which is used for both standalone and the sentinel redis configurations.
This supports a comma separated list or single definition of a redis endpoint in the following formats:
`<endpoint>:<port>,<endpoint2>:<port2>,<endpoint3>`.

If you are using standalone and provide multiple endpoints, only the first will be used.

#### REDIS_MASTER_NAME

By default, this property is set to `mymaster`.
This is **REQUIRED** when `REDIS_USE_SENTINELS` is set to `true`. Is used to indicate the name of the redis master instance.

### Example:

An example of `Redis` configuration environment variables is below:
```yaml
environment:
 - MULTI_QUEUE_TYPE=REDIS
 - REDIS_USE_SENTINELS=true
 - REDIS_PREFIX=my-prefix
 - REDIS_ENDPOINT=sentinel1.com:5545,sentinel2.org:9980
 - REDIS_MASTER_NAME=not-my-master
```

## SQL Database

The `SQL` mechanism requires some configuration too similarly to `Redis`.
To set the application into `SQL` mode you need to provide the following environment variable with the appropriate value: `MULTI_QUEUE_TYPE=SQL`.

### SQL Environment Properties

Below are the required properties for `SQL` configuration.

#### spring.jpa.hibernate.ddl-auto

Depending on your setup this is probably required initially but may change based on your usage needs. Since the application uses Hibernate to create and generate the database structure.
If the structure is not initialised, I recommend setting this property to `create` (E.g. `spring.jpa.hibernate.ddl-auto=create`).

Please refer to https://docs.spring.io/spring-boot/docs/1.1.0.M1/reference/html/howto-database-initialization.html#howto-initialize-a-database-using-hibernate

#### spring.autoconfigure.exclude

***This property is required***.

Just providing `spring.autoconfigure.exclude=` as one of the environment variables is required to force JPA to initialise correctly. 
By default, it is suppressed to allow of more stream lined configuration of the other mechanisms.

#### spring.datasource.url

***This property is required***.

This defines the database connection string that the application should connect to. E.g: `jdbc:mysql://localhost:3306/message-queue`

#### spring.datasource.username

***This property is required***.

This is the username/account name used to access the database at the configured endpoint.

#### spring.datasource.password

***This property is required***.

This is the password used to access the database at the configured endpoint.

### Example:
```yaml
environment:
 - MULTI_QUEUE_TYPE=SQL
 - spring.jpa.hibernate.ddl-auto=create
 - spring.autoconfigure.exclude=
 - spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/postgres
 - spring.datasource.username=postgres
 - spring.datasource.password=5up3r5tR0nG!
```
