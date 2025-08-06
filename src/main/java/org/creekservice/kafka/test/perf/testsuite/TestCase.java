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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class TestCase {

    private final String description;
    private final String data;
    private final boolean valid;
    private final String comment;
    private final Path suiteFilePath;

    public TestCase(
            @JsonProperty(value = "description", required = true) final String description,
            @JsonProperty(value = "data", required = true) final JsonNode data,
            @JsonProperty(value = "valid", required = true) final boolean valid,
            @JsonProperty(value = "comment") final Optional<String> comment,
            @JacksonInject("suiteFilePath") final Path suiteFilePath) {
        try {
            this.description = requireNonNull(description, "description");
            this.data = TestSuiteMapper.MAPPER.writeValueAsString(requireNonNull(data, "data"));
            this.valid = valid;
            this.comment = requireNonNull(comment, "comment").orElse("");
            this.suiteFilePath = requireNonNull(suiteFilePath, "suiteFilePath");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String description() {
        return description;
    }

    public String getData() {
        return data;
    }

    public boolean valid() {
        return valid;
    }
}
