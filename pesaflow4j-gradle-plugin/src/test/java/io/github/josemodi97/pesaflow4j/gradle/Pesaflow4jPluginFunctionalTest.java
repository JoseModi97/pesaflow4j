package io.github.josemodi97.pesaflow4j.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Pesaflow4jPluginFunctionalTest {

    @TempDir
    Path projectDir;

    private void writeBuildFile(String taskConfig) throws IOException {
        Files.write(projectDir.resolve("settings.gradle.kts"),
                "rootProject.name = \"consumer\"\n".getBytes(StandardCharsets.UTF_8));
        Files.write(projectDir.resolve("build.gradle.kts"),
                ("plugins {\n"
                        + "    id(\"io.github.josemodi97.pesaflow4j\")\n"
                        + "}\n"
                        + taskConfig).getBytes(StandardCharsets.UTF_8));
    }

    private GradleRunner runner(String... args) throws IOException {
        return runnerWithConfig("", args);
    }

    private GradleRunner runnerWithConfig(String taskConfig, String... args) throws IOException {
        writeBuildFile(taskConfig);
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(args);
    }

    /** For tests that need a `dependencies { ... }` block, not just task config. */
    private GradleRunner runnerWithFullBuildFile(String buildFileContent, String... args) throws IOException {
        Files.write(projectDir.resolve("settings.gradle.kts"),
                "rootProject.name = \"consumer\"\n".getBytes(StandardCharsets.UTF_8));
        Files.write(projectDir.resolve("build.gradle.kts"), buildFileContent.getBytes(StandardCharsets.UTF_8));
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(args);
    }

    @Test
    void writesAPlaceholderPropertiesFile() throws IOException {
        BuildResult result = runner("pesaflow4jInit").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":pesaflow4jInit").getOutcome());

        Path written = projectDir.resolve("src/main/resources/pesaflow4j.properties");
        assertTrue(Files.exists(written));
        String content = new String(Files.readAllBytes(written), StandardCharsets.UTF_8);
        assertTrue(content.contains("pesaflow4j.apiClientId="));
        assertTrue(content.contains("pesaflow4j.currency=KES"));
    }

    @Test
    void doesNotOverwriteAnExistingPropertiesFile() throws IOException {
        Path existing = projectDir.resolve("src/main/resources/pesaflow4j.properties");
        Files.createDirectories(existing.getParent());
        Files.write(existing, "pesaflow4j.apiClientId=already-configured\n".getBytes(StandardCharsets.UTF_8));

        runner("pesaflow4jInit").build();

        String content = new String(Files.readAllBytes(existing), StandardCharsets.UTF_8);
        assertTrue(content.contains("already-configured"));
    }

    @Test
    void servletFrameworkWritesTheGeneratedServletExample() throws IOException {
        String taskConfig = "tasks.named<io.github.josemodi97.pesaflow4j.gradle.Pesaflow4jInitTask>(\"pesaflow4jInit\") {\n"
                + "    framework.set(\"servlet\")\n"
                + "}\n";

        BuildResult result = runnerWithConfig(taskConfig, "pesaflow4jInit").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":pesaflow4jInit").getOutcome());
        Path example = projectDir.resolve("src/main/java/pesaflow4j/PesaflowNotifyServlet.java");
        assertTrue(Files.exists(example));
        String content = new String(Files.readAllBytes(example), StandardCharsets.UTF_8);
        assertTrue(content.contains("Pesaflow4jServletWebhookHandler"));
    }

    @Test
    void springBoot3FrameworkAlsoAddsWebhookProperties() throws IOException {
        String taskConfig = "tasks.named<io.github.josemodi97.pesaflow4j.gradle.Pesaflow4jInitTask>(\"pesaflow4jInit\") {\n"
                + "    framework.set(\"spring-boot3\")\n"
                + "}\n";

        runnerWithConfig(taskConfig, "pesaflow4jInit").build();

        Path example = projectDir.resolve("src/main/java/pesaflow4j/PesaflowPaymentListener.java");
        assertTrue(Files.exists(example));
        assertTrue(new String(Files.readAllBytes(example), StandardCharsets.UTF_8).contains("spring.boot3"));

        Path properties = projectDir.resolve("src/main/resources/pesaflow4j.properties");
        assertTrue(new String(Files.readAllBytes(properties), StandardCharsets.UTF_8).contains("pesaflow4j.webhook.enabled=true"));
    }

    @Test
    void autoDetectsJakartaFromTheConsumerProjectsOwnDependencies() throws IOException {
        String buildFile = "plugins {\n"
                + "    java\n"
                + "    id(\"io.github.josemodi97.pesaflow4j\")\n"
                + "}\n"
                + "repositories { mavenCentral() }\n"
                + "dependencies {\n"
                + "    implementation(\"jakarta.servlet:jakarta.servlet-api:6.0.0\")\n"
                + "}\n";

        BuildResult result = runnerWithFullBuildFile(buildFile, "pesaflow4jInit").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":pesaflow4jInit").getOutcome());
        Path example = projectDir.resolve("src/main/java/pesaflow4j/PesaflowNotifyServlet.java");
        assertTrue(Files.exists(example));
        String content = new String(Files.readAllBytes(example), StandardCharsets.UTF_8);
        assertTrue(content.contains("jakarta.servlet"));
        assertTrue(result.getOutput().contains("auto-detected framework 'jakarta'"));
    }

    @Test
    void autoDetectionFallsBackToPlainWithNoRelevantDependencies() throws IOException {
        String buildFile = "plugins {\n"
                + "    java\n"
                + "    id(\"io.github.josemodi97.pesaflow4j\")\n"
                + "}\n";

        BuildResult result = runnerWithFullBuildFile(buildFile, "pesaflow4jInit").build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":pesaflow4jInit").getOutcome());
        assertFalse(Files.exists(projectDir.resolve("src/main/java/pesaflow4j/PesaflowNotifyServlet.java")));
        assertTrue(result.getOutput().contains("auto-detected framework 'plain'"));
    }

    @Test
    void failsOnAnUnsupportedFramework() throws IOException {
        String taskConfig = "tasks.named<io.github.josemodi97.pesaflow4j.gradle.Pesaflow4jInitTask>(\"pesaflow4jInit\") {\n"
                + "    framework.set(\"quarkus\")\n"
                + "}\n";

        BuildResult result = runnerWithConfig(taskConfig, "pesaflow4jInit").buildAndFail();

        assertFalse(result.getOutput().isEmpty());
        assertTrue(result.getOutput().contains("Unsupported framework"));
    }
}
