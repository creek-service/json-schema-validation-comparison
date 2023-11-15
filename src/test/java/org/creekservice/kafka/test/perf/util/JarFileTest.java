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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.security.CodeSource;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JarFileTest {

    @Mock(strictness = LENIENT)
    private Function<Class<?>, CodeSource> sourceAccessor;

    @Mock(strictness = LENIENT)
    private CodeSource codeSource;

    private JarFile jarFile;

    @BeforeEach
    void setUp() {
        jarFile = new JarFile(sourceAccessor);

        when(sourceAccessor.apply(any())).thenReturn(codeSource);
        when(codeSource.getLocation())
                .thenReturn(Test.class.getProtectionDomain().getCodeSource().getLocation());
    }

    @Test
    void shouldPassSuppliedTypeToLocatorOnSize() {
        // When:
        jarFile.size(Test.class);

        // Then:
        verify(sourceAccessor).apply(Test.class);
    }

    @Test
    void shouldPassSuppliedTypeToLocatorOnVersion() {
        // When:
        jarFile.version(Test.class);

        // Then:
        verify(sourceAccessor).apply(Test.class);
    }

    @Test
    void shouldThrowIfSourceCodeNotAvailableOnSize() {
        // Given:
        when(sourceAccessor.apply(any())).thenReturn(null);

        // When:
        final Exception e =
                assertThrows(IllegalArgumentException.class, () -> jarFile.size(Test.class));

        // Then:
        assertThat(e.getMessage(), is("Type not loaded from a jar file: " + Test.class));
    }

    @Test
    void shouldThrowIfSourceCodeNotAvailableOnVersion() {
        // Given:
        when(sourceAccessor.apply(any())).thenReturn(null);

        // When:
        final Exception e =
                assertThrows(IllegalArgumentException.class, () -> jarFile.version(Test.class));

        // Then:
        assertThat(e.getMessage(), is("Type not loaded from a jar file: " + Test.class));
    }

    @Test
    void shouldThrowIfNotLoadedFromFile() throws Exception {
        // Given:
        when(codeSource.getLocation()).thenReturn(new URL("ftp:/localhost/something"));

        // When:
        final Exception e =
                assertThrows(IllegalArgumentException.class, () -> jarFile.size(Test.class));

        // Then:
        assertThat(
                e.getMessage(),
                is("Type not loaded from accessible jar file. location: ftp:/localhost/something"));
    }

    @Test
    void shouldReturnJarSize() {
        assertThat(JarFile.jarSizeForClass(Test.class), is(greaterThan(1000L)));
    }

    @Test
    void shouldThrowOnVersionIfNotStoredInGradleCache() throws Exception {
        // Given:
        when(codeSource.getLocation()).thenReturn(new URL("file:/localhost/something"));

        // When:
        final Exception e =
                assertThrows(IllegalArgumentException.class, () -> jarFile.version(Test.class));

        // Then:
        assertThat(
                e.getMessage(),
                is("Jar not loaded from the Gradle cache. jar: /localhost/something"));
    }

    @Test
    void shouldThrowOnVersionIfNotGradleCachePathTooShort() throws Exception {
        // Given:
        when(codeSource.getLocation()).thenReturn(new URL("file:/too/short/.gradle/caches/"));

        // When:
        final Exception e =
                assertThrows(IllegalArgumentException.class, () -> jarFile.version(Test.class));

        // Then:
        assertThat(
                e.getMessage(),
                is("Could not decode version from path. jar: /too/short/.gradle/caches"));
    }

    @Test
    void shouldThrowOnVersionIfNotGradleCachePathStillTooShort() throws Exception {
        // Given:
        when(codeSource.getLocation()).thenReturn(new URL("file:/too/short/.gradle/caches/still"));

        // When:
        final Exception e =
                assertThrows(IllegalArgumentException.class, () -> jarFile.version(Test.class));

        // Then:
        assertThat(
                e.getMessage(),
                is("Could not decode version from path. jar: /too/short/.gradle/caches/still"));
    }

    @Test
    void shouldThrowOnVersionIfNotGradleCachePathIsStillTooShort() throws Exception {
        // Given:
        when(codeSource.getLocation())
                .thenReturn(new URL("file:/too/short/.gradle/caches/still/too"));

        // When:
        final Exception e =
                assertThrows(IllegalArgumentException.class, () -> jarFile.version(Test.class));

        // Then:
        assertThat(
                e.getMessage(),
                is("Could not decode version from path. jar: /too/short/.gradle/caches/still/too"));
    }

    @Test
    void shouldNoThrowOnVersionIfGradleCachePathIsNotTooShort() throws Exception {
        // Given:
        when(codeSource.getLocation())
                .thenReturn(new URL("file:/too/short/.gradle/caches/not/too/short.jar"));

        // When:
        final String version = jarFile.version(Test.class);

        // Then:
        assertThat(version, is("not"));
    }

    @Test
    void shouldReturnJarVersion() {
        assertThat(
                JarFile.jarVersionForClass(Test.class), is(matchesPattern("\\d+\\.\\d+\\.\\d+")));
    }
}
