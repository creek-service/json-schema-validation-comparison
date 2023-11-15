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

package org.creekservice.kafka.test.perf.testsuite.output;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toMap;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import org.creekservice.api.test.util.TestPaths;
import org.creekservice.kafka.test.perf.implementations.Implementation;
import org.creekservice.kafka.test.perf.testsuite.JsonSchemaTestSuite;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.util.Table;

public final class PerDraftSummary {

    public static final Path ROOT_DIR =
            TestPaths.moduleRoot("json-schema-validation-comparison")
                    .resolve("build/json-schema-test-suite/tests");

    private final Map<String, ImplTables> results;

    public PerDraftSummary(final Map<Implementation, JsonSchemaTestSuite.Result> results) {
        this.results =
                results.entrySet().stream()
                        .collect(
                                toMap(
                                        e -> e.getKey().metadata().shortName(),
                                        e -> new ImplTables(e.getValue()),
                                        throwOnDuplicate(),
                                        TreeMap::new));
    }

    public String toMarkdown() {
        return results.entrySet().stream()
                .map(e -> "#### " + e.getKey() + lineSeparator() + e.getValue().toMarkdown())
                .collect(Collectors.joining(lineSeparator()));
    }

    private static <T> BinaryOperator<T> throwOnDuplicate() {
        return (m1, m2) -> {
            throw new IllegalStateException("Duplicate!");
        };
    }

    private static class Counts {
        private int pass;
        private int fail;

        void add(final JsonSchemaTestSuite.TestResult result) {
            final boolean passed = !(result.error() || result.failed());
            if (passed) {
                pass++;
            } else {
                fail++;
            }
        }
    }

    private static class Builder {

        private final Map<Path, Counts> bySuite = new TreeMap<>();

        void add(final JsonSchemaTestSuite.TestResult result, final SchemaSpec spec) {
            final Path suitePath =
                    ROOT_DIR.resolve(spec.dirName()).relativize(result.suite().filePath());
            bySuite.computeIfAbsent(suitePath, k -> new Counts()).add(result);
        }

        public Table build() {
            final Table table = new Table(List.of("suite", "pass", "fail", "total"));
            bySuite.forEach(
                    (suite, counts) -> {
                        final Table.Row row = table.addRow();
                        row.put("suite", suite);
                        row.put("pass", counts.pass);
                        row.put("fail", counts.fail);
                        row.put("total", counts.pass + counts.fail);
                    });
            return table;
        }
    }

    private static class ImplTables {

        private final Map<SchemaSpec, Table> tables;

        ImplTables(final JsonSchemaTestSuite.Result results) {
            final Map<SchemaSpec, Builder> output = new TreeMap<>();
            results.visit(
                    (spec, result) ->
                            output.computeIfAbsent(spec, k -> new Builder()).add(result, spec));

            this.tables =
                    output.entrySet().stream()
                            .collect(
                                    Collectors.toMap(
                                            Map.Entry::getKey,
                                            e -> e.getValue().build(),
                                            throwOnDuplicate(),
                                            TreeMap::new));
        }

        public String toMarkdown() {
            return tables.entrySet().stream()
                    .map(
                            e ->
                                    "##### "
                                            + e.getKey().capitalisedName()
                                            + lineSeparator()
                                            + lineSeparator()
                                            + e.getValue().toMarkdown())
                    .collect(Collectors.joining(lineSeparator()));
        }
    }
}
