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

package org.creekservice.kafka.test.perf.testsuite;

import static java.util.stream.Collectors.toMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.creekservice.kafka.test.perf.implementations.Implementation;
import org.creekservice.kafka.test.perf.implementations.Implementations;
import org.creekservice.kafka.test.perf.testsuite.JsonSchemaTestSuite.Result;
import org.creekservice.kafka.test.perf.testsuite.JsonSchemaTestSuite.TestPredicate;
import org.creekservice.kafka.test.perf.testsuite.output.PerDraftSummary;
import org.creekservice.kafka.test.perf.testsuite.output.Summary;
import org.creekservice.kafka.test.perf.util.Logging;

/** Entry point for the functional tests. */
public final class JsonTestSuiteMain {

    static {
        Logging.disable();
    }

    // Increase locally to allow for meaningful profiling:
    private static final int ITERATIONS = 1;

    private JsonTestSuiteMain() {}

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public static void main(final String... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException(
                    "Invoke with exactly one argument: the path to the root directory containing"
                            + " the JSON test suite from"
                            + " https://github.com/json-schema-org/JSON-Schema-Test-Suite.");
        }

        final JsonSchemaTestSuite testSuite =
                new TestSuiteLoader(path -> true).load(Paths.get(args[0]));

        final Map<Implementation, JsonSchemaTestSuite.Runner> prepared =
                Implementations.all().stream()
                        .collect(
                                toMap(
                                        Function.identity(),
                                        impl -> testSuite.prepare(impl, TestPredicate.ALL)));

        final Map<Implementation, Result> results = new HashMap<>();
        for (int i = 0; i < ITERATIONS; i++) {
            for (final Map.Entry<Implementation, JsonSchemaTestSuite.Runner> e :
                    prepared.entrySet()) {
                results.put(e.getKey(), e.getValue().run(spec -> true));
            }
        }

        outputResults(results);
    }

    private static void outputResults(final Map<Implementation, Result> results) {
        System.out.println("# Overall comparison");
        System.out.println(new Summary(results));
        System.out.println();
        System.out.println("# Specific Draft & Implementation results");
        System.out.println(new PerDraftSummary(results));
    }
}
