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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.function.Function;
import org.creekservice.api.test.util.TestPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JarFileTest {

    private static final URL THIS_FILE;

    static {
        try {
            THIS_FILE =
                    TestPaths.moduleRoot("json-schema-validation-comparison")
                            .resolve(
                                    "src/test/java/org/creekservice/kafka/test/perf/util/JarFileTest.java")
                            .toUri()
                            .toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Mock(strictness = Mock.Strictness.LENIENT)
    private Function<Class<?>, CodeSource> sourceAccessor;

    @Mock private CodeSource codeSource;
    private JarFile jarFile;

    @BeforeEach
    void setUp() {
        jarFile = new JarFile(sourceAccessor);

        when(sourceAccessor.apply(any())).thenReturn(codeSource);
    }

    @Test
    void shouldPassSuppliedTypeToLocator() throws Exception {
        // Given:
        when(codeSource.getLocation()).thenReturn(THIS_FILE.toURI().toURL());

        // When:
        jarFile.size(Test.class);

        // Then:
        verify(sourceAccessor).apply(Test.class);
    }

    @Test
    void shouldThrowIfSourceCodeNotAvailable() {
        // Given:
        when(sourceAccessor.apply(any())).thenReturn(null);

        // When:
        final Exception e =
                assertThrows(IllegalArgumentException.class, () -> jarFile.size(Test.class));

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
}
