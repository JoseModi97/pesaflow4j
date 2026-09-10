plugins {
    `java-library`
    `maven-publish`
    signing
}

description = "PesaFlow4J adapter for jakarta.servlet (Tomcat 10+, Spring Boot 3, Jakarta EE 9+ servlets)."

dependencies {
    api(project(":pesaflow4j-core"))
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

java {
    withJavadocJar()
    withSourcesJar()
}

// The jakarta.servlet-api (Jakarta EE 9+) ecosystem this module targets
// (Tomcat 10+, Spring Boot 3) requires Java 11+ at a minimum, so this
// module's floor is raised above the pesaflow4j-core default of Java 8.
tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.josemodi97.pesaflow4j.jakarta")
    }
}

apply(from = "${rootDir}/gradle/publishing.gradle.kts")
