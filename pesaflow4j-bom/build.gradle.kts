plugins {
    `java-platform`
    `maven-publish`
    signing
}

description = "Bill of materials for PesaFlow4J: import this to pin matching versions of every " +
        "PesaFlow4J module without repeating version numbers."

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":pesaflow4j-core"))
        api(project(":pesaflow4j-servlet"))
        api(project(":pesaflow4j-jakarta"))
        api(project(":pesaflow4j-spring-boot2-starter"))
        api(project(":pesaflow4j-spring-boot3-starter"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "pesaflow4j-bom"
            from(components["javaPlatform"])

            pom {
                name.set("pesaflow4j-bom")
                description.set(project.description)
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
