/*
 * Copyright 2023 Creek Contributors (https://github.com/creek-service)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.creekservice.kafka.test.perf.util;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;

/** Grabs metadata about an Implementation's jar file. */
public final class ImplJarFile {

    private static final String GRADLE_CACHE_PATH = ".gradle" + File.separator + "caches";

    private final long jarSize;
    private final String jarVersion;
    private final String minJavaVersion;

    /**
     * Grab metadata about an implementations jar file.
     *
     * @param type a single type from the implementation's main jar.
     * @throws IllegalArgumentException if the supplied {@code type} isn't loaded from an accessible
     *     jar.
     */
    public ImplJarFile(final Class<?> type) {
        this(type, ImplJarFile::sourceCode);
    }

    @VisibleForTesting
    ImplJarFile(final Class<?> type, final Function<Class<?>, CodeSource> locator) {
        final Path location = location(requireNonNull(type, "type"), locator);
        this.jarSize = jarSize(location);
        this.jarVersion = jarVersion(type, location);
        this.minJavaVersion = minJavaVersion(type, location);
    }

    public long jarSize() {
        return jarSize;
    }

    public String jarVersion() {
        return jarVersion;
    }

    public String minJavaVersion() {
        return minJavaVersion;
    }

    private static CodeSource sourceCode(final Class<?> type) {
        return type.getProtectionDomain().getCodeSource();
    }

    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "Location supplied by JVM")
    private static Path location(
            final Class<?> type, final Function<Class<?>, CodeSource> locator) {
        final CodeSource codeSource = locator.apply(type);
        if (codeSource == null) {
            throw new IllegalArgumentException("Type not loaded from a jar file: " + type);
        }

        final URL location = codeSource.getLocation();

        try {
            return Paths.get(location.toURI());
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Type not loaded from accessible jar file. location: " + location, e);
        }
    }

    private static long jarSize(final Path location) {
        try {
            return Files.size(location);
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Failed to read file size. location: " + location, e);
        }
    }

    /**
     * Extract the version number from the path within the Gradle cache.
     *
     * <p>Dependencies are in the Gradle cache. This stores jars in a directory structure that
     * contains the version number. For example {@code
     * .gradle/caches/modules-2/files-2.1/com.damnhandy/handy-uri-templates/
     * 2.1.8/170102d8e1d6fcc5e8f9bef45de923285dd3a80f/handy-uri-templates-2.1.8.jar} Where the
     * version is {@code 2.1.8}.
     *
     * @param type a type loaded from the jar file.
     * @return the version of the jar file.
     */
    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "Path provided by JVM")
    private static String jarVersion(final Class<?> type, final Path location) {
        final String textPath = location.toString();
        final int idx = textPath.indexOf(GRADLE_CACHE_PATH);
        if (idx < 0) {
            throw new IllegalArgumentException(
                    "Jar not loaded from the Gradle cache. jar: " + location);
        }
        final Path startPath = Paths.get(textPath.substring(0, idx + GRADLE_CACHE_PATH.length()));
        final Path relative = startPath.relativize(location);

        return Optional.of(relative)
                .map(Path::getParent)
                .map(Path::getParent)
                .map(Path::getFileName)
                .map(Path::toString)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Could not decode version from path. jar: " + location));
    }

    private static String minJavaVersion(final Class<?> type, final Path location) {
        try (JarFile jarFile = new JarFile(location.toFile())) {
            final Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();

                if (entry.getName().endsWith(".class")) {
                    try (InputStream classInputStream = jarFile.getInputStream(entry)) {
                        return minJavaVersion(classInputStream);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to extract the min Java version the jar supports. Path: " + location,
                    e);
        }

        return "Unknown";
    }

    private static String minJavaVersion(final InputStream classFile) throws IOException {
        final ClassReader classReader = new ClassReader(classFile);
        final VersionClassVisitor versionClassVisitor = new VersionClassVisitor();
        classReader.accept(versionClassVisitor, 0);

        final int majorVersion = versionClassVisitor.majorVersion;
        if (majorVersion < 0) {
            throw new IllegalArgumentException("Failed to determine Java version");
        }

        final String javaVersion =
                majorVersion < 49 ? "1." + (majorVersion - 44) : String.valueOf(majorVersion - 44);

        return "Java " + javaVersion;
    }

    private static class VersionClassVisitor extends org.objectweb.asm.ClassVisitor {

        private int majorVersion = -1;

        VersionClassVisitor() {
            super(org.objectweb.asm.Opcodes.ASM9);
        }

        @Override
        public void visit(
                final int version,
                final int access,
                final String name,
                final String signature,
                final String superName,
                final String[] interfaces) {
            if (version > majorVersion) {
                majorVersion = version;
            }
        }
    }
}
