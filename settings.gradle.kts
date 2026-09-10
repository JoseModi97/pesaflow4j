rootProject.name = "pesaflow4j"

include("pesaflow4j-core")
include("pesaflow4j-servlet")
include("pesaflow4j-jakarta")
include("pesaflow4j-spring-boot2-starter")
include("pesaflow4j-spring-boot3-starter")
include("pesaflow4j-cli")
include("pesaflow4j-bom")

// Build-tool plugins are standalone Gradle builds (a Gradle plugin project
// cannot sanely be a subproject of the thing it builds) — see
// pesaflow4j-gradle-plugin/ and pesaflow4j-maven-plugin/ directly.
