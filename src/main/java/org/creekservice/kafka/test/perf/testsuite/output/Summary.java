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

import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.creekservice.kafka.test.perf.implementations.Implementation;
import org.creekservice.kafka.test.perf.testsuite.JsonSchemaTestSuite;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.util.Table;

public final class Summary {

    /** How much weight to put in required features vs optional. */
    private static final int REQUIRED_WEIGHT = 3;

    private static final String COL_IMPL = "Impl";
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

    @Override
    public String toString() {
        return table.toString()
                + lineSeparator()
                + lineSeparator()
                + String.format(
                        "Time: %d.%03ds", +duration.toSecondsPart(), duration.toMillisPart());
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

        counts.entrySet().stream()
                .sorted(
                        Comparator.<Map.Entry<String, Map<String, Counts>>>comparingDouble(
                                        e1 -> e1.getValue().get(COL_OVERALL).score())
                                .reversed())
                .forEach(e -> populateRow(table.addRow(), e.getKey(), e.getValue(), specColumns));

        return table;
    }

    private static void populateRow(
            final Table.Row row,
            final String impl,
            final Map<String, Counts> specCounts,
            final List<String> specColumns) {
        row.put(COL_IMPL, impl);
        specColumns.forEach(col -> row.put(col, formatCell(specCounts.get(col))));
    }

    private static String formatCell(final Counts counts) {
        if (counts.totalTotal() == 0) {
            return "";
        }
        return "pass: r:"
                + counts.reqPassed
                + " o:"
                + counts.optPassed
                + " / fail: r:"
                + counts.reqFail()
                + " o:"
                + counts.optFail()
                + lineSeparator()
                + "r:"
                + counts.reqPassPct()
                + " o:"
                + counts.optPassPct()
                + " / r:"
                + counts.reqFailPct()
                + " f:"
                + counts.optFailPct()
                + lineSeparator()
                + "score: "
                + counts.formattedScore();
    }

    private static class Counts {

        private int reqPassed;
        private int reqTotal;
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

        int reqFail() {
            return reqTotal - reqPassed;
        }

        int optFail() {
            return optTotal - optPassed;
        }

        String reqPassPct() {
            return percentage(reqPassed, reqTotal);
        }

        String optPassPct() {
            return percentage(optPassed, optTotal);
        }

        String reqFailPct() {
            return percentage(reqFail(), reqTotal);
        }

        String optFailPct() {
            return percentage(optFail(), optTotal);
        }

        String formattedScore() {
            final NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMinimumFractionDigits(1);
            nf.setMaximumFractionDigits(1);
            return nf.format(score());
        }

        double score() {
            final double reqPct = reqTotal == 0 ? 0 : ((double) reqPassed / reqTotal);
            final double optPct = optTotal == 0 ? 0 : ((double) optPassed / optTotal);
            return 100 * ((reqPct * REQUIRED_WEIGHT) + optPct) / (REQUIRED_WEIGHT + 1);
        }

        static Counts combine(final Counts c0, final Counts c1) {
            final Counts counts = new Counts();
            counts.reqPassed = c0.reqPassed + c1.reqPassed;
            counts.reqTotal = c0.reqTotal + c1.reqTotal;
            counts.optPassed = c0.optPassed + c1.optPassed;
            counts.optTotal = c0.optTotal + c1.optTotal;
            return counts;
        }

        private String percentage(final int value, final int total) {
            final NumberFormat nf = NumberFormat.getPercentInstance();
            nf.setMinimumFractionDigits(1);
            nf.setMaximumFractionDigits(1);
            return total == 0 ? nf.format(0) : nf.format(((double) value / total));
        }
    }
}
