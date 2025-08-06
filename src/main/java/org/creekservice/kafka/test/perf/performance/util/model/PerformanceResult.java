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

package org.creekservice.kafka.test.perf.performance.util.model;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public final class PerformanceResult {

    private static final String PERFORMANCE_PACKAGE =
            "org.creekservice.kafka.test.perf.performance.";

    private final String testClass;
    private final String testCase;

    private final String mode;
    private final Metric metric;

    @JsonCreator
    public PerformanceResult(
            @JsonProperty(value = "benchmark", required = true) final String benchmark,
            @JsonProperty(value = "mode", required = true) final String mode,
            @JsonProperty(value = "primaryMetric", required = true) final Metric primaryMetric) {
        this.testClass = extractTestClass(requireNonNull(benchmark, "benchmark"));
        this.testCase = extractTestCase(benchmark);
        this.mode = requireNonNull(mode, "mode");
        this.metric = requireNonNull(primaryMetric, "primaryMetric");
    }

    public String testClass() {
        return testClass;
    }

    public String testCase() {
        return testCase;
    }

    public String mode() {
        return mode;
    }

    public Metric metric() {
        return metric;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PerformanceResult that = (PerformanceResult) o;
        return Objects.equals(testClass, that.testClass)
                && Objects.equals(testCase, that.testCase)
                && Objects.equals(mode, that.mode)
                && Objects.equals(metric, that.metric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testClass, testCase, mode, metric);
    }

    private static String extractTestClass(final String benchmark) {
        final String test = benchmark.substring(PERFORMANCE_PACKAGE.length());
        return test.substring(0, test.indexOf("."));
    }

    private static String extractTestCase(final String benchmark) {
        return benchmark.substring(benchmark.lastIndexOf(".") + 1);
    }
}
