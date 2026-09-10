plugins {
    `java-library`
    `maven-publish`
    signing
}

description = "PesaFlow4J auto-configuration for Spring Boot 3.x: a Pesaflow4jClient bean bound to " +
        "pesaflow4j.* properties, plus an opt-in webhook endpoint that publishes application events."

val springBootVersion = "3.3.5"

dependencies {
    api(project(":pesaflow4j-core"))
    implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    compileOnly("org.springframework:spring-webmvc:6.1.14")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-test:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    testImplementation("org.springframework:spring-webmvc:6.1.14")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

java {
    withJavadocJar()
    withSourcesJar()
}

// Spring Boot 3.x itself requires Java 17+, so this module's floor is
// raised above the pesaflow4j-core default of Java 8.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.josemodi97.pesaflow4j.spring.boot3")
    }
}

apply(from = "${rootDir}/gradle/publishing.gradle.kts")
