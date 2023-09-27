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

package org.creekservice.kafka.test.perf;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.creekservice.kafka.test.perf.testsuite.JsonSchemaTestSuite.Result;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.util.Logging;
import org.creekservice.kafka.test.perf.validator.EveritValidator;
import org.creekservice.kafka.test.perf.validator.JustifyValidator;
import org.creekservice.kafka.test.perf.validator.MedeiaValidator;
import org.creekservice.kafka.test.perf.validator.NetworkNtValidator;
import org.creekservice.kafka.test.perf.validator.SchemaFriendValidator;
import org.creekservice.kafka.test.perf.validator.SkemaValidator;
import org.creekservice.kafka.test.perf.validator.SnowValidator;
import org.creekservice.kafka.test.perf.validator.VertxValidator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;

/**
 * Benchmark results for JSON Schema validation.
 *
 * <p>The benchmark runs each validator through the <a
 * href="https://github.com/json-schema-org/JSON-Schema-Test-Suite">standard set of tests</a>
 *
 * <p>The results show the average time it take each impl to run through the test suite, per draft.
 *
 * <p>Most recent results (On 2021 Macbook, M1 Max: 2.06 - 3.22 GHz, in High Power mode, JDK
 * 17.0.6):
 *
 * <pre>
 * Benchmark                                                Mode  Cnt     Score    Error  Units
 * JsonValidateBenchmark.measureDraft_2019_09_NetworkNt     avgt   20     6.017 ±  0.216  ms/op
 * JsonValidateBenchmark.measureDraft_2019_09_SchemaFriend  avgt   20     1.482 ±  0.005  ms/op
 * JsonValidateBenchmark.measureDraft_2019_09_Snow          avgt   20   316.178 ± 28.242  ms/op
 * JsonValidateBenchmark.measureDraft_2019_09_Vertx         avgt   20     3.818 ±  0.028  ms/op
 * JsonValidateBenchmark.measureDraft_2020_12_NetworkNt     avgt   20     7.305 ±  0.073  ms/op
 * JsonValidateBenchmark.measureDraft_2020_12_SchemaFriend  avgt   20     1.654 ±  0.005  ms/op
 * JsonValidateBenchmark.measureDraft_2020_12_Skema         avgt   20     2.812 ±  0.015  ms/op
 * JsonValidateBenchmark.measureDraft_2020_12_Vertx         avgt   20     3.669 ±  0.019  ms/op
 * JsonValidateBenchmark.measureDraft_3_SchemaFriend        avgt   20     0.235 ±  0.005  ms/op
 * JsonValidateBenchmark.measureDraft_4_Everit              avgt   20     0.328 ±  0.006  ms/op
 * JsonValidateBenchmark.measureDraft_4_Justify             avgt   20     0.634 ±  0.009  ms/op
 * JsonValidateBenchmark.measureDraft_4_Medeia              avgt   20     0.346 ±  0.006  ms/op
 * JsonValidateBenchmark.measureDraft_4_NetworkNt           avgt   20     1.086 ±  0.004  ms/op
 * JsonValidateBenchmark.measureDraft_4_SchemaFriend        avgt   20     0.480 ±  0.017  ms/op
 * JsonValidateBenchmark.measureDraft_4_Vertx               avgt   20     1.362 ±  0.006  ms/op
 * JsonValidateBenchmark.measureDraft_6_Everit              avgt   20     0.400 ±  0.003  ms/op
 * JsonValidateBenchmark.measureDraft_6_Justify             avgt   20     0.816 ±  0.008  ms/op
 * JsonValidateBenchmark.measureDraft_6_Medeia              avgt   20     0.416 ±  0.007  ms/op
 * JsonValidateBenchmark.measureDraft_6_NetworkNt           avgt   20     1.771 ±  0.044  ms/op
 * JsonValidateBenchmark.measureDraft_6_SchemaFriend        avgt   20     0.700 ±  0.018  ms/op
 * JsonValidateBenchmark.measureDraft_6_Snow                avgt   20    78.241 ±  6.515  ms/op
 * JsonValidateBenchmark.measureDraft_7_Everit              avgt   20     0.508 ±  0.005  ms/op
 * JsonValidateBenchmark.measureDraft_7_Justify             avgt   20     1.044 ±  0.019  ms/op
 * JsonValidateBenchmark.measureDraft_7_Medeia              avgt   20     0.666 ±  0.007  ms/op
 * JsonValidateBenchmark.measureDraft_7_NetworkNt           avgt   20     2.573 ±  0.032  ms/op
 * JsonValidateBenchmark.measureDraft_7_SchemaFriend        avgt   20     0.918 ±  0.012  ms/op
 * JsonValidateBenchmark.measureDraft_7_Snow                avgt   20    76.627 ±  6.336  ms/op
 * JsonValidateBenchmark.measureDraft_7_Vertx               avgt   20     2.141 ±  0.072  ms/op
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(MILLISECONDS)
@Threads(4)
@Fork(4) // Note: to debug, set fork to 0.
// @Warmup(iterations = 0, time = 10)
// @Measurement(iterations = 1, time = 10)
@SuppressWarnings({"FieldMayBeFinal", "MethodName"}) // not final to avoid folding.
public class JsonValidateBenchmark {

    static {
        Logging.disable();
    }

    @Benchmark
    public Result measureDraft_4_Medeia(final MedeiaValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_04);
    }

    @Benchmark
    public Result measureDraft_6_Medeia(final MedeiaValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_06);
    }

    @Benchmark
    public Result measureDraft_7_Medeia(final MedeiaValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_07);
    }

    @Benchmark
    public Result measureDraft_4_Everit(final EveritValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_04);
    }

    @Benchmark
    public Result measureDraft_6_Everit(final EveritValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_06);
    }

    @Benchmark
    public Result measureDraft_7_Everit(final EveritValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_07);
    }

    @Benchmark
    public Result measureDraft_2020_12_Skema(final SkemaValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_2020_12);
    }

    @Benchmark
    public Result measureDraft_4_Vertx(final VertxValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_04);
    }

    @Benchmark
    public Result measureDraft_7_Vertx(final VertxValidator validator) {
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

    @Benchmark
    public Result measureDraft_3_SchemaFriend(final SchemaFriendValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_03);
    }

    @Benchmark
    public Result measureDraft_4_SchemaFriend(final SchemaFriendValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_04);
    }

    @Benchmark
    public Result measureDraft_6_SchemaFriend(final SchemaFriendValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_06);
    }

    @Benchmark
    public Result measureDraft_7_SchemaFriend(final SchemaFriendValidator validator) {
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

    @Benchmark
    public Result measureDraft_4_NetworkNt(final NetworkNtValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_04);
    }

    @Benchmark
    public Result measureDraft_6_NetworkNt(final NetworkNtValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_06);
    }

    @Benchmark
    public Result measureDraft_7_NetworkNt(final NetworkNtValidator validator) {
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

    @Benchmark
    public Result measureDraft_6_Snow(final SnowValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_06);
    }

    @Benchmark
    public Result measureDraft_7_Snow(final SnowValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_07);
    }

    @Benchmark
    public Result measureDraft_2019_09_Snow(final SnowValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_2019_09);
    }

    @Benchmark
    public Result measureDraft_4_Justify(final JustifyValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_04);
    }

    @Benchmark
    public Result measureDraft_6_Justify(final JustifyValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_06);
    }

    @Benchmark
    public Result measureDraft_7_Justify(final JustifyValidator validator) {
        return validator.validate(SchemaSpec.DRAFT_07);
    }
}
