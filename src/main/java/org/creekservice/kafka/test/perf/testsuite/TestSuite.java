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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class TestSuite {

    private static final String FORMAT_ASSERTION_TAG = "format-assertion";
    private static final String FORMAT_ANNOTATION_TAG = "format-annotation";

    private final String description;
    private final String schema;
    private final List<TestCase> tests;
    private final String comment;
    private final Path suiteFilePath;
    private final Compatibility compatibility;
    private final List<String> tags;
    private final Map<URI, String> externalSchemas;
    private final boolean inOptionalDir;

    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "False +")
    @SuppressWarnings("checkstyle:ParameterNumber")
    public TestSuite(
            @JsonProperty(value = "description", required = true) final String description,
            @JsonProperty(value = "schema", required = true) final JsonNode schema,
            @JsonProperty(value = "tests", required = true) final List<TestCase> tests,
            @JsonProperty(value = "comment") final Optional<String> comment,
            @JsonProperty(value = "compatibility") final Optional<String> compatibilityStr,
            @JsonProperty(value = "tags") final Optional<List<String>> tags,
            @JsonProperty(value = "externalSchemas")
                    final Optional<Map<String, JsonNode>> externalSchemas,
            @JacksonInject("suiteFilePath") final Path suiteFilePath) {
        try {
            this.description = requireNonNull(description, "description");
            this.schema =
                    TestSuiteMapper.MAPPER.writeValueAsString(requireNonNull(schema, "schema"));
            this.tests = List.copyOf(requireNonNull(tests, "tests"));
            this.comment = requireNonNull(comment, "comment").orElse("");
            this.suiteFilePath = requireNonNull(suiteFilePath, "suiteFilePath");
            this.compatibility =
                    Compatibility.parse(
                            requireNonNull(compatibilityStr, "compatibility").orElse(""));
            this.tags = List.copyOf(requireNonNull(tags, "tags").orElse(List.of()));
            this.externalSchemas =
                    parseExternalSchemas(
                            requireNonNull(externalSchemas, "externalSchemas").orElse(Map.of()));
            this.inOptionalDir =
                    suiteFilePath.getParent() != null
                            && suiteFilePath
                                    .getParent()
                                    .toString()
                                    .contains(File.separator + "optional");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String description() {
        return description;
    }

    public String comment() {
        return comment;
    }

    public String schema() {
        return schema;
    }

    public List<TestCase> tests() {
        return List.copyOf(tests);
    }

    public Path filePath() {
        return suiteFilePath;
    }

    public Compatibility compatibility() {
        return compatibility;
    }

    public Map<URI, String> externalSchemas() {
        return Map.copyOf(externalSchemas);
    }

    public boolean formatAssertion() {
        return tags.contains(FORMAT_ASSERTION_TAG);
    }

    public boolean optional(final SchemaSpec spec) {
        if (inOptionalDir) {
            return true;
        }
        final int token = spec.compatibilityToken();
        if (tags.contains(FORMAT_ASSERTION_TAG) && token <= 7) {
            return true;
        }
        if (tags.contains(FORMAT_ANNOTATION_TAG) && token <= 7) {
            return true;
        }
        return false;
    }

    private static Map<URI, String> parseExternalSchemas(final Map<String, JsonNode> raw) {
        return raw.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                e -> URI.create(e.getKey()), e -> e.getValue().toString()));
    }
}
