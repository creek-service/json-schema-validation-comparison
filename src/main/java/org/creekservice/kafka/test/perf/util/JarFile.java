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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Optional;
import java.util.function.Function;

/** Utility for determining the size of a jar file a class is loaded from. */
public final class JarFile {

    private static final String GRADLE_CACHE_PATH = ".gradle" + File.separator + "caches";

    private final Function<Class<?>, CodeSource> sourceAccessor;

    /**
     * Determine the size of the jar the supplied type is loaded from.
     *
     * @param type a class loaded from the jar.
     * @return the size of the jar, in bytes.
     * @throws IllegalArgumentException if the supplied {@code type} isn't loaded from an accessible
     *     jar.
     */
    public static long jarSizeForClass(final Class<?> type) {
        return new JarFile(JarFile::sourceCode).size(type);
    }

    /**
     * Determine the version of the jar the supplied type is loaded from.
     *
     * @param type a class loaded from the jar.
     * @return the version of the jar.
     * @throws IllegalArgumentException if the supplied {@code type} isn't loaded from an accessible
     *     jar.
     */
    public static String jarVersionForClass(final Class<?> type) {
        return new JarFile(JarFile::sourceCode).version(type);
    }

    @VisibleForTesting
    JarFile(final Function<Class<?>, CodeSource> locator) {
        this.sourceAccessor = requireNonNull(locator, "locator");
    }

    long size(final Class<?> type) {
        final Path path = pathToJar(type);
        return size(path);
    }

    String version(final Class<?> type) {
        final Path path = pathToJar(type);
        return version(path);
    }

    private Path pathToJar(final Class<?> type) {
        return asPath(location(type));
    }

    private URL location(final Class<?> type) {
        final CodeSource codeSource = sourceAccessor.apply(type);
        if (codeSource == null) {
            throw new IllegalArgumentException("Type not loaded from a jar file: " + type);
        }

        return codeSource.getLocation();
    }

    private static CodeSource sourceCode(final Class<?> type) {
        return type.getProtectionDomain().getCodeSource();
    }

    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "Location supplied by JVM")
    private static Path asPath(final URL location) {
        try {
            return Paths.get(location.toURI());
        } catch (final Exception e) {
            throw new IllegalArgumentException(
                    "Type not loaded from accessible jar file. location: " + location, e);
        }
    }

    private static long size(final Path path) {
        try {
            return Files.size(path);
        } catch (final Exception e) {
            throw new IllegalArgumentException("Failed to read file size. location: " + path, e);
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
     * @param path the path to the jar within the gradle cache.
     * @return the version.
     */
    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "Path provided by JVM")
    private static String version(final Path path) {
        final String textPath = path.toString();
        final int idx = textPath.indexOf(GRADLE_CACHE_PATH);
        if (idx < 0) {
            throw new IllegalArgumentException(
                    "Jar not loaded from the Gradle cache. jar: " + path);
        }
        final Path startPath = Paths.get(textPath.substring(0, idx + GRADLE_CACHE_PATH.length()));
        final Path relative = startPath.relativize(path);

        return Optional.of(relative)
                .map(Path::getParent)
                .map(Path::getParent)
                .map(Path::getFileName)
                .map(Path::toString)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Could not decode version from path. jar: " + path));
    }
}
