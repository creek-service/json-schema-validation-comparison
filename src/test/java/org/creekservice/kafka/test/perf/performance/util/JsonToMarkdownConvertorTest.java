/*
 * Copyright 2023-2025 Creek Contributors (https://github.com/creek-service)
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
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.creekservice.kafka.test.perf.performance.util.model.Metric;
import org.creekservice.kafka.test.perf.performance.util.model.PerformanceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonToMarkdownConvertorTest {

    private static final String EXPECTED_HEADINGS =
            "| Benchmark | Mode | Score | Score Error (99.9%) | Unit |"
                    + System.lineSeparator()
                    + "|-----------|------|-------|---------------------|------|"
                    + System.lineSeparator();

    private static final Path SOME_PATH = Paths.get("some/path");

    @Mock private PerformanceJsonReader reader;
    private JsonToMarkdownConvertor convertor;

    @BeforeEach
    void setUp() {
        convertor = new JsonToMarkdownConvertor(reader);
    }

    @Test
    void shouldConvertJsonToMarkdown() {
        // Given:
        when(reader.read(SOME_PATH))
                .thenReturn(
                        new PerformanceResult[] {
                            new PerformanceResult(
                                    "org.creekservice.kafka.test.perf.performance.JsonValidateBenchmark.measureDraft_4_Medeia",
                                    "avgt",
                                    new Metric(
                                            new BigDecimal("0.34276444437738995"),
                                            new BigDecimal("0.0038394222791281593"),
                                            "ms/op")),
                            new PerformanceResult(
                                    "org.creekservice.kafka.test.perf.performance.JsonValidateBenchmark.measureDraft_7_Medeia",
                                    "avgt",
                                    new Metric(
                                            new BigDecimal("0.893598359837538"),
                                            new BigDecimal("0.0035983789573"),
                                            "ms/op")),
                            new PerformanceResult(
                                    "org.creekservice.kafka.test.perf.performance.JsonSerdeBenchmark.measureEveritRoundTrip",
                                    "diff",
                                    new Metric(
                                            new BigDecimal("2135454.1245"),
                                            new BigDecimal("0.003536745566"),
                                            "us/op"))
                        });

        // When:
        final Map<String, String> results = convertor.convert(SOME_PATH);

        // Then:
        assertThat(
                results,
                is(
                        Map.of(
                                "JsonValidateBenchmark",
                                EXPECTED_HEADINGS
                                        + "| measureDraft_4_Medeia | avgt | 0.34276 | 0.0038394 |"
                                        + " ms/op |"
                                        + System.lineSeparator()
                                        + "| measureDraft_7_Medeia | avgt | 0.89360 | 0.0035984 |"
                                        + " ms/op |"
                                        + System.lineSeparator(),
                                "JsonSerdeBenchmark",
                                EXPECTED_HEADINGS
                                        + "| measureEveritRoundTrip | diff | 2135454 | 0.0035367 |"
                                        + " us/op |"
                                        + System.lineSeparator())));
    }

    @Test
    void shouldHandleNaN() {
        // Given:
        when(reader.read(SOME_PATH))
                .thenReturn(
                        new PerformanceResult[] {
                            new PerformanceResult(
                                    "org.creekservice.kafka.test.perf.performance.JsonValidateBenchmark.measureDraft_7_Medeia",
                                    "avgt",
                                    new Metric(new BigDecimal("0.893602424"), "NaN", "ms/op"))
                        });

        // When:
        final Map<String, String> results = convertor.convert(SOME_PATH);

        // Then:
        assertThat(
                results,
                is(
                        Map.of(
                                "JsonValidateBenchmark",
                                EXPECTED_HEADINGS
                                        + "| measureDraft_7_Medeia | avgt | 0.89360 |  | ms/op"
                                        + " |"
                                        + System.lineSeparator())));
    }
}
