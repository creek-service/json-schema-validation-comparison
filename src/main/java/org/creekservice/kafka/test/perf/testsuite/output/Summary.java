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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.creekservice.kafka.test.perf.serde.SerdeImpl;
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

    public Summary(final Map<SerdeImpl, JsonSchemaTestSuite.Result> results) {
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

    private static Table createTable(final Map<SerdeImpl, JsonSchemaTestSuite.Result> results) {
        final Map<String, Map<SchemaSpec, Counts>> counts =
                results.entrySet().stream()
                        .collect(toMap(e -> e.getKey().name(), e -> resultCounts(e.getValue())));

        final List<SchemaSpec> specs =
                Arrays.stream(SchemaSpec.values())
                        .filter(
                                spec ->
                                        counts.values().stream()
                                                .map(map -> map.get(spec))
                                                .anyMatch(c -> c.totalTotal() > 0))
                        .collect(toList());

        final List<String> headers = specs.stream().map(SchemaSpec::name).collect(toList());

        headers.add(0, COL_IMPL);
        headers.add(1, COL_OVERALL);

        final Table table = new Table(headers);

        counts.forEach((impl, cs) -> populateRow(table.addRow(), impl, cs, specs));

        return table;
    }

    private static Map<SchemaSpec, Counts> resultCounts(final JsonSchemaTestSuite.Result result) {
        final Map<SchemaSpec, Counts> counts = new HashMap<>();
        Arrays.stream(SchemaSpec.values()).forEach(s -> counts.put(s, new Counts()));

        result.visit((spec, r) -> counts.get(spec).add(r));

        return counts;
    }

    private static void populateRow(
            final Table.Row row,
            final String impl,
            final Map<SchemaSpec, Counts> specCounts,
            final List<SchemaSpec> specs) {
        row.put(COL_IMPL, impl);

        specs.forEach(spec -> row.put(spec.name(), formatCell(specCounts.get(spec))));

        final Counts overall = specCounts.values().stream().reduce(new Counts(), Counts::combine);
        row.put(COL_OVERALL, formatCell(overall));
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
                + counts.score();
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

        String score() {
            final double reqPct = reqTotal == 0 ? 0 : ((double) reqPassed / reqTotal);
            final double optPct = optTotal == 0 ? 0 : ((double) optPassed / optTotal);
            final double score =
                    100 * ((reqPct * REQUIRED_WEIGHT) + optPct) / (REQUIRED_WEIGHT + 1);
            final NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMinimumFractionDigits(1);
            nf.setMaximumFractionDigits(1);
            return nf.format(score);
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
