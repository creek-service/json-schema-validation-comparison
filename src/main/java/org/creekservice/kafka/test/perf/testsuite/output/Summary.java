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
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.creekservice.kafka.test.perf.implementations.Implementation;
import org.creekservice.kafka.test.perf.testsuite.JsonSchemaTestSuite;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.util.Table;

public final class Summary {

    private static final String COL_IMPL = "Implementations";
    private static final String COL_OVERALL = "Overall";

    private final Table table;
    private final Duration duration;

    public Summary(final Map<Implementation, JsonSchemaTestSuite.Result> results) {
        this.duration =
                results.values().stream()
                        .map(JsonSchemaTestSuite.Result::duration)
                        .reduce(Duration.ZERO, Duration::plus);
        this.table = createTable(results);
    }

    public String toMarkdown() {
        return table.toMarkdown()
                + lineSeparator()
                + lineSeparator()
                + String.format(
                        "Time: %d.%03ds", +duration.toSecondsPart(), duration.toMillisPart());
    }

    public String toJson() {
        return table.toJson();
    }

    private static Table createTable(
            final Map<Implementation, JsonSchemaTestSuite.Result> results) {
        final Map<String, Map<String, Counts>> counts = buildCounts(results);

        final List<String> specColumns = buildSpecHeaders(counts);

        final List<String> headers = new ArrayList<>(specColumns);
        headers.add(0, COL_IMPL);

        return buildTable(counts, specColumns, headers);
    }

    private static Map<String, Map<String, Counts>> buildCounts(
            final Map<Implementation, JsonSchemaTestSuite.Result> results) {

        final Map<String, Map<String, Counts>> counts =
                results.entrySet().stream()
                        .collect(
                                toMap(
                                        e -> e.getKey().metadata().shortName(),
                                        e -> resultCounts(e.getValue())));

        counts.values()
                .forEach(
                        specCounts ->
                                specCounts.put(
                                        COL_OVERALL,
                                        specCounts.values().stream()
                                                .reduce(new Counts(), Counts::combine)));
        return counts;
    }

    private static Map<String, Counts> resultCounts(final JsonSchemaTestSuite.Result result) {
        final Map<String, Counts> counts = new HashMap<>();
        Arrays.stream(SchemaSpec.values()).forEach(s -> counts.put(s.name(), new Counts()));
        result.visit((spec, r) -> counts.get(spec.name()).add(r));
        return counts;
    }

    private static List<String> buildSpecHeaders(final Map<String, Map<String, Counts>> counts) {
        final List<String> specColumnHeaders =
                Arrays.stream(SchemaSpec.values())
                        .map(SchemaSpec::name)
                        .filter(
                                name ->
                                        counts.values().stream()
                                                .map(map -> map.get(name))
                                                .anyMatch(c -> c.totalTotal() > 0))
                        .collect(toList());

        specColumnHeaders.add(0, COL_OVERALL);
        return List.copyOf(specColumnHeaders);
    }

    private static Table buildTable(
            final Map<String, Map<String, Counts>> counts,
            final List<String> specColumns,
            final List<String> headers) {
        final Table table = new Table(headers);
        counts.forEach(
                (impl, specCounts) -> populateRow(table.addRow(), impl, specCounts, specColumns));
        return table;
    }

    private static void populateRow(
            final Table.Row row,
            final String impl,
            final Map<String, Counts> specCounts,
            final List<String> specColumns) {
        row.put(COL_IMPL, impl);
        specColumns.forEach(col -> row.put(col, specCounts.get(col)));
    }

    private static final class Counts {

        @JsonProperty("requiredPass")
        private int reqPassed;

        private int reqTotal;

        @JsonProperty("optionalPass")
        private int optPassed;

        private int optTotal;

        void add(final JsonSchemaTestSuite.TestResult result) {
            final boolean passed = !(result.error() || result.failed());
            if (result.optional()) {
                optTotal++;
                if (passed) {
                    optPassed++;
                }
            } else {
                reqTotal++;
                if (passed) {
                    reqPassed++;
                }
            }
        }

        int totalTotal() {
            return reqTotal + optTotal;
        }

        @JsonProperty("requiredFail")
        int reqFail() {
            return reqTotal - reqPassed;
        }

        @JsonProperty("optionalFail")
        int optFail() {
            return optTotal - optPassed;
        }

        @JsonProperty("requiredPassPct")
        BigDecimal reqPassPct() {
            return percentage(reqPassed, reqTotal);
        }

        @JsonProperty("optionalPassPct")
        BigDecimal optPassPct() {
            return percentage(optPassed, optTotal);
        }

        @JsonProperty("requiredFailPct")
        BigDecimal reqFailPct() {
            return percentage(reqFail(), reqTotal);
        }

        @JsonProperty("optionalFailPct")
        BigDecimal optFailPct() {
            return percentage(optFail(), optTotal);
        }

        @Override
        public String toString() {
            if (totalTotal() == 0) {
                return "";
            }
            return "pass: r:"
                    + reqPassed
                    + " ("
                    + reqPassPct()
                    + "%)"
                    + " o:"
                    + optPassed
                    + " ("
                    + optPassPct()
                    + "%)"
                    + "<br>fail: r:"
                    + reqFail()
                    + " ("
                    + reqFailPct()
                    + "%)"
                    + " o:"
                    + optFail()
                    + " ("
                    + optFailPct()
                    + "%)";
        }

        static Counts combine(final Counts c0, final Counts c1) {
            final Counts counts = new Counts();
            counts.reqPassed = c0.reqPassed + c1.reqPassed;
            counts.reqTotal = c0.reqTotal + c1.reqTotal;
            counts.optPassed = c0.optPassed + c1.optPassed;
            counts.optTotal = c0.optTotal + c1.optTotal;
            return counts;
        }

        private BigDecimal percentage(final int value, final int total) {
            return total == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(value)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_EVEN);
        }
    }
}
