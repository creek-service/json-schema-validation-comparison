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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.function.Function;

/** Utility for determining the size of a jar file a class is loaded from. */
public final class JarFile {

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

    @VisibleForTesting
    JarFile(final Function<Class<?>, CodeSource> locator) {
        this.sourceAccessor = requireNonNull(locator, "locator");
    }

    long size(final Class<?> type) {
        final Path path = pathToJar(type);
        return size(path);
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
}
