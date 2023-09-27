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

import static java.util.concurrent.TimeUnit.MICROSECONDS;

import org.creekservice.kafka.test.perf.model.ModelState;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.serde.ConfluentSerde;
import org.creekservice.kafka.test.perf.serde.EveritSerde;
import org.creekservice.kafka.test.perf.serde.JacksonIntermediateSerde;
import org.creekservice.kafka.test.perf.serde.JacksonSerde;
import org.creekservice.kafka.test.perf.serde.JustifySerde;
import org.creekservice.kafka.test.perf.serde.MedeiaSerde;
import org.creekservice.kafka.test.perf.serde.NetworkNtSerde;
import org.creekservice.kafka.test.perf.serde.SchemaFriendSerde;
import org.creekservice.kafka.test.perf.serde.SerdeImpl;
import org.creekservice.kafka.test.perf.serde.SkemaSerde;
import org.creekservice.kafka.test.perf.serde.SnowSerde;
import org.creekservice.kafka.test.perf.serde.VertxSerde;
import org.creekservice.kafka.test.perf.util.Logging;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
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
 * <p>Most recent results (On 2021 Macbook, M1 Max: 2.06 - 3.22 GHz, in High Power mode, JDK
 * 17.0.6):
 *
 * <pre>
 * Benchmark                                               Mode  Cnt  Score   Error  Units
 * JsonSerdeBenchmark.measureJacksonIntermediateRoundTrip  avgt   20  3.852 ± 0.063  us/op
 * JsonSerdeBenchmark.measureRawJacksonRoundTrip           avgt   20  3.890 ± 0.047  us/op
 * JsonSerdeBenchmark.measureConfluentRoundTrip            avgt   20  131.029 ±  1.964  us/op
 * JsonSerdeBenchmark.measureEveritRoundTrip               avgt   20  116.423 ±  2.763  us/op
 * JsonSerdeBenchmark.measureJustifyRoundTrip              avgt   20   75.547 ±  0.819  us/op
 * JsonSerdeBenchmark.measureMedeiaRoundTrip               avgt   20   38.443 ±  1.010  us/op
 * JsonSerdeBenchmark.measureNetworkNtRoundTrip            avgt   20  898.339 ± 30.028  us/op
 * JsonSerdeBenchmark.measureSchemaFriendRoundTrip         avgt   20  127.588 ±  0.897  us/op
 * JsonSerdeBenchmark.measureSkemaRoundTrip                avgt   20  111.483 ±  2.036  us/op
 * JsonSerdeBenchmark.measureSnowRoundTrip                 avgt   20  611.803 ±  6.733  us/op
 * JsonSerdeBenchmark.measureVertxRoundTrip                avgt   20  738.511 ± 45.223  us/op
 * </pre>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(MICROSECONDS)
@Threads(4)
@Fork(4) // Note: to debug, set fork to 0.
// @Warmup(iterations = 0, time = 10)
// @Measurement(iterations = 1, time = 10)
@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class JsonSerdeBenchmark {

    static {
        Logging.disable();
    }

    @Benchmark
    public TestModel measureJacksonRoundTrip(final JacksonSerde serde, final ModelState model) {
        return roundTrip(serde, model);
    }

    @Benchmark
    public TestModel measureJacksonIntermediateRoundTrip(
            final JacksonIntermediateSerde serde, final ModelState model) {
        return roundTrip(serde, model);
    }

    @Benchmark
    public TestModel measureMedeiaRoundTrip(final MedeiaSerde serde, final ModelState model) {
        return roundTrip(serde, model);
    }

    @Benchmark
    public TestModel measureEveritRoundTrip(final EveritSerde serde, final ModelState model) {
        return roundTrip(serde, model);
    }

    @Benchmark
    public TestModel measureSkemaRoundTrip(final SkemaSerde serde, final ModelState model) {
        return roundTrip(serde, model);
    }

    @Benchmark
    public TestModel measureConfluentRoundTrip(final ConfluentSerde serde, final ModelState model) {
        return roundTrip(serde, model);
    }

    @Benchmark
    public TestModel measureVertxRoundTrip(final VertxSerde serde, final ModelState model) {
        return roundTrip(serde, model);
    }

    @Benchmark
    public TestModel measureSchemaFriendRoundTrip(
            final SchemaFriendSerde serde, final ModelState model) {
        return roundTrip(serde, model);
    }

    @Benchmark
    public TestModel measureNetworkNtRoundTrip(final NetworkNtSerde serde, final ModelState model) {
        return roundTrip(serde, model);
    }

    @Benchmark
    public TestModel measureSnowRoundTrip(final SnowSerde serde, final ModelState model) {
        return roundTrip(serde, model);
    }

    @Benchmark
    public TestModel measureJustifyRoundTrip(final JustifySerde serde, final ModelState model) {
        return roundTrip(serde, model);
    }

    private TestModel roundTrip(final SerdeImpl serde, final ModelState model) {
        final byte[] serialized = serde.serializer().serialize(model.model, true);
        return serde.deserializer().deserialize(serialized);
    }
}
