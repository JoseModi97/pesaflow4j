plugins {
    java
}

allprojects {
    group = "io.github.josemodi97"
    version = project.findProperty("pesaflow4jVersion") as String? ?: "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    // pesaflow4j-bom is a java-platform (BOM) project, which cannot carry
    // the java/java-library plugin, so it applies its own plugins and opts
    // out of this shared block instead of auto-applying "java" here.
    if (name == "pesaflow4j-bom") {
        return@subprojects
    }

    apply(plugin = "java")

    tasks.withType<JavaCompile>().configureEach {
        // Compile against the Java 8 API surface regardless of which JDK
        // runs the build, so the published jar stays usable on Java 8+
        // without needing a real JDK 8 installed on this machine or in CI.
        options.release.set(8)
        options.encoding = "UTF-8"
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            encoding = "UTF-8"
            charSet = "UTF-8"
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
