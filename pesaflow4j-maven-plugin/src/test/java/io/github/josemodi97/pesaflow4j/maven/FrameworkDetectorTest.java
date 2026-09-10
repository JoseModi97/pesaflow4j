package io.github.josemodi97.pesaflow4j.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

class FrameworkDetectorTest {

    private static MavenProject projectWith(Dependency... dependencies) {
        Model model = new Model();
        model.setDependencies(new ArrayList<>(Arrays.asList(dependencies)));
        return new MavenProject(model);
    }

    private static Dependency dependency(String groupId, String artifactId, String version) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        return dependency;
    }

    @Test
    void detectsPlainWhenNoRelevantDependency() {
        MavenProject project = projectWith(dependency("junit", "junit", "4.13.2"));
        assertEquals("plain", FrameworkDetector.detect(project).framework);
    }

    @Test
    void detectsServletFromJavaxServletApi() {
        MavenProject project = projectWith(dependency("javax.servlet", "javax.servlet-api", "4.0.1"));
        assertEquals("servlet", FrameworkDetector.detect(project).framework);
    }

    @Test
    void detectsJakartaFromJakartaServletApi() {
        MavenProject project = projectWith(dependency("jakarta.servlet", "jakarta.servlet-api", "6.0.0"));
        assertEquals("jakarta", FrameworkDetector.detect(project).framework);
    }

    @Test
    void detectsSpringBoot3FromExplicitStarterVersion() {
        MavenProject project = projectWith(
                dependency("org.springframework.boot", "spring-boot-starter-web", "3.3.5"));
        assertEquals("spring-boot3", FrameworkDetector.detect(project).framework);
    }

    @Test
    void detectsSpringBoot2FromExplicitStarterVersion() {
        MavenProject project = projectWith(
                dependency("org.springframework.boot", "spring-boot-starter-web", "2.7.18"));
        assertEquals("spring-boot2", FrameworkDetector.detect(project).framework);
    }

    @Test
    void usesServletNamespaceAsTiebreakerWhenSpringBootVersionIsManagedElsewhere() {
        MavenProject project = projectWith(
                dependency("org.springframework.boot", "spring-boot-starter-web", null),
                dependency("jakarta.servlet", "jakarta.servlet-api", "6.0.0"));
        assertEquals("spring-boot3", FrameworkDetector.detect(project).framework);
    }

    @Test
    void detectsSpringBootFromAParentSpringBootStarterParent() {
        MavenProject project = projectWith();
        MavenProject parent = projectWith();
        parent.setArtifactId("spring-boot-starter-parent");
        parent.setVersion("3.3.5");
        project.setParent(parent);

        assertEquals("spring-boot3", FrameworkDetector.detect(project).framework);
    }
}
