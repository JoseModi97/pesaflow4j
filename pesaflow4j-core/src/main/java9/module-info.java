/**
 * Packaged as a Java 9+ module descriptor via a multi-release jar (this file
 * lives under {@code src/main/java9}, compiled at {@code --release 9} and
 * shaded into {@code META-INF/versions/9/} — the base jar entries stay
 * Java-8-only, since Java 8 doesn't understand multi-release jars and
 * simply ignores the versioned folder).
 *
 * <p>Only the public API packages are exported; {@code .internal} stays
 * encapsulated even from module-path consumers.
 */
module io.github.josemodi97.pesaflow4j.core {
    exports io.github.josemodi97.pesaflow4j;
    exports io.github.josemodi97.pesaflow4j.model;
    exports io.github.josemodi97.pesaflow4j.exception;
    exports io.github.josemodi97.pesaflow4j.util;
}
