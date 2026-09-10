import org.gradle.api.publish.maven.MavenPublication

// Shared Maven Central publishing configuration, applied by every publishable
// module's build.gradle.kts (`apply(from = "${rootDir}/gradle/publishing.gradle.kts")`)
// so the POM metadata Central requires isn't repeated per module.

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.name
            from(components["java"])

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
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/JoseModi97/pesaflow4j/issues")
                }
            }
        }
    }

    repositories {
        maven {
            name = "buildDir"
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
        // Sonatype Central Publishing Portal target — see PLAN.md
        // "Publishing to Maven Central" for the credentials/GPG setup.
    }
}

configure<SigningExtension> {
    setRequired({ gradle.taskGraph.hasTask("publish") && project.hasProperty("signing.keyId") })
    sign(extensions.getByType<PublishingExtension>().publications["mavenJava"])
}
