plugins {
    `java-library`
    `maven-publish`
    signing
}

description = "PesaFlow4J auto-configuration for Spring Boot 2.x: a Pesaflow4jClient bean bound to " +
        "pesaflow4j.* properties, plus an opt-in webhook endpoint that publishes application events."

val springBootVersion = "2.7.18"

dependencies {
    api(project(":pesaflow4j-core"))
    implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    compileOnly("org.springframework:spring-webmvc:5.3.39")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-test:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    testImplementation("org.springframework:spring-webmvc:5.3.39")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.josemodi97.pesaflow4j.spring.boot2")
    }
}

apply(from = "${rootDir}/gradle/publishing.gradle.kts")
