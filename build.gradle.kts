import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val springVersion = "3.2.1"

plugins {
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.spring") version "1.9.21"
    jacoco
}

group = "au.kilemon"
// Make sure version matches version defined in MessageQueueApplication
version = "0.3.1"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-validation:${springVersion}")
    implementation("org.springframework.boot:spring-boot-starter-data-redis:${springVersion}")
    // JPA dependency
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${springVersion}")
    // No SQL drivers
    // https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-data-mongodb
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb:${springVersion}")

    // https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-starter-webmvc-ui
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.3.0")
    // https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-starter-webmvc-ui
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    implementation("com.google.code.gson:gson:2.10.1")

    compileOnly("org.projectlombok:lombok:1.18.30")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.9.21")

    // Database drivers
    // https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
    implementation("com.mysql:mysql-connector-j:8.2.0")
    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql:postgresql:42.7.1")

    // JWT token
    // https://mvnrepository.com/artifact/com.auth0/java-jwt
    implementation("com.auth0:java-jwt:4.4.0")

    // Test dependencies
    testImplementation("au.kilemon:mock-all:0.1.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test:${springVersion}")
    // Required to mock MultiQueue objects since they apparently override a final 'remove(Object)' method.
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // Generate the report after the tests
    reports {
        xml.required.set(false)
        csv.required.set(true)
    }
}
