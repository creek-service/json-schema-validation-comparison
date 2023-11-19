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

package org.creekservice.kafka.test.perf.performance.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mock.Strictness.LENIENT;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import org.creekservice.kafka.test.perf.performance.util.model.PerformanceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PerformanceDataValidatorTest {

    private static final Path SOME_PATH = Path.of("some/path");
    @Mock private PerformanceJsonReader reader;

    @Mock(strictness = LENIENT)
    private PerformanceResult result1;

    private PerformanceDataValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PerformanceDataValidator(reader);

        when(reader.read(SOME_PATH)).thenReturn(new PerformanceResult[] {result1});
        when(result1.testClass()).thenReturn("JsonTestBenchmark");
    }

    @Test
    void shouldThrowIfBenchmarkMethodDoesNotMatchPattern() {
        // Given:
        when(result1.testCase()).thenReturn("invalidPattern");

        // When:
        final Exception e =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(SOME_PATH));

        // Then:
        assertThat(
                e.getMessage(),
                is(
                        "The JSON benchmark results contain a benchmark method with a name that"
                                + " does not match the expected pattern. Expected pattern:"
                                + " measure(?<draft>Draft[_0-9]+)_(?<impl>[A-Za-z0-9]+)"
                                + System.lineSeparator()
                                + "Method name: JsonTestBenchmark.invalidPattern"));
    }

    @Test
    void shouldThrowOnUnknownDraftVersion() {
        // Given:
        when(result1.testCase()).thenReturn("measureDraft_11_Snow");

        // When:
        final Exception e =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(SOME_PATH));

        // Then:
        assertThat(
                e.getMessage(),
                is(
                        "The JSON benchmark results contain a benchmark method with a name that"
                                + " does not contain a valid schema specification draft."
                                + System.lineSeparator()
                                + "Available versions: [Draft_03, Draft_04, Draft_06, Draft_07,"
                                + " Draft_2019_09, Draft_2020_12]"
                                + System.lineSeparator()
                                + "Detected version: Draft_11"
                                + System.lineSeparator()
                                + "Method name: JsonTestBenchmark.measureDraft_11_Snow"));
    }

    @Test
    void shouldThrowOnUnknownImplementation() {
        // Given:
        when(result1.testCase()).thenReturn("measureDraft_07_InvalidImpl");

        // When:
        final Exception e =
                assertThrows(IllegalArgumentException.class, () -> validator.validate(SOME_PATH));

        // Then:
        assertThat(
                e.getMessage(),
                is(
                        "The JSON benchmark results contain a benchmark method with a name that not"
                                + " end with a known implementation's short name."
                                + System.lineSeparator()
                                + "Detected short name: InvalidImpl"
                                + System.lineSeparator()
                                + "Method name: JsonTestBenchmark.measureDraft_07_InvalidImpl"));
    }

    @Test
    void shouldParseJson() {
        // Given:
        when(result1.testCase()).thenReturn("measureDraft_2020_12_Vertx");

        // When:
        validator.validate(SOME_PATH);

        // Then: did not throw.
    }
}
