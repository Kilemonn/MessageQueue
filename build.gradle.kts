import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val springVersion = "3.4.2"
val springDocVersion = "2.6.0" // Version 2.7+ requires spring v3.4+
val testContainersVersion = "1.20.4"

plugins {
    id("org.springframework.boot") version "3.3.6" // Upgrading this requires java 21+ which is not supported by Kotlin yet or Gradle (because of Kotlin)
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.spring") version "2.1.10"
    jacoco
}

group = "au.kilemon"
// Make sure version matches version defined in MessageQueueApplication
version = "0.4.1"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-data-redis:${springVersion}")
    // JPA dependency
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${springVersion}")

    // https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-starter-webmvc-ui
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:${springDocVersion}")
    // https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-starter-webmvc-ui
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${springDocVersion}")

    implementation("com.google.code.gson:gson:2.11.0")

    compileOnly("org.projectlombok:lombok:1.18.32")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:2.1.10")

    // Database drivers
    // https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
    implementation("com.mysql:mysql-connector-j:8.4.0")
    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql:postgresql:42.7.3")
    // https://mvnrepository.com/artifact/com.microsoft.sqlserver/mssql-jdbc
    implementation("com.microsoft.sqlserver:mssql-jdbc:12.6.3.jre11")
    // https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")

    // DB Dialects (Needed for SQLLite)
    // https://mvnrepository.com/artifact/org.hibernate.orm/hibernate-community-dialects
    implementation("org.hibernate.orm:hibernate-community-dialects:6.6.13.Final")

    // No SQL drivers
    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-data-mongodb
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb:${springVersion}")

    // JWT token
    // https://mvnrepository.com/artifact/com.auth0/java-jwt
    implementation("com.auth0:java-jwt:4.4.0")

    /* Test dependencies */

    // Need to import this module name as lower case even if the repo is upper case
    // https://jitpack.io/#Kilemonn/Mock-All
    testImplementation("com.github.Kilemonn:mock-all:0.1.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${springVersion}")
    // Required to mock MultiQueue objects since they apparently override a final 'remove(Object)' method.
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.testcontainers:testcontainers:${testContainersVersion}")
    testImplementation("org.testcontainers:junit-jupiter:${testContainersVersion}")
    testImplementation(kotlin("test"))
}

// If we provide a `com.github.X:Artifact:...-SNAPSHOT` dependency this setting will make sure the snapshot
// Is not cached, so we always get the latest
configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // Generate the report after the tests
    reports {
        xml.required.set(false)
        csv.required.set(true)
    }
}
