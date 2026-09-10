package io.github.josemodi97.pesaflow4j.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Pesaflow4jInitMojoTest {

    @TempDir
    Path projectDir;

    private Pesaflow4jInitMojo mojoFor(String framework) {
        Pesaflow4jInitMojo mojo = new Pesaflow4jInitMojo();
        mojo.propertiesFile = projectDir.resolve("src/main/resources/pesaflow4j.properties").toFile();
        mojo.javaSourceDirectory = projectDir.resolve("src/main/java").toFile();
        mojo.framework = framework;
        mojo.project = new MavenProject();
        return mojo;
    }

    @Test
    void plainFrameworkOnlyWritesThePropertiesFile() throws MojoExecutionException {
        Pesaflow4jInitMojo mojo = mojoFor("plain");
        mojo.execute();

        assertTrue(mojo.propertiesFile.exists());
        assertFalse(new File(mojo.javaSourceDirectory, "pesaflow4j/PesaflowNotifyServlet.java").exists());
        assertFalse(new File(mojo.javaSourceDirectory, "pesaflow4j/PesaflowPaymentListener.java").exists());
    }

    @Test
    void rejectsAnUnsupportedFramework() {
        Pesaflow4jInitMojo mojo = mojoFor("quarkus");
        assertTrue(org.junit.jupiter.api.Assertions.assertThrows(MojoExecutionException.class, mojo::execute)
                .getMessage().contains("Unsupported"));
    }

    @Test
    void servletFrameworkGeneratesAWorkingServletAgainstJavaxServletApi() throws Exception {
        Pesaflow4jInitMojo mojo = mojoFor("servlet");
        mojo.execute();

        File example = new File(mojo.javaSourceDirectory, "pesaflow4j/PesaflowNotifyServlet.java");
        assertTrue(example.exists());
        assertCompilesCleanly(example);
    }

    @Test
    void jakartaFrameworkGeneratesAWorkingServletAgainstJakartaServletApi() throws Exception {
        Pesaflow4jInitMojo mojo = mojoFor("jakarta");
        mojo.execute();

        File example = new File(mojo.javaSourceDirectory, "pesaflow4j/PesaflowNotifyServlet.java");
        assertTrue(example.exists());
        assertCompilesCleanly(example);
    }

    @Test
    void springBoot2FrameworkGeneratesAWorkingListenerAndWebhookProperties() throws Exception {
        Pesaflow4jInitMojo mojo = mojoFor("spring-boot2");
        mojo.execute();

        File example = new File(mojo.javaSourceDirectory, "pesaflow4j/PesaflowPaymentListener.java");
        assertTrue(example.exists());
        assertCompilesCleanly(example);

        String properties = new String(Files.readAllBytes(mojo.propertiesFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(properties.contains("pesaflow4j.webhook.enabled=true"));
    }

    @Test
    void springBoot3FrameworkGeneratesAWorkingListenerAndWebhookProperties() throws Exception {
        Pesaflow4jInitMojo mojo = mojoFor("spring-boot3");
        mojo.execute();

        File example = new File(mojo.javaSourceDirectory, "pesaflow4j/PesaflowPaymentListener.java");
        assertTrue(example.exists());
        assertCompilesCleanly(example);

        String properties = new String(Files.readAllBytes(mojo.propertiesFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(properties.contains("pesaflow4j.webhook.enabled=true"));
    }

    @Test
    void autoFrameworkDetectsJakartaFromProjectDependenciesAndGeneratesAWorkingExample() throws Exception {
        Pesaflow4jInitMojo mojo = mojoFor("auto");
        Dependency jakartaServlet = new Dependency();
        jakartaServlet.setGroupId("jakarta.servlet");
        jakartaServlet.setArtifactId("jakarta.servlet-api");
        jakartaServlet.setVersion("6.0.0");
        Model model = new Model();
        model.setDependencies(new ArrayList<>(Collections.singletonList(jakartaServlet)));
        mojo.project = new MavenProject(model);
        mojo.propertiesFile = projectDir.resolve("src/main/resources/pesaflow4j.properties").toFile();
        mojo.javaSourceDirectory = projectDir.resolve("src/main/java").toFile();

        mojo.execute();

        File example = new File(mojo.javaSourceDirectory, "pesaflow4j/PesaflowNotifyServlet.java");
        assertTrue(example.exists());
        assertTrue(sourceContent(example).contains("jakarta.servlet"));
        assertCompilesCleanly(example);
    }

    @Test
    void autoFrameworkFallsBackToPlainWhenNothingIsDetected() throws MojoExecutionException {
        Pesaflow4jInitMojo mojo = mojoFor("auto");
        mojo.execute();

        assertTrue(mojo.propertiesFile.exists());
        assertFalse(new File(mojo.javaSourceDirectory, "pesaflow4j/PesaflowNotifyServlet.java").exists());
        assertFalse(new File(mojo.javaSourceDirectory, "pesaflow4j/PesaflowPaymentListener.java").exists());
    }

    @Test
    void doesNotOverwriteAnExistingExampleFile() throws MojoExecutionException, IOException {
        Pesaflow4jInitMojo mojo = mojoFor("servlet");
        File example = new File(mojo.javaSourceDirectory, "pesaflow4j/PesaflowNotifyServlet.java");
        Files.createDirectories(example.getParentFile().toPath());
        Files.write(example.toPath(), "// hand-written, keep me\n".getBytes(StandardCharsets.UTF_8));

        mojo.execute();

        String content = new String(Files.readAllBytes(example.toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("hand-written, keep me"));
    }

    /**
     * Compiles the generated file for real against this test's own runtime
     * classpath (which already carries pesaflow4j-servlet, pesaflow4j-jakarta,
     * both Spring Boot starters, and the relevant servlet-api jars as test
     * dependencies - see pom.xml) - proving the generated source is actually
     * correct Java against the real API, not just plausible-looking text.
     */
    private static void assertCompilesCleanly(File sourceFile) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // sourceFile is <javaSourceDirectory>/pesaflow4j/Foo.java, so its
        // grandparent is the source root the compiled .class package structure
        // should be rooted at.
        File outputDir = sourceFile.getParentFile().getParentFile();
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();

        int result = compiler.run(null, diagnostics, diagnostics,
                "-cp", System.getProperty("java.class.path"),
                "-d", outputDir.getAbsolutePath(),
                sourceFile.getAbsolutePath());

        assertEquals(0, result, "Generated file failed to compile:\n" + diagnostics
                + "\n\n--- source ---\n" + sourceContent(sourceFile));
    }

    private static String sourceContent(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<unreadable: " + e.getMessage() + ">";
        }
    }
}
