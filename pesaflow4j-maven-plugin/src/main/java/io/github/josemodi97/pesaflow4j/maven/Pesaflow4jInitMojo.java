package io.github.josemodi97.pesaflow4j.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Scaffolds a {@code pesaflow4j.properties} placeholder credentials file
 * into the project, mirroring what a project-scaffolding CLI's {@code init}
 * command would do — without overwriting files that already exist.
 *
 * <p>By default ({@code pesaflow4j.framework=auto}), the integration
 * example to also generate is auto-detected from the project's declared
 * dependencies (see {@link FrameworkDetector}) — no flag needed for the
 * common case. Override explicitly when the guess is wrong or you want a
 * specific target:
 *
 * <pre>{@code
 * mvn io.github.josemodi97:pesaflow4j-maven-plugin:init -Dpesaflow4j.framework=spring-boot3
 * }</pre>
 *
 * <p>Supported values: {@code auto} (default), {@code plain} (properties
 * file only, no example), {@code servlet} ({@code javax.servlet}),
 * {@code jakarta} ({@code jakarta.servlet}), {@code spring-boot2},
 * {@code spring-boot3}.
 */
@Mojo(name = "init", requiresProject = true, threadSafe = true)
public class Pesaflow4jInitMojo extends AbstractMojo {

    private static final Set<String> SUPPORTED_FRAMEWORKS = new LinkedHashSet<>(
            Arrays.asList("plain", "servlet", "jakarta", "spring-boot2", "spring-boot3"));

    static final String BASE_TEMPLATE =
            "# PesaFlow4J credentials - fill these in.\n"
                    + "# Load with Pesaflow4jConfig.builder()...build(), or bind pesaflow4j.* directly\n"
                    + "# if you're using a pesaflow4j-spring-boot2-starter / pesaflow4j-spring-boot3-starter.\n"
                    + "# See: https://github.com/JoseModi97/pesaflow4j#quickstart\n"
                    + "pesaflow4j.apiClientId=\n"
                    + "pesaflow4j.apiKey=\n"
                    + "pesaflow4j.secret=\n"
                    + "pesaflow4j.serviceId=\n"
                    + "pesaflow4j.currency=KES\n";

    static final String WEBHOOK_PROPERTIES =
            "pesaflow4j.webhook.enabled=true\n"
                    + "pesaflow4j.webhook.path=/pesaflow4j/notify\n";

    /** Where to write the placeholder credentials file. */
    @Parameter(property = "pesaflow4j.propertiesFile",
            defaultValue = "${project.basedir}/src/main/resources/pesaflow4j.properties")
    File propertiesFile;

    /**
     * Which integration example to also generate, in addition to the
     * properties file: {@code auto} (detect from the project's
     * dependencies), {@code plain}, {@code servlet}, {@code jakarta},
     * {@code spring-boot2}, or {@code spring-boot3}.
     */
    @Parameter(property = "pesaflow4j.framework", defaultValue = "auto")
    String framework;

    /** Base directory example Java sources are written under. */
    @Parameter(property = "pesaflow4j.javaSourceDirectory",
            defaultValue = "${project.basedir}/src/main/java")
    File javaSourceDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        String requested = framework == null ? "auto" : framework.trim().toLowerCase(Locale.ROOT);
        String normalizedFramework;

        if ("auto".equals(requested)) {
            FrameworkDetector.Result detected = FrameworkDetector.detect(project);
            normalizedFramework = detected.framework;
            getLog().info("Auto-detected framework '" + normalizedFramework + "' (" + detected.reason
                    + "). Pass -Dpesaflow4j.framework=<value> to override.");
        } else {
            normalizedFramework = requested;
        }

        if (!SUPPORTED_FRAMEWORKS.contains(normalizedFramework)) {
            throw new MojoExecutionException("Unsupported pesaflow4j.framework '" + framework
                    + "'. Supported values: auto, " + SUPPORTED_FRAMEWORKS);
        }

        writeIfAbsent(propertiesFile, BASE_TEMPLATE + webhookPropertiesFor(normalizedFramework));

        FrameworkExample example = FrameworkExample.forFramework(normalizedFramework);
        if (example != null) {
            File exampleFile = new File(javaSourceDirectory, example.relativePath);
            writeIfAbsent(exampleFile, example.content);
        }

        getLog().info("Fill in your PesaFlow credentials, then see "
                + "https://github.com/JoseModi97/pesaflow4j#quickstart to build a Pesaflow4jClient.");
    }

    private static String webhookPropertiesFor(String normalizedFramework) {
        return ("spring-boot2".equals(normalizedFramework) || "spring-boot3".equals(normalizedFramework))
                ? WEBHOOK_PROPERTIES : "";
    }

    private void writeIfAbsent(File file, String content) throws MojoExecutionException {
        if (file.exists()) {
            getLog().info(file + " already exists - leaving it untouched.");
            return;
        }
        try {
            Files.createDirectories(file.getParentFile().toPath());
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write " + file, e);
        }
        getLog().info("Wrote " + file);
    }
}
