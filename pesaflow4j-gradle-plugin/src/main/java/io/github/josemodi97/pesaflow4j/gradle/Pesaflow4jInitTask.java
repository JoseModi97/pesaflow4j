package io.github.josemodi97.pesaflow4j.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * Scaffolds a {@code pesaflow4j.properties} placeholder credentials file
 * into the project, mirroring what a project-scaffolding CLI's {@code init}
 * command would do — without overwriting files that already exist.
 *
 * <p>Set {@code framework} to also generate a working integration example
 * for that target:
 *
 * <pre>{@code
 * tasks.named<Pesaflow4jInitTask>("pesaflow4jInit") {
 *     framework.set("spring-boot3")
 * }
 * }</pre>
 *
 * <p>Supported values: {@code plain} (default — properties file only),
 * {@code servlet} ({@code javax.servlet}), {@code jakarta}
 * ({@code jakarta.servlet}), {@code spring-boot2}, {@code spring-boot3}.
 */
public abstract class Pesaflow4jInitTask extends DefaultTask {

    private static final Set<String> SUPPORTED_FRAMEWORKS = new LinkedHashSet<>(
            Arrays.asList("plain", "servlet", "jakarta", "spring-boot2", "spring-boot3"));

    private static final String BASE_TEMPLATE =
            "# PesaFlow4J credentials - fill these in.\n"
                    + "# Load with Pesaflow4jConfig.builder()...build(), or bind pesaflow4j.* directly\n"
                    + "# if you're using a pesaflow4j-spring-boot2-starter / pesaflow4j-spring-boot3-starter.\n"
                    + "# See: https://github.com/JoseModi97/pesaflow4j#quickstart\n"
                    + "pesaflow4j.apiClientId=\n"
                    + "pesaflow4j.apiKey=\n"
                    + "pesaflow4j.secret=\n"
                    + "pesaflow4j.serviceId=\n"
                    + "pesaflow4j.currency=KES\n";

    private static final String WEBHOOK_PROPERTIES =
            "pesaflow4j.webhook.enabled=true\n"
                    + "pesaflow4j.webhook.path=/pesaflow4j/notify\n";

    @Inject
    public Pesaflow4jInitTask(ProjectLayout layout) {
        getPropertiesFile().convention(layout.getProjectDirectory().file("src/main/resources/pesaflow4j.properties"));
        getJavaSourceDirectory().convention(layout.getProjectDirectory().dir("src/main/java"));
        getFramework().convention("plain");
    }

    @OutputFile
    public abstract RegularFileProperty getPropertiesFile();

    /** Base directory example Java sources are written under. */
    @Internal
    public abstract DirectoryProperty getJavaSourceDirectory();

    /**
     * Which integration example to also generate, in addition to the
     * properties file: {@code plain}, {@code servlet}, {@code jakarta},
     * {@code spring-boot2}, or {@code spring-boot3}.
     */
    @Input
    public abstract Property<String> getFramework();

    @TaskAction
    public void run() throws IOException {
        String normalizedFramework = getFramework().get().trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_FRAMEWORKS.contains(normalizedFramework)) {
            throw new IllegalArgumentException("Unsupported framework '" + getFramework().get()
                    + "'. Supported values: " + SUPPORTED_FRAMEWORKS);
        }

        writeIfAbsent(getPropertiesFile().get().getAsFile(), BASE_TEMPLATE + webhookPropertiesFor(normalizedFramework));

        FrameworkExample example = FrameworkExample.forFramework(normalizedFramework);
        if (example != null) {
            File exampleFile = getJavaSourceDirectory().get().file(example.relativePath).getAsFile();
            writeIfAbsent(exampleFile, example.content);
        }

        getLogger().lifecycle("Fill in your PesaFlow credentials, then see "
                + "https://github.com/JoseModi97/pesaflow4j#quickstart to build a Pesaflow4jClient.");
    }

    private static String webhookPropertiesFor(String normalizedFramework) {
        return ("spring-boot2".equals(normalizedFramework) || "spring-boot3".equals(normalizedFramework))
                ? WEBHOOK_PROPERTIES : "";
    }

    private void writeIfAbsent(File file, String content) throws IOException {
        if (file.exists()) {
            getLogger().lifecycle("{} already exists - leaving it untouched.", file);
            return;
        }
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        getLogger().lifecycle("Wrote {}", file);
    }
}
