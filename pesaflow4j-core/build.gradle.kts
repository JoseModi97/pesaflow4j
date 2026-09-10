plugins {
    `java-library`
    `maven-publish`
    signing
}

description = "PesaFlow4J - a dependency-free Java SDK for signing, submitting, and verifying " +
        "Kenya PesaFlow / eCitizen PaymentAPI checkouts and webhook callbacks. Java 8+."

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    withJavadocJar()
    withSourcesJar()
}

// --- Multi-release jar: a real module-info.java (Java 9+) and an
// HttpClient-based HttpTransport (Java 11+), layered on top of the
// Java-8-only base classes below. See PLAN.md SS3 for why this is done as
// two extra source sets rather than raising the whole module's floor.
sourceSets {
    create("java9") {
        java {
            srcDir("src/main/java9")
        }
    }
    create("java11") {
        java {
            srcDir("src/main/java11")
        }
    }
}

dependencies {
    // module-info.java's `exports` clauses need the main classes compiled
    // and on the classpath to validate against; the java11 HttpTransport
    // variant needs them to resolve Pesaflow4jTransportException etc.
    "java9Implementation"(files(sourceSets["main"].output))
    "java11Implementation"(files(sourceSets["main"].output))
}

tasks.named<JavaCompile>("compileJava9Java") {
    options.release.set(9)
    dependsOn(tasks.named("compileJava"))
    // A module-info.java compiled on its own (separately from the package
    // sources it describes) needs its `exports` clauses validated against
    // those packages as if they belonged to this compilation, not as an
    // external classpath dependency - that's what --patch-module does.
    doFirst {
        options.compilerArgs.addAll(listOf(
            "--patch-module", "io.github.josemodi97.pesaflow4j.core=${sourceSets["main"].output.asPath}"
        ))
    }
}

tasks.named<JavaCompile>("compileJava11Java") {
    options.release.set(11)
    dependsOn(tasks.named("compileJava"))
}

tasks.jar {
    dependsOn("compileJava9Java", "compileJava11Java")

    into("META-INF/versions/9") {
        from(sourceSets["java9"].output)
    }
    into("META-INF/versions/11") {
        from(sourceSets["java11"].output)
    }

    manifest {
        attributes(
            "Multi-Release" to "true",
            "Implementation-Title" to "PesaFlow4J",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "PesaFlow4J contributors"
        )
    }
}

apply(from = "${rootDir}/gradle/publishing.gradle.kts")
