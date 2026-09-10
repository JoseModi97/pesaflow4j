package io.github.josemodi97.pesaflow4j.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class FrameworkDetectorTest {

    private static Project javaProject() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("java");
        return project;
    }

    @Test
    void detectsPlainWhenNoRelevantDependency() {
        Project project = javaProject();
        project.getDependencies().add("implementation", "junit:junit:4.13.2");

        assertEquals("plain", FrameworkDetector.detect(project).framework);
    }

    @Test
    void detectsServletFromJavaxServletApi() {
        Project project = javaProject();
        project.getDependencies().add("implementation", "javax.servlet:javax.servlet-api:4.0.1");

        assertEquals("servlet", FrameworkDetector.detect(project).framework);
    }

    @Test
    void detectsJakartaFromJakartaServletApi() {
        Project project = javaProject();
        project.getDependencies().add("implementation", "jakarta.servlet:jakarta.servlet-api:6.0.0");

        assertEquals("jakarta", FrameworkDetector.detect(project).framework);
    }

    @Test
    void detectsSpringBoot3FromExplicitStarterVersion() {
        Project project = javaProject();
        project.getDependencies().add("implementation", "org.springframework.boot:spring-boot-starter-web:3.3.5");

        assertEquals("spring-boot3", FrameworkDetector.detect(project).framework);
    }

    @Test
    void detectsSpringBoot2FromExplicitStarterVersion() {
        Project project = javaProject();
        project.getDependencies().add("implementation", "org.springframework.boot:spring-boot-starter-web:2.7.18");

        assertEquals("spring-boot2", FrameworkDetector.detect(project).framework);
    }

    @Test
    void usesServletNamespaceAsTiebreakerWhenSpringBootVersionIsUnmanagedByABom() {
        Project project = javaProject();
        // No explicit version, as when the org.springframework.boot plugin
        // manages it via its own BOM:
        project.getDependencies().add("implementation", "org.springframework.boot:spring-boot-starter-web");
        project.getDependencies().add("implementation", "jakarta.servlet:jakarta.servlet-api:6.0.0");

        assertEquals("spring-boot3", FrameworkDetector.detect(project).framework);
    }

    @Test
    void detectsEmptyProjectAsPlain() {
        Project project = javaProject();
        assertEquals("plain", FrameworkDetector.detect(project).framework);
    }
}
