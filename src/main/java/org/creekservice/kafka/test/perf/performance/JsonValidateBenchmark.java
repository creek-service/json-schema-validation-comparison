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

package org.creekservice.kafka.test.perf.performance;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.creekservice.api.test.util.TestPaths;
import org.creekservice.kafka.test.perf.implementations.DevHarrelImplementation;
import org.creekservice.kafka.test.perf.implementations.EveritImplementation;
import org.creekservice.kafka.test.perf.implementations.Implementation;
import org.creekservice.kafka.test.perf.implementations.JustifyImplementation;
import org.creekservice.kafka.test.perf.implementations.MedeiaImplementation;
import org.creekservice.kafka.test.perf.implementations.NetworkNtImplementation;
import org.creekservice.kafka.test.perf.implementations.SchemaFriendImplementation;
import org.creekservice.kafka.test.perf.implementations.SkemaImplementation;
import org.creekservice.kafka.test.perf.implementations.SnowImplementation;
import org.creekservice.kafka.test.perf.implementations.VertxImplementation;
import org.creekservice.kafka.test.perf.testsuite.JsonSchemaTestSuite;
import org.creekservice.kafka.test.perf.testsuite.JsonSchemaTestSuite.Result;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.testsuite.TestCase;
import org.creekservice.kafka.test.perf.testsuite.TestSuiteLoader;
import org.creekservice.kafka.test.perf.util.Logging;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

/**
 * Benchmark results for JSON Schema validation.
 *
 * <p>The benchmark runs each validator through the <a
 * href="https://github.com/json-schema-org/JSON-Schema-Test-Suite">standard set of tests</a>
 *
 * <p>The results show the average time it takes each impl to run through the test suite, per draft.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(MILLISECONDS)
@Threads(1) // GitHub linux runners have two cores, so running more threads is pointless.
@Fork(6) // Note: to debug, set fork to 0.
// @Warmup(iterations = 0, time = 10)
// @Measurement(iterations = 1, time = 10)
@SuppressWarnings({"FieldMayBeFinal", "MethodName"}) // not final to avoid folding.
public class JsonValidateBenchmark {

    public static final JsonSchemaTestSuite TEST_SUITE =
            new TestSuiteLoader(p -> true)
                    .load(
                            TestPaths.moduleRoot("json-schema-validation-comparison")
                                    .resolve("build/json-schema-test-suite"));

    static {
        Logging.disable();
    }

    public static class MedeiaValidator extends ValidatorState {

        public MedeiaValidator() {
            super(new MedeiaImplementation());
        }
    }

    @Benchmark
    public Result measureDraft_04_Medeia(final MedeiaValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_04);
    }

    @Benchmark
    public Result measureDraft_06_Medeia(final MedeiaValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_06);
    }

    @Benchmark
    public Result measureDraft_07_Medeia(final MedeiaValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_07);
    }

    public static class EveritValidator extends ValidatorState {

        public EveritValidator() {
            super(new EveritImplementation());
        }
    }

    @Benchmark
    public Result measureDraft_04_Everit(final EveritValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_04);
    }

    @Benchmark
    public Result measureDraft_06_Everit(final EveritValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_06);
    }

    @Benchmark
    public Result measureDraft_07_Everit(final EveritValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_07);
    }

    public static class SkemaValidator extends ValidatorState {

        public SkemaValidator() {
            super(new SkemaImplementation());
        }
    }

    @Benchmark
    public Result measureDraft_2020_12_Skema(final SkemaValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_2020_12);
    }

    public static class VertxValidator extends ValidatorState {

        public VertxValidator() {
            super(new VertxImplementation());
        }
    }

    @Benchmark
    public Result measureDraft_04_Vertx(final VertxValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_04);
    }

    @Benchmark
    public Result measureDraft_07_Vertx(final VertxValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_07);
    }

    @Benchmark
    public Result measureDraft_2019_09_Vertx(final VertxValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_2019_09);
    }

    @Benchmark
    public Result measureDraft_2020_12_Vertx(final VertxValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_2020_12);
    }

    public static class SchemaFriendValidator extends ValidatorState {

        public SchemaFriendValidator() {
            super(new SchemaFriendImplementation());
        }
    }

    @Benchmark
    public Result measureDraft_03_SchemaFriend(final SchemaFriendValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_03);
    }

    @Benchmark
    public Result measureDraft_04_SchemaFriend(final SchemaFriendValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_04);
    }

    @Benchmark
    public Result measureDraft_06_SchemaFriend(final SchemaFriendValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_06);
    }

    @Benchmark
    public Result measureDraft_07_SchemaFriend(final SchemaFriendValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_07);
    }

    @Benchmark
    public Result measureDraft_2019_09_SchemaFriend(final SchemaFriendValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_2019_09);
    }

    @Benchmark
    public Result measureDraft_2020_12_SchemaFriend(final SchemaFriendValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_2020_12);
    }

    public static class NetworkNtValidator extends ValidatorState {

        public NetworkNtValidator() {
            super(new NetworkNtImplementation());
        }
    }

    @Benchmark
    public Result measureDraft_04_NetworkNt(final NetworkNtValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_04);
    }

    @Benchmark
    public Result measureDraft_06_NetworkNt(final NetworkNtValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_06);
    }

    @Benchmark
    public Result measureDraft_07_NetworkNt(final NetworkNtValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_07);
    }

    @Benchmark
    public Result measureDraft_2019_09_NetworkNt(final NetworkNtValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_2019_09);
    }

    @Benchmark
    public Result measureDraft_2020_12_NetworkNt(final NetworkNtValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_2020_12);
    }

    public static class SnowValidator extends ValidatorState {

        public SnowValidator() {
            super(new SnowImplementation());
        }
    }

    @Benchmark
    public Result measureDraft_06_Snow(final SnowValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_06);
    }

    @Benchmark
    public Result measureDraft_07_Snow(final SnowValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_07);
    }

    @Benchmark
    public Result measureDraft_2019_09_Snow(final SnowValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_2019_09);
    }

    public static class JustifyValidator extends ValidatorState {

        public JustifyValidator() {
            super(new JustifyImplementation());
        }
    }

    @Benchmark
    public Result measureDraft_04_Justify(final JustifyValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_04);
    }

    @Benchmark
    public Result measureDraft_06_Justify(final JustifyValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_06);
    }

    @Benchmark
    public Result measureDraft_07_Justify(final JustifyValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_07);
    }

    public static class DevHarrelValidator extends ValidatorState {

        public DevHarrelValidator() {
            super(new DevHarrelImplementation());
        }
    }

    @Benchmark
    public Result measureDraft_07_DevHarrel(final DevHarrelValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_07);
    }

    @Benchmark
    public Result measureDraft_2019_09_DevHarrel(final DevHarrelValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_2019_09);
    }

    @Benchmark
    public Result measureDraft_2020_12_DevHarrel(final DevHarrelValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_2020_12);
    }

    @State(Scope.Benchmark)
    @SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
    abstract static class ValidatorState {

        private final JsonSchemaTestSuite.Runner runner;

        protected ValidatorState(final Implementation implementation) {
            runner = TEST_SUITE.prepare(implementation, new ValidatorState.PreTestPredicate());
        }

        public Result validate(final SchemaSpec spec) {
            return runner.run(spec::equals);
        }

        private static final class PreTestPredicate implements JsonSchemaTestSuite.TestPredicate {
            @Override
            public boolean test(final TestCase testCase) {
                // Only test valid cases during performance testing,
                // As the cost of error handling varies massively between impls.
                // There is a strong correlation between that cost and the richness of error
                // messages.
                // Impls should not be penalised for rich error handling!
                // Production use cases should almost never see validation errors, so its cost isn't
                // that relevant.
                return testCase.valid();
            }
        }
    }
}
