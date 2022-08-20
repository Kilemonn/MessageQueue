import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
}

group = "au.kilemon"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:2.7.2")
    implementation("org.springframework.boot:spring-boot-starter-validation:2.7.2")

    compileOnly("org.projectlombok:lombok:1.18.24")

    testImplementation("org.springframework.boot:spring-boot-starter-test:2.7.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
