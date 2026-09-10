plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
}

group = "io.github.josemodi97"
version = project.findProperty("pesaflow4jVersion") as String? ?: "0.1.0-SNAPSHOT"

description = "Gradle plugin for PesaFlow4J: scaffolds a placeholder pesaflow4j.properties " +
        "credentials file into your project (./gradlew pesaflow4jInit)."

repositories {
    mavenCentral()
    // The generated Spring Boot examples are compile-verified in tests
    // against the real pesaflow4j-spring-boot2/3-starter jars, which aren't
    // on Maven Central (this repo isn't published yet) - only available
    // locally after `gradle publishToMavenLocal` (or `mvn install`) from
    // the root reactor, since this is a standalone build. See PLAN.md.
    mavenLocal()
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.named<JavaCompile>("compileJava") {
    // Gradle plugin bytecode is loaded by whatever JVM runs the Gradle
    // daemon; targeting Java 8 keeps this plugin usable by the widest range
    // of Gradle/JDK combinations, matching the rest of PesaFlow4J. This is
    // deliberately NOT applied to compileTestJava: the test suite's
    // compile-verification of the generated Spring Boot examples needs
    // pesaflow4j-spring-boot3-starter and pesaflow4j-jakarta on its
    // classpath, which declare a Java 17 / 11 floor via Gradle module
    // metadata - a constraint on the shipped plugin jar, not on tests that
    // never leave this build.
    options.release.set(8)
}

val pesaflow4jVersionForTests = "0.1.0-SNAPSHOT"

dependencies {
    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Test-only: the generated framework examples (Pesaflow4jInitTask /
    // FrameworkExample) are verified by actually compiling them with
    // javax.tools.JavaCompiler against these real dependency jars - not
    // just eyeballed - so a wrong generated import or method signature
    // fails the build, not just looks plausible.
    testImplementation("javax.servlet:javax.servlet-api:4.0.1")
    testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
    testImplementation("io.github.josemodi97:pesaflow4j-core:$pesaflow4jVersionForTests")
    testImplementation("io.github.josemodi97:pesaflow4j-servlet:$pesaflow4jVersionForTests")
    testImplementation("io.github.josemodi97:pesaflow4j-jakarta:$pesaflow4jVersionForTests")
    testImplementation("io.github.josemodi97:pesaflow4j-spring-boot2-starter:$pesaflow4jVersionForTests")
    testImplementation("io.github.josemodi97:pesaflow4j-spring-boot3-starter:$pesaflow4jVersionForTests")
}

gradlePlugin {
    plugins {
        create("pesaflow4j") {
            id = "io.github.josemodi97.pesaflow4j"
            implementationClass = "io.github.josemodi97.pesaflow4j.gradle.Pesaflow4jPlugin"
            displayName = "PesaFlow4J"
            description = project.description
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named("test") {
    dependsOn(tasks.named("pluginUnderTestMetadata"))
}
