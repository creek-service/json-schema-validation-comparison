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
import static org.hamcrest.Matchers.lessThan;
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

    @Test
    void shouldPassSuppliedTypeToLocator() {
        // When:
        new ImplJarFile(Test.class, sourceAccessor);

        // Then:
        verify(sourceAccessor).apply(Test.class);
    }

    @Test
    void shouldThrowIfSourceCodeNotAvailable() {
        // Given:
        when(sourceAccessor.apply(any())).thenReturn(null);

        // When:
        final Exception e =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new ImplJarFile(Test.class, sourceAccessor));

        // Then:
        assertThat(e.getMessage(), is("Type not loaded from a jar file: " + Test.class));
    }

    @Test
    void shouldThrowIfNotLoadedFromFile() throws Exception {
        // Given:
        when(codeSource.getLocation()).thenReturn(new URL("ftp:/localhost/something"));

        // When:
        final Exception e =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new ImplJarFile(Test.class, sourceAccessor));

        // Then:
        assertThat(
                e.getMessage(),
                is(
                        "Type not loaded from accessible jar file. location:"
                                + " ftp:/localhost/something"));
    }

    @Test
    void shouldReturnJarSize() {
        // Given:
        final ImplJarFile jarFile = new ImplJarFile(Test.class);

        // Then:
        assertThat(jarFile.jarSize(), is(greaterThan(1_000L)));
        assertThat(jarFile.jarSize(), is(lessThan(1_000_000L)));
    }

    @Test
    void shouldReturnJarVersion() {
        // Given:
        final ImplJarFile jarFile = new ImplJarFile(Test.class);

        // Then:
        assertThat(jarFile.jarVersion(), matchesPattern("\\d+\\.\\d+\\.\\d+"));
    }

    @Test
    void shouldReturnJarMinJavaVersion() {
        // Given:
        final ImplJarFile jarFile = new ImplJarFile(Test.class);

        // Then:
        assertThat(jarFile.minJavaVersion(), is("Java 9"));
    }
}
