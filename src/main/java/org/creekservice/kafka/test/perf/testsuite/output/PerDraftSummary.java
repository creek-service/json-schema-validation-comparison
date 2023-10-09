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
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.creekservice.api.test.util.TestPaths;
import org.creekservice.kafka.test.perf.serde.SerdeImpl;
import org.creekservice.kafka.test.perf.testsuite.JsonSchemaTestSuite;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.util.Table;
import org.jetbrains.annotations.NotNull;

public final class PerDraftSummary {

    public static final Path ROOT_DIR =
            TestPaths.moduleRoot("json-schema-validation-comparison")
                    .resolve("build/json-schema-test-suite/tests");

    private final Map<Key, Table> results;

    public PerDraftSummary(final Map<SerdeImpl, JsonSchemaTestSuite.Result> results) {
        this.results =
                results.entrySet().stream()
                        .flatMap(e -> buildResults(e.getKey(), e.getValue()))
                        .collect(
                                toMap(
                                        Map.Entry::getKey,
                                        e -> e.getValue().build(),
                                        throwOnDuplicate(),
                                        TreeMap::new));
    }

    @Override
    public String toString() {
        return results.entrySet().stream()
                .map(e -> "## " + e.getKey() + lineSeparator() + e.getValue())
                .collect(Collectors.joining(lineSeparator()));
    }

    private Stream<Map.Entry<Key, Builder>> buildResults(
            final SerdeImpl impl, final JsonSchemaTestSuite.Result results) {
        final Map<Key, Builder> output = new TreeMap<>();
        results.visit(
                (spec, result) -> {
                    output.computeIfAbsent(new Key(spec, impl.name()), k -> new Builder())
                            .add(result, spec);
                });
        return output.entrySet().stream();
    }

    private static BinaryOperator<Table> throwOnDuplicate() {
        return (m1, m2) -> {
            throw new IllegalStateException("Duplicate!");
        };
    }

    @SuppressFBWarnings("EQ_COMPARETO_USE_OBJECT_EQUALS")
    private static final class Key implements Comparable<Key> {

        private static final Comparator<Key> COMPARATOR =
                Comparator.comparing(Key::spec).thenComparing(Key::impl);

        private final SchemaSpec spec;
        private final String impl;

        private Key(final SchemaSpec spec, final String impl) {
            this.spec = requireNonNull(spec, "spec");
            this.impl = requireNonNull(impl, "impl");
        }

        SchemaSpec spec() {
            return spec;
        }

        String impl() {
            return impl;
        }

        @Override
        public int compareTo(@NotNull final Key o) {
            return COMPARATOR.compare(this, o);
        }

        @Override
        public String toString() {
            return impl + ": " + spec;
        }
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
}
