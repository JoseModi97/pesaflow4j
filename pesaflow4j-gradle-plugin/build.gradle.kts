plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("com.gradle.plugin-publish") version "2.1.1"
}

group = "io.github.josemodi97"
// Single source of truth for "what version of the main reactor's modules
// does this standalone build's own version - and its test dependencies
// below - line up with". Pass -PpesaflowVersion=X.Y.Z to override; the
// fallback must be kept in sync with the root reactor's current version
// (see pom.xml) since the two are released independently. Previously this
// drifted (a separate hardcoded "0.1.0-SNAPSHOT" test-only constant vs.
// this property), which broke CI after the 0.1.0 release: mavenLocal only
// had pesaflow4j-core:0.1.0, not the stale :0.1.0-SNAPSHOT these tests
// asked for.
val pesaflow4jReactorVersion = project.findProperty("pesaflow4jVersion") as String? ?: "0.1.0"
version = pesaflow4jReactorVersion

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
    testImplementation("io.github.josemodi97:pesaflow4j-core:$pesaflow4jReactorVersion")
    testImplementation("io.github.josemodi97:pesaflow4j-servlet:$pesaflow4jReactorVersion")
    testImplementation("io.github.josemodi97:pesaflow4j-jakarta:$pesaflow4jReactorVersion")
    testImplementation("io.github.josemodi97:pesaflow4j-spring-boot2-starter:$pesaflow4jReactorVersion")
    testImplementation("io.github.josemodi97:pesaflow4j-spring-boot3-starter:$pesaflow4jReactorVersion")
}

gradlePlugin {
    // Required by com.gradle.plugin-publish for the Plugin Portal listing.
    website.set("https://github.com/JoseModi97/pesaflow4j")
    vcsUrl.set("https://github.com/JoseModi97/pesaflow4j")

    plugins {
        create("pesaflow4j") {
            id = "io.github.josemodi97.pesaflow4j"
            implementationClass = "io.github.josemodi97.pesaflow4j.gradle.Pesaflow4jPlugin"
            displayName = "PesaFlow4J"
            description = project.description
            // Keyword tags are how the Plugin Portal's own search surfaces
            // this plugin - the same discoverability goal as the Maven
            // Central SEO checklist in PLAN.md SS7, applied to this portal.
            tags.set(listOf(
                "pesaflow", "ecitizen", "kenya", "payment-gateway", "mpesa",
                "stk-push", "fintech", "mobile-money", "sdk", "scaffolding",
                "safaricom", "payments"
            ))
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
