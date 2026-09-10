package io.github.josemodi97.pesaflow4j.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

/**
 * Registers PesaFlow4J's project-scaffolding tasks. Apply with:
 *
 * <pre>{@code
 * plugins {
 *     id("io.github.josemodi97.pesaflow4j") version "0.1.0"
 * }
 * }</pre>
 *
 * then run {@code ./gradlew pesaflow4jInit}. The framework it scaffolds an
 * example for is auto-detected from your declared dependencies (see
 * {@link FrameworkDetector}) unless you set it explicitly:
 *
 * <pre>{@code
 * tasks.named<Pesaflow4jInitTask>("pesaflow4jInit") {
 *     framework.set("spring-boot3")
 * }
 * }</pre>
 */
public class Pesaflow4jPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        TaskProvider<Pesaflow4jInitTask> initTask = project.getTasks().register("pesaflow4jInit", Pesaflow4jInitTask.class, task -> {
            task.setGroup("pesaflow4j");
            task.setDescription("Scaffolds a pesaflow4j.properties file with placeholder PesaFlow4J credentials.");
        });

        // Deferred to afterEvaluate: the consumer's `dependencies { ... }`
        // block (and any `plugins { id("org.springframework.boot") ... }`)
        // hasn't been evaluated yet at the point this plugin itself is
        // applied - just detecting at apply()-time would always see an
        // empty dependency set.
        project.afterEvaluate(p -> {
            FrameworkDetector.Result detected = FrameworkDetector.detect(p);
            initTask.configure(task -> task.getFramework().convention(detected.framework));
            // lifecycle, not info: Gradle hides INFO-level logging by
            // default (unlike Maven, where getLog().info(...) is shown by
            // default), and this message should be visible on a plain
            // `./gradlew pesaflow4jInit` run, not just with --info.
            p.getLogger().lifecycle("pesaflow4j: auto-detected framework '{}' ({}). "
                    + "Set framework.set(\"...\") on the pesaflow4jInit task to override.",
                    detected.framework, detected.reason);
        });
    }
}
