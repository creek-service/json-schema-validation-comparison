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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImplJarFileTest {

    @Mock(strictness = LENIENT)
    private Function<Class<?>, CodeSource> sourceAccessor;

    @Mock(strictness = LENIENT)
    private CodeSource codeSource;

    @BeforeEach
    void setUp() {
        when(sourceAccessor.apply(any())).thenReturn(codeSource);
        when(codeSource.getLocation())
                .thenReturn(Test.class.getProtectionDomain().getCodeSource().getLocation());
    }

    @Nested
    class JarBaseTaskTest {

        private ExampleTask task;

        @BeforeEach
        void setUp() {
            task = new ExampleTask(sourceAccessor);
        }

        @Test
        void shouldPassSuppliedTypeToLocator() {
            // When:
            task.getPath(Test.class);

            // Then:
            verify(sourceAccessor).apply(Test.class);
        }

        @Test
        void shouldThrowIfSourceCodeNotAvailable() {
            // Given:
            when(sourceAccessor.apply(any())).thenReturn(null);

            // When:
            final Exception e =
                    assertThrows(IllegalArgumentException.class, () -> task.getPath(Test.class));

            // Then:
            assertThat(e.getMessage(), is("Type not loaded from a jar file: " + Test.class));
        }

        @Test
        void shouldThrowIfNotLoadedFromFile() throws Exception {
            // Given:
            when(codeSource.getLocation()).thenReturn(new URL("ftp:/localhost/something"));

            // When:
            final Exception e =
                    assertThrows(IllegalArgumentException.class, () -> task.getPath(Test.class));

            // Then:
            assertThat(
                    e.getMessage(),
                    is(
                            "Type not loaded from accessible jar file. location:"
                                    + " ftp:/localhost/something"));
        }

        @Test
        void shouldGetPathOfJar() {
            // When:
            final Path path = task.getPath(Test.class);

            // Then:
            assertThat(path.toString(), containsString(".gradle" + File.separator + "caches"));
        }

        private final class ExampleTask extends ImplJarFile.JarTaskBase {

            ExampleTask(final Function<Class<?>, CodeSource> locator) {
                super(locator);
            }

            Path getPath(final Class<Test> testClass) {
                return pathToJar(testClass);
            }
        }
    }

    @Nested
    class JarSizeTest {
        @Test
        void shouldReturnJarSize() {
            assertThat(ImplJarFile.jarSizeForClass(Test.class), is(greaterThan(1000L)));
        }
    }

    @Nested
    class JarVersionTest {
        @Test
        void shouldReturnJarVersion() {
            assertThat(
                    ImplJarFile.jarVersionForClass(Test.class),
                    is(matchesPattern("\\d+\\.\\d+\\.\\d+")));
        }
    }

    @Nested
    class JarMinJavaVersionTest {
        @Test
        void shouldReturnJarMinJavaVersion() {
            assertThat(ImplJarFile.jarMinJavaVersion(Test.class), is("Java 9"));
        }
    }
}
