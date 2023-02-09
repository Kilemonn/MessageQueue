import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.5"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.spring") version "1.8.0"
    jacoco
}

group = "au.kilemon"
// Make sure version matches version defined in MessageQueueApplication
version = "0.1.7"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:2.7.5")
    implementation("org.springframework.boot:spring-boot-starter-validation:2.7.5")

    // JPA dependency
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:2.7.5")

    // https://mvnrepository.com/artifact/org.springdoc/springdoc-openapi-ui
    implementation("org.springdoc:springdoc-openapi-ui:1.6.11")

    implementation("org.springframework.boot:spring-boot-starter-data-redis:2.7.5")

    implementation("com.google.code.gson:gson:2.10")

    compileOnly("org.projectlombok:lombok:1.18.24")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.8.0")

    // Database drivers
    // https://mvnrepository.com/artifact/com.mysql/mysql-connector-j
    implementation("com.mysql:mysql-connector-j:8.0.31")
    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    implementation("org.postgresql:postgresql:42.5.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test:2.7.5")
    // Required to mock MultiQueue objects since they apparently override a final 'remove(Object)' method.
    testImplementation("org.mockito:mockito-inline:5.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("org.testcontainers:testcontainers:1.17.5")
    testImplementation("org.testcontainers:junit-jupiter:1.17.5")
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
    toolVersion = "0.8.8"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // Generate the report after the tests
    reports {
        xml.required.set(false)
        csv.required.set(true)
    }
}
