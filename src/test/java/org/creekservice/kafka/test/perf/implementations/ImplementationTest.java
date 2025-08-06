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

package org.creekservice.kafka.test.perf.implementations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.creekservice.api.test.util.TestPaths;
import org.creekservice.kafka.test.perf.model.ModelState;
import org.creekservice.kafka.test.perf.model.PolyTypeA;
import org.creekservice.kafka.test.perf.model.PolyTypeB;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.util.TestSchemas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ImplementationTest {

    private static final Path REMOTES_ROOT =
            TestPaths.moduleRoot("json-schema-validation-comparison")
                    .resolve("src/test/resources/remotes");

    private static final TestModel BAD_DECIMAL =
            new TestModel(
                    "some name",
                    new BigDecimal("-0145.000001"),
                    TestModel.AnEnum.THAT,
                    List.of("element"),
                    List.of(new PolyTypeA(UUID.randomUUID()), new PolyTypeB(0.0000000002d)));

    private AdditionalSchemas additionalSchemas;

    @BeforeEach
    void setUp() {
        this.additionalSchemas =
                new AdditionalSchemas(
                        Map.of(
                                URI.create("http://localhost:1234/draft2020-12/integer.json"),
                                        TestPaths.readString(
                                                REMOTES_ROOT.resolve("draft2020-12/integer.json")),
                                URI.create("http://localhost:1234/draft7/integer.json"),
                                        TestPaths.readString(
                                                REMOTES_ROOT.resolve("draft7/integer.json"))),
                        REMOTES_ROOT);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void shouldReturnValueMetaData(final String shortName, final Implementation impl) {
        assertThat(impl.metadata(), is(notNullValue()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void shouldNotThrowPreparingValidSchema(final String shortName, final Implementation impl) {
        // Given:
        final TestData testData = testData(impl);

        // When:
        impl.prepare(testData.schema, testData.spec, additionalSchemas, false);

        // Then: did not throw.
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void shouldThrowValidatingInvalidJson(final String shortName, final Implementation impl) {
        assumeFalse(
                shortName.equals("Jackson") || shortName.equals("Confluent"),
                "Exclude impls that don't support this");

        // Given:
        final TestData testData = testData(impl);
        final Implementation.JsonValidator validator =
                impl.prepare(testData.schema, testData.spec, additionalSchemas, false);
        final String badJson =
                new String(validator.serialize(BAD_DECIMAL, false), StandardCharsets.UTF_8);

        // Then:
        assertThrows(RuntimeException.class, () -> validator.validate(badJson));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void shouldNotThrowValidatingValidJson(final String shortName, final Implementation impl) {
        assumeFalse(
                shortName.equals("Jackson") || shortName.equals("Confluent"),
                "Exclude impls that don't support this");

        // Given:
        final TestData testData = testData(impl);
        final Implementation.JsonValidator validator =
                impl.prepare(testData.schema, testData.spec, additionalSchemas, false);
        final String goodJson =
                new String(
                        validator.serialize(ModelState.TEST_MODEL, false), StandardCharsets.UTF_8);

        // When:
        validator.validate(goodJson);

        // Then: did not throw.
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void shouldHandleRemoteSchemas(final String shortName, final Implementation impl) {
        assumeFalse(
                shortName.equals("Jackson")
                        || shortName.equals("Confluent")
                        || shortName.equals("Skema")
                        || shortName.equals("Vertx"),
                "Exclude impls that don't support this");

        // Given:
        final TestData testData = testData(impl);
        final Implementation.JsonValidator validator =
                impl.prepare(testData.remoteSchema, testData.spec, additionalSchemas, false);

        // Then:
        validator.validate("100");
        assertThrows(RuntimeException.class, () -> validator.validate("abc"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void shouldRoundTrip(final String shortName, final Implementation impl) {
        // Given:
        final TestData testData = testData(impl);
        final Implementation.JsonValidator validator =
                impl.prepare(testData.schema, testData.spec, additionalSchemas, false);

        // When:
        final byte[] bytes = validator.serialize(ModelState.TEST_MODEL, true);
        final TestModel result = validator.deserialize(bytes);

        // Then:
        assertThat(result, is(ModelState.TEST_MODEL));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void shouldValidateOnSerialize(final String shortName, final Implementation impl) {
        assumeFalse(shortName.equals("Jackson"), "Exclude the raw Jackson serde");

        // Given:
        final TestData testData = testData(impl);
        final Implementation.JsonValidator validator =
                impl.prepare(testData.schema, testData.spec, additionalSchemas, false);

        // Then:
        assertThrows(RuntimeException.class, () -> validator.serialize(BAD_DECIMAL, true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void shouldNotValidateOnSerialize(final String shortName, final Implementation impl) {
        // Given:
        final TestData testData = testData(impl);
        final Implementation.JsonValidator validator =
                impl.prepare(testData.schema, testData.spec, additionalSchemas, false);

        // When:
        validator.serialize(BAD_DECIMAL, false);

        // Then: did not throw.
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implementations")
    void shouldValidateOnDeserialize(final String shortName, final Implementation impl) {
        assumeFalse(shortName.equals("Jackson"), "Exclude the raw Jackson serde");

        // Given:
        final TestData testData = testData(impl);
        final Implementation.JsonValidator validator =
                impl.prepare(testData.schema, testData.spec, additionalSchemas, false);
        final byte[] serialized = validator.serialize(BAD_DECIMAL, false);

        // Then:
        assertThrows(RuntimeException.class, () -> validator.deserialize(serialized));
    }

    private static Stream<Object[]> implementations() {
        return Implementations.all().stream()
                .map(impl -> new Object[] {impl.metadata().shortName(), impl});
    }

    private TestData testData(final Implementation impl) {
        if (impl.supports(SchemaSpec.DRAFT_07)) {
            return new TestData(
                    SchemaSpec.DRAFT_07,
                    TestSchemas.DRAFT_7_SCHEMA,
                    "{\"$ref\": \"http://localhost:1234/draft7/integer.json\"}");
        }
        if (impl.supports(SchemaSpec.DRAFT_2020_12)) {
            return new TestData(
                    SchemaSpec.DRAFT_2020_12,
                    TestSchemas.DRAFT_2020_SCHEMA,
                    " {\n"
                            + "\t\"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
                            + "\t\"$ref\": \"http://localhost:1234/draft2020-12/integer.json\"\n"
                            + "}");
        }
        throw new UnsupportedOperationException(
                "This test case needs extending to cover the the implementation");
    }

    private static final class TestData {
        final SchemaSpec spec;
        final String schema;
        final String remoteSchema;

        private TestData(final SchemaSpec spec, final String schema, final String remoteSchema) {
            this.spec = spec;
            this.schema = schema;
            this.remoteSchema = remoteSchema;
        }
    }
}
