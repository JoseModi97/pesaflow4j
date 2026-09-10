plugins {
    application
    id("com.gradleup.shadow") version "8.3.5"
    id("org.graalvm.buildtools.native") version "0.10.3"
    `maven-publish`
    signing
}

description = "PesaFlow4J command-line tool: build/submit signed checkouts, poll settlement status, " +
        "and verify captured webhook payloads without writing any application code."

val mainClassName = "io.github.josemodi97.pesaflow4j.cli.Pesaflow4jCli"

dependencies {
    implementation(project(":pesaflow4j-core"))
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set(mainClassName)
}

// picocli's annotation processor, given -Aproject=true, generates the
// GraalVM reflection/resource config (META-INF/native-image/**) that this
// CLI's @Command classes need for native-image compilation - without this,
// native-image would silently drop the reflective command/option wiring
// picocli relies on. See https://picocli.info/#_graalvm_native_image
tasks.compileJava {
    options.compilerArgs.add("-Aproject=true")
}

// GraalVM native-image compilation itself needs an actual GraalVM
// distribution (with native-image installed) as the build JDK, plus - on
// Windows - the MSVC linker; neither is set up in every environment, so
// this configures `./gradlew nativeCompile` for whoever *does* have GraalVM
// installed rather than assuming it's present. CI builds and smoke-tests
// the real binary on a GraalVM JDK (see .github/workflows/ci.yml).
graalvmNative {
    // The plugin's "run my JUnit tests as a native image too" support pulls
    // in org.graalvm.buildtools:junit-platform-native, which requires a
    // Java 11+ compile target - incompatible with this reactor's Java 8
    // floor (options.release.set(8), applied project-wide in the root
    // build.gradle.kts). We only need native-image *compilation* of the
    // CLI binary here, not native-image *test execution*, so this is
    // disabled rather than raising the whole module's Java floor for it.
    testSupport.set(false)

    binaries {
        named("main") {
            imageName.set("pesaflow4j")
            mainClass.set(mainClassName)
            buildArgs.add("--no-fallback")
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

// Keep the shadow jar's default "-all" classifier (rather than overwriting
// the plain jar's output path) so it doesn't collide with the `application`
// plugin's distTar/distZip/startScripts tasks, which also consume the
// plain jar's output.
tasks.shadowJar {
    manifest {
        attributes("Main-Class" to "io.github.josemodi97.pesaflow4j.cli.Pesaflow4jCli")
    }
}

tasks.jar {
    manifest {
        attributes(
            "Automatic-Module-Name" to "io.github.josemodi97.pesaflow4j.cli",
            "Main-Class" to "io.github.josemodi97.pesaflow4j.cli.Pesaflow4jCli"
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.name
            artifact(tasks.shadowJar)
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))

            pom {
                name.set(project.name)
                description.set(project.description ?: project.name)
                url.set("https://github.com/JoseModi97/pesaflow4j")
                inceptionYear.set("2026")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("josemodi97")
                        name.set("Jose Modi")
                        url.set("https://github.com/JoseModi97")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/JoseModi97/pesaflow4j.git")
                    developerConnection.set("scm:git:ssh://git@github.com/JoseModi97/pesaflow4j.git")
                    url.set("https://github.com/JoseModi97/pesaflow4j")
                }
            }
        }
    }

    repositories {
        maven {
            name = "buildDir"
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}
