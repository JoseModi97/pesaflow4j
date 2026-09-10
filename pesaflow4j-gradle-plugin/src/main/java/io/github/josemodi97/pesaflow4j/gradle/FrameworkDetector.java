package io.github.josemodi97.pesaflow4j.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

/**
 * Best-effort detection of which framework {@link Pesaflow4jInitTask}
 * should scaffold an example for, based on the consumer project's declared
 * dependencies — used when {@code framework} is left at its default of
 * {@code auto} rather than set explicitly.
 *
 * <p>This inspects declared (unresolved) dependency notations across every
 * configuration — it never triggers dependency resolution — plus whether
 * the {@code org.springframework.boot} plugin is applied. It's a
 * heuristic, not a guarantee: a project pulling something in an unusual
 * way won't be detected, and {@link #detect(Project)} falls back to
 * {@code "plain"} in that case. Always logs what it found (or didn't) so
 * the guess is never silent — see {@link Pesaflow4jInitTask#run()}.
 *
 * <p>Kept in exact logic-parity with the Maven plugin's identically-named
 * class (a separate, standalone Gradle build can't share Java source with
 * the Maven reactor).
 */
final class FrameworkDetector {

    private FrameworkDetector() {
    }

    static final class Result {
        final String framework;
        final String reason;

        Result(String framework, String reason) {
            this.framework = framework;
            this.reason = reason;
        }
    }

    static Result detect(Project project) {
        boolean hasJakartaServlet = false;
        boolean hasJavaxServlet = false;
        boolean hasSpringBoot = project.getPluginManager().hasPlugin("org.springframework.boot");
        String springBootMajor = null;

        for (Configuration configuration : project.getConfigurations()) {
            for (Dependency dependency : configuration.getDependencies()) {
                String group = dependency.getGroup();
                String name = dependency.getName();
                if ("jakarta.servlet".equals(group) && "jakarta.servlet-api".equals(name)) {
                    hasJakartaServlet = true;
                }
                if ("javax.servlet".equals(group) && "javax.servlet-api".equals(name)) {
                    hasJavaxServlet = true;
                }
                if ("org.springframework.boot".equals(group)) {
                    hasSpringBoot = true;
                    String version = dependency.getVersion();
                    if (springBootMajor == null && version != null && !version.trim().isEmpty()) {
                        springBootMajor = majorVersionOf(version);
                    }
                }
            }
        }

        if (hasSpringBoot) {
            if ("3".equals(springBootMajor)) {
                return new Result("spring-boot3", "found a Spring Boot 3.x dependency");
            }
            if ("2".equals(springBootMajor)) {
                return new Result("spring-boot2", "found a Spring Boot 2.x dependency");
            }
            // The org.springframework.boot Gradle plugin manages starter
            // versions via a BOM, so individual dependency declarations
            // often have no explicit version - use the servlet namespace
            // as a tiebreaker, since Boot 3 always pulls jakarta.servlet
            // and Boot 2 always pulls javax.servlet.
            if (hasJakartaServlet) {
                return new Result("spring-boot3", "found Spring Boot plus jakarta.servlet (Boot 3's namespace)");
            }
            if (hasJavaxServlet) {
                return new Result("spring-boot2", "found Spring Boot plus javax.servlet (Boot 2's namespace)");
            }
            return new Result("spring-boot3",
                    "found Spring Boot but couldn't tell 2.x from 3.x - defaulting to the current major");
        }

        if (hasJakartaServlet) {
            return new Result("jakarta", "found a jakarta.servlet-api dependency");
        }
        if (hasJavaxServlet) {
            return new Result("servlet", "found a javax.servlet-api dependency");
        }

        return new Result("plain", "no Spring Boot or servlet-api dependency found");
    }

    private static String majorVersionOf(String version) {
        String trimmed = version.trim();
        int dot = trimmed.indexOf('.');
        return dot > 0 ? trimmed.substring(0, dot) : trimmed;
    }
}
