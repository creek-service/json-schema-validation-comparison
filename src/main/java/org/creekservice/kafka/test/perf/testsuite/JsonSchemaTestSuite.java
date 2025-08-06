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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.creekservice.kafka.test.perf.implementations.Implementation;
import org.creekservice.kafka.test.perf.implementations.Implementation.JsonValidator;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.util.Executable;

public final class JsonSchemaTestSuite {

    private final List<SpecTestSuites> tests;
    private final AdditionalSchemas additionalSchemas;

    public JsonSchemaTestSuite(
            final Collection<SpecTestSuites> tests,
            final Map<URI, String> remotes,
            final Path remotesDir) {
        this.tests = List.copyOf(requireNonNull(tests, "tests"));
        this.additionalSchemas = new AdditionalSchemas(remotes, remotesDir);
    }

    public interface TestPredicate {
        default boolean test(SchemaSpec spec) {
            return true;
        }

        default boolean test(TestSuite testSuite) {
            return true;
        }

        default boolean test(TestCase testCase) {
            return true;
        }

        TestPredicate ALL = new TestPredicate() {};
    }

    public interface Runner {
        Result run(Predicate<SchemaSpec> spec);
    }

    public Runner prepare(final Implementation implementation, final TestPredicate testPredicate) {
        final Map<SchemaSpec, Executable<SpecResult>> prepared =
                tests.stream()
                        .filter(suites -> testPredicate.test(suites.spec()))
                        .filter(suites -> implementation.supports(suites.spec()))
                        .collect(
                                Collectors.toMap(
                                        SpecTestSuites::spec,
                                        suites ->
                                                prepareSpecSuites(
                                                        suites, implementation, testPredicate)));

        return specPredicate -> {
            final Instant start = Instant.now();

            final List<SpecResult> results =
                    prepared.entrySet().stream()
                            .filter(e -> specPredicate.test(e.getKey()))
                            .map(Map.Entry::getValue)
                            .map(Executable::exec)
                            .collect(Collectors.toList());

            return new Result(Duration.between(start, Instant.now()), results);
        };
    }

    private Executable<SpecResult> prepareSpecSuites(
            final SpecTestSuites specSuites,
            final Implementation implementation,
            final TestPredicate testPredicate) {
        final List<Executable<List<TestResult>>> prepared =
                specSuites.testSuites().stream()
                        .filter(testPredicate::test)
                        .map(
                                suite ->
                                        prepareSuite(
                                                specSuites.spec(),
                                                suite,
                                                implementation,
                                                testPredicate))
                        .collect(Collectors.toList());

        return () -> {
            final List<TestResult> results =
                    prepared.stream()
                            .map(Executable::exec)
                            .flatMap(List::stream)
                            .collect(Collectors.toList());

            return new SpecResult(specSuites.spec(), results);
        };
    }

    private Executable<List<TestResult>> prepareSuite(
            final SchemaSpec spec,
            final TestSuite suite,
            final Implementation implementation,
            final TestPredicate testPredicate) {

        final JsonValidator validator = prepareValidator(spec, suite, implementation);

        return () ->
                suite.tests().stream()
                        .filter(testPredicate::test)
                        .map(test -> runTest(validator, test, suite))
                        .collect(Collectors.toList());
    }

    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "Known not to be null")
    private JsonValidator prepareValidator(
            final SchemaSpec spec, final TestSuite suite, final Implementation implementation) {
        try {
            final boolean format =
                    Paths.get("format").equals(suite.filePath().getParent().getFileName());
            return implementation.prepare(
                    suite.schema(), spec, additionalSchemas, suite.optional() && format);
        } catch (final Throwable t) {
            final RuntimeException e = new RuntimeException("Failed to build validator", t);
            return new JsonValidator() {
                @Override
                public void validate(final String json) {
                    throw e;
                }

                @Override
                public byte[] serialize(final TestModel model, final boolean validate) {
                    throw e;
                }

                @Override
                public TestModel deserialize(final byte[] data) {
                    throw e;
                }
            };
        }
    }

    private TestResult runTest(
            final JsonValidator validator, final TestCase test, final TestSuite suite) {
        try {
            validator.validate(test.getData());
            return test.valid()
                    ? TestResult.pass(test, suite)
                    : TestResult.fail(test, suite, "Passed when it should have failed");
        } catch (final Exception e) {
            return test.valid()
                    ? TestResult.fail(test, suite, e.getMessage() + " ")
                    : TestResult.pass(test, suite);
        } catch (final Throwable t) {
            return TestResult.error(test, suite, t);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static final class TestResult {
        private final TestCase test;
        private final TestSuite suite;
        private final Optional<Throwable> error;
        private final Optional<String> failure;

        public static TestResult pass(final TestCase test, final TestSuite suite) {
            return new TestResult(test, suite, Optional.empty(), Optional.empty());
        }

        public static TestResult fail(
                final TestCase test, final TestSuite suite, final String failure) {
            if (failure.isEmpty()) {
                throw new IllegalArgumentException("failure message must be supplied");
            }
            return new TestResult(test, suite, Optional.empty(), Optional.of(failure));
        }

        public static TestResult error(
                final TestCase test, final TestSuite suite, final Throwable e) {
            return new TestResult(test, suite, Optional.of(e), Optional.empty());
        }

        private TestResult(
                final TestCase test,
                final TestSuite suite,
                final Optional<Throwable> error,
                final Optional<String> failure) {
            this.test = requireNonNull(test, "test");
            this.suite = requireNonNull(suite, "suite");
            this.error = requireNonNull(error, "error");
            this.failure = requireNonNull(failure, "failure");
        }

        public boolean error() {
            return error.isPresent();
        }

        public boolean failed() {
            return failure.isPresent();
        }

        public boolean optional() {
            return suite.optional();
        }

        public TestSuite suite() {
            return suite;
        }

        public TestCase test() {
            return test;
        }
    }

    private static final class SpecResult {
        private final SchemaSpec spec;
        private final List<TestResult> results;

        SpecResult(final SchemaSpec spec, final List<TestResult> results) {
            this.spec = requireNonNull(spec, "spec");
            this.results = List.copyOf(requireNonNull(results, "results"));
        }

        public void visit(final Visitor visitor) {
            results.forEach(r -> visitor.accept(spec, r));
        }

        public interface Visitor {

            void accept(SchemaSpec spec, TestResult result);
        }
    }

    public static final class Result {

        private final Duration duration;
        private final List<SpecResult> results;

        public Result(final Duration duration, final List<SpecResult> results) {
            this.duration = requireNonNull(duration, "duration");
            this.results = List.copyOf(requireNonNull(results, "results"));
        }

        public void visit(final Visitor visitor) {
            results.forEach(r -> r.visit(visitor::accept));
        }

        public Duration duration() {
            return duration;
        }

        public interface Visitor {

            void accept(SchemaSpec spec, TestResult result);
        }
    }
}
