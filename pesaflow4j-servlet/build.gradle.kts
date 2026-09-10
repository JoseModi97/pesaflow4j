plugins {
    `java-library`
    `maven-publish`
    signing
}

description = "PesaFlow4J adapter for javax.servlet (Tomcat 8/9, Spring Boot 2, plain Java EE servlets)."

dependencies {
    api(project(":pesaflow4j-core"))
    compileOnly("javax.servlet:javax.servlet-api:4.0.1")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("javax.servlet:javax.servlet-api:4.0.1")
    testImplementation("org.mockito:mockito-core:5.14.2")
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.josemodi97.pesaflow4j.servlet")
    }
}

apply(from = "${rootDir}/gradle/publishing.gradle.kts")
