package io.github.josemodi97.pesaflow4j.maven;

import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;

/**
 * Best-effort detection of which framework {@link Pesaflow4jInitMojo}
 * should scaffold an example for, based on the invoking project's declared
 * dependencies — used when {@code pesaflow4j.framework} is left at its
 * default of {@code auto} rather than set explicitly.
 *
 * <p>This inspects declared (unresolved) {@code <dependencies>} only — it
 * never triggers dependency resolution — plus, for Maven, the parent POM
 * when it looks like {@code spring-boot-starter-parent}. It's a heuristic,
 * not a guarantee: a project pulling Spring Boot in some other way (a BOM
 * import with no direct starter dependency, for instance) won't be
 * detected, and {@link #detect(MavenProject)} falls back to {@code "plain"}
 * in that case. Always logs what it found (or didn't) so the guess is
 * never silent — see {@link Pesaflow4jInitMojo#execute()}.
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

    static Result detect(MavenProject project) {
        List<Dependency> dependencies = project.getDependencies();

        boolean hasJakartaServlet = hasDependency(dependencies, "jakarta.servlet", "jakarta.servlet-api");
        boolean hasJavaxServlet = hasDependency(dependencies, "javax.servlet", "javax.servlet-api");

        String springBootMajor = null;
        boolean hasSpringBoot = false;
        for (Dependency dependency : dependencies) {
            if ("org.springframework.boot".equals(dependency.getGroupId())) {
                hasSpringBoot = true;
                String version = dependency.getVersion();
                if (springBootMajor == null && version != null && !version.trim().isEmpty()) {
                    springBootMajor = majorVersionOf(version);
                }
            }
        }
        if (springBootMajor == null && project.getParent() != null
                && "spring-boot-starter-parent".equals(project.getParent().getArtifactId())) {
            hasSpringBoot = true;
            springBootMajor = majorVersionOf(project.getParent().getVersion());
        }

        if (hasSpringBoot) {
            if ("3".equals(springBootMajor)) {
                return new Result("spring-boot3", "found a Spring Boot 3.x dependency/parent");
            }
            if ("2".equals(springBootMajor)) {
                return new Result("spring-boot2", "found a Spring Boot 2.x dependency/parent");
            }
            // A Spring Boot dependency exists but its major version isn't
            // directly visible (e.g. version managed elsewhere) - use the
            // servlet namespace as a tiebreaker, since Boot 3 always pulls
            // jakarta.servlet and Boot 2 always pulls javax.servlet.
            if (hasJakartaServlet) {
                return new Result("spring-boot3", "found Spring Boot plus jakarta.servlet (Boot 3's namespace)");
            }
            if (hasJavaxServlet) {
                return new Result("spring-boot2", "found Spring Boot plus javax.servlet (Boot 2's namespace)");
            }
            return new Result("spring-boot3",
                    "found a Spring Boot dependency but couldn't tell 2.x from 3.x - defaulting to the current major");
        }

        if (hasJakartaServlet) {
            return new Result("jakarta", "found a jakarta.servlet-api dependency");
        }
        if (hasJavaxServlet) {
            return new Result("servlet", "found a javax.servlet-api dependency");
        }

        return new Result("plain", "no Spring Boot or servlet-api dependency found");
    }

    private static boolean hasDependency(List<Dependency> dependencies, String groupId, String artifactId) {
        for (Dependency dependency : dependencies) {
            if (groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    private static String majorVersionOf(String version) {
        if (version == null || version.trim().isEmpty()) {
            return null;
        }
        String trimmed = version.trim();
        int dot = trimmed.indexOf('.');
        return dot > 0 ? trimmed.substring(0, dot) : trimmed;
    }
}
