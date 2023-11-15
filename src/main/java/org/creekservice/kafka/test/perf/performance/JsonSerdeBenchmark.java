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

package org.creekservice.kafka.test.perf.performance;

import static java.util.concurrent.TimeUnit.MICROSECONDS;

import java.nio.file.Path;
import java.util.Map;
import org.creekservice.kafka.test.perf.implementations.DevHarrelImplementation;
import org.creekservice.kafka.test.perf.implementations.EveritImplementation;
import org.creekservice.kafka.test.perf.implementations.Implementation;
import org.creekservice.kafka.test.perf.implementations.JacksonImplementation;
import org.creekservice.kafka.test.perf.implementations.JustifyImplementation;
import org.creekservice.kafka.test.perf.implementations.MedeiaImplementation;
import org.creekservice.kafka.test.perf.implementations.NetworkNtImplementation;
import org.creekservice.kafka.test.perf.implementations.SchemaFriendImplementation;
import org.creekservice.kafka.test.perf.implementations.SkemaImplementation;
import org.creekservice.kafka.test.perf.implementations.SnowImplementation;
import org.creekservice.kafka.test.perf.implementations.VertxImplementation;
import org.creekservice.kafka.test.perf.model.ModelState;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.util.Logging;
import org.creekservice.kafka.test.perf.util.TestSchemas;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

/**
 * Benchmark results for JSON Serde.
 *
 * <p>The benchmark serializes and deserializes roughly 1K of fairly simple JSON. Testing the
 * performance of different JSON parsers/generators and schema validators.
 *
 * <p>The JSON / Model / Schema is deliberately simplistic, as Kafka/Creek use-cases tend to only
 * use the basic JSON schema features: primitives, enums, arrays, polymorphic types and length
 * assertions. This can be extended in the future it needed.
 *
 * <p>Benchmark methods should be added for Draft_7 and Draft_2020_12 for each implementation that
 * supports them.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(MICROSECONDS)
@Threads(4)
@Fork(4) // Note: to debug, set fork to 0.
// @Warmup(iterations = 0, time = 10)
// @Measurement(iterations = 1, time = 10)
@SuppressWarnings({"FieldMayBeFinal", "MethodName"}) // not final to avoid folding.
public class JsonSerdeBenchmark {

    static {
        Logging.disable();
    }

    public static class JacksonState extends ImplementationState {
        public JacksonState() {
            super(new JacksonImplementation());
        }
    }

    @Benchmark
    public TestModel measureDraft_07_Jackson(final JacksonState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_07);
    }

    @Benchmark
    public TestModel measureDraft_2020_12_Jackson(final JacksonState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_2020_12);
    }

    public static class MedeiaState extends ImplementationState {
        public MedeiaState() {
            super(new MedeiaImplementation());
        }
    }

    @Benchmark
    public TestModel measureDraft_07_Medeia(final MedeiaState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_07);
    }

    public static class EveritState extends ImplementationState {
        public EveritState() {
            super(new EveritImplementation());
        }
    }

    @Benchmark
    public TestModel measureDraft_07_Everit(final EveritState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_07);
    }

    public static class SkemaState extends ImplementationState {
        public SkemaState() {
            super(new SkemaImplementation());
        }
    }

    @Benchmark
    public TestModel measureDraft_2020_12_Skema(final SkemaState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_2020_12);
    }

    public static class VertxState extends ImplementationState {
        public VertxState() {
            super(new VertxImplementation());
        }
    }

    @Benchmark
    public TestModel measureDraft_07_Vertx(final VertxState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_07);
    }

    @Benchmark
    public TestModel measureDraft_2020_12_Vertx(final VertxState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_2020_12);
    }

    public static class SchemaFriendState extends ImplementationState {
        public SchemaFriendState() {
            super(new SchemaFriendImplementation());
        }
    }

    @Benchmark
    public TestModel measureDraft_07_SchemaFriend(
            final SchemaFriendState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_07);
    }

    @Benchmark
    public TestModel measureDraft_2020_12_SchemaFriend(
            final SchemaFriendState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_2020_12);
    }

    public static class NetworkNtState extends ImplementationState {
        public NetworkNtState() {
            super(new NetworkNtImplementation());
        }
    }

    @Benchmark
    public TestModel measureDraft_07_NetworkNt(final NetworkNtState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_07);
    }

    @Benchmark
    public TestModel measureDraft_2020_12_NetworkNt(
            final NetworkNtState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_2020_12);
    }

    public static class SnowState extends ImplementationState {
        public SnowState() {
            super(new SnowImplementation());
        }
    }

    @Benchmark
    public TestModel measureDraft_07_Snow(final SnowState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_07);
    }

    public static class JustifyState extends ImplementationState {
        public JustifyState() {
            super(new JustifyImplementation());
        }
    }

    @Benchmark
    public TestModel measureDraft_07_Justify(final JustifyState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_07);
    }

    public static class DevHarrelState extends ImplementationState {
        public DevHarrelState() {
            super(new DevHarrelImplementation());
        }
    }

    @Benchmark
    public TestModel measureDraft_2020_12_DevHarrel(
            final DevHarrelState impl, final ModelState model) {
        return impl.roundTrip(model, SchemaSpec.DRAFT_2020_12);
    }

    @State(Scope.Thread)
    private static class ImplementationState {

        private final Implementation.JsonValidator validator07;
        private final Implementation.JsonValidator validator2020;

        ImplementationState(final Implementation impl) {
            this.validator07 =
                    impl.supports(SchemaSpec.DRAFT_07)
                            ? impl.prepare(
                                    TestSchemas.DRAFT_7_SCHEMA,
                                    SchemaSpec.DRAFT_07,
                                    new AdditionalSchemas(Map.of(), Path.of("")))
                            : null;

            this.validator2020 =
                    impl.supports(SchemaSpec.DRAFT_2020_12)
                            ? impl.prepare(
                                    TestSchemas.DRAFT_2020_SCHEMA,
                                    SchemaSpec.DRAFT_2020_12,
                                    new AdditionalSchemas(Map.of(), Path.of("")))
                            : null;

            if (validator07 == null && validator2020 == null) {
                throw new UnsupportedOperationException(
                        "Benchmark code needs enhancing to cover this case.");
            }
        }

        public TestModel roundTrip(final ModelState model, final SchemaSpec version) {
            final Implementation.JsonValidator validator = validator(version);
            final byte[] serialized = validator.serialize(model.model, true);
            return validator.deserialize(serialized);
        }

        private Implementation.JsonValidator validator(final SchemaSpec version) {
            switch (version) {
                case DRAFT_07:
                    return validator07;
                case DRAFT_2020_12:
                    return validator2020;
                default:
                    throw new UnsupportedOperationException(
                            "Benchmark code needs enhancing to cover this case.");
            }
        }
    }
}
