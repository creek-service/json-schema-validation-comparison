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

package org.creekservice.kafka.test.perf.testsuite;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class TestSuiteFile {

    private final String description;
    private final List<TestSuite> tests;

    public TestSuiteFile(
            @JsonProperty(value = "description", required = true) final String description,
            @JsonProperty(value = "tests", required = true) final List<TestSuite> tests) {
        this.description = requireNonNull(description, "description");
        this.tests = List.copyOf(requireNonNull(tests, "tests"));
    }

    public String description() {
        return description;
    }

    public List<TestSuite> tests() {
        return tests;
    }
}
