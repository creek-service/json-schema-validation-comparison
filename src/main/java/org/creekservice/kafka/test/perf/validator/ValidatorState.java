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

package org.creekservice.kafka.test.perf.validator;

import org.creekservice.api.test.util.TestPaths;
import org.creekservice.kafka.test.perf.serde.SerdeImpl;
import org.creekservice.kafka.test.perf.testsuite.JsonSchemaTestSuite;
import org.creekservice.kafka.test.perf.testsuite.JsonSchemaTestSuite.Result;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.testsuite.TestCase;
import org.creekservice.kafka.test.perf.testsuite.TestSuiteLoader;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
abstract class ValidatorState {

    public static final JsonSchemaTestSuite TEST_SUITE =
            new TestSuiteLoader(p -> true)
                    .load(
                            TestPaths.moduleRoot("json-schema-validation-comparison")
                                    .resolve("build/json-schema-test-suite"));

    private final JsonSchemaTestSuite.Runner runner;

    ValidatorState(final SerdeImpl serde) {
        runner = TEST_SUITE.prepare(serde.validator(), new PreTestPredicate());
    }

    public Result validate(final SchemaSpec spec) {
        return runner.run(spec::equals);
    }

    private static class PreTestPredicate implements JsonSchemaTestSuite.TestPredicate {
        @Override
        public boolean test(final TestCase testCase) {
            // Only test valid cases during performance testing,
            // As the cost of error handling varies massively between impls.
            // There is a strong correlation between that cost and the richness of error messages.
            // Impls should not be penalised for rich error handling!
            // Production use cases should almost never see validation errors, so its cost isn't
            // that relevant.
            return testCase.valid();
        }
    }
}
