package io.github.josemodi97.pesaflow4j.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Compiles every generated {@link FrameworkExample} for real against this
 * test's own runtime classpath (which carries pesaflow4j-servlet,
 * pesaflow4j-jakarta, both Spring Boot starters, and the relevant
 * servlet-api jars as test dependencies - see build.gradle.kts) - proving
 * the generated source is actually correct Java against the real API, not
 * just plausible-looking text. Mirrors pesaflow4j-maven-plugin's identical
 * check on the Maven side.
 */
class FrameworkExampleCompileTest {

    @TempDir
    Path outputDir;

    @Test
    void plainFrameworkHasNoExample() {
        assertNull(FrameworkExample.forFramework("plain"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"servlet", "jakarta", "spring-boot2", "spring-boot3"})
    void generatedExampleCompilesCleanly(String framework) throws IOException {
        FrameworkExample example = FrameworkExample.forFramework(framework);
        assertNotNullExample(framework, example);

        Path sourceFile = outputDir.resolve(example.relativePath);
        Files.createDirectories(sourceFile.getParent());
        Files.write(sourceFile, example.content.getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();

        int result = compiler.run(null, diagnostics, diagnostics,
                "-cp", System.getProperty("java.class.path"),
                "-d", outputDir.toFile().getAbsolutePath(),
                sourceFile.toFile().getAbsolutePath());

        assertEquals(0, result, "Generated '" + framework + "' example failed to compile:\n" + diagnostics
                + "\n\n--- source ---\n" + example.content);
    }

    private static void assertNotNullExample(String framework, FrameworkExample example) {
        if (example == null) {
            throw new AssertionError("Expected a FrameworkExample for '" + framework + "'");
        }
    }
}
