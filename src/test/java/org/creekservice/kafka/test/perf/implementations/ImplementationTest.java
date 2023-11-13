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

package org.creekservice.kafka.test.perf.implementations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.creekservice.api.test.util.TestPaths;
import org.creekservice.kafka.test.perf.model.ModelState;
import org.creekservice.kafka.test.perf.model.PolyTypeA;
import org.creekservice.kafka.test.perf.model.PolyTypeB;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.creekservice.kafka.test.perf.util.TestSchemas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class ImplementationTest {

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

    private Implementation impl;
    private TestData testData;
    private AdditionalSchemas additionalSchemas;

    @BeforeEach
    void setUp() throws Exception {
        this.impl = instantiateSerde();
        this.testData = testData(impl);
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

    @Test
    void shouldReturnValueMetaData() {
        assertThat(impl.metadata(), is(notNullValue()));
    }

    @Test
    void shouldNotThrowPreparingValidSchema() {
        impl.prepare(testData.schema, testData.spec, additionalSchemas);
    }

    @SuppressFBWarnings("RV_EXCEPTION_NOT_THROWN")
    @Test
    void shouldThrowValidatingInvalidJson() {
        assumeFalse(
                getClass().getName().contains("Jackson")
                        || getClass().getName().contains("Confluent"),
                "Exclude impls that don't support this");

        // Given:
        final Implementation.JsonValidator validator =
                impl.prepare(testData.schema, testData.spec, additionalSchemas);
        final String badJson =
                new String(validator.serialize(BAD_DECIMAL, false), StandardCharsets.UTF_8);

        // Then:
        assertThrows(RuntimeException.class, () -> validator.validate(badJson));
    }

    @Test
    void shouldNotThrowValidatingValidJson() {
        assumeFalse(
                getClass().getName().contains("Jackson")
                        || getClass().getName().contains("Confluent"),
                "Exclude impls that don't support this");

        // Given:
        final Implementation.JsonValidator validator =
                impl.prepare(testData.schema, testData.spec, additionalSchemas);
        final String goodJson =
                new String(
                        validator.serialize(ModelState.TEST_MODEL, false), StandardCharsets.UTF_8);

        // When:
        validator.validate(goodJson);

        // Then: did not throw.
    }

    @SuppressFBWarnings("RV_EXCEPTION_NOT_THROWN")
    @Test
    void shouldHandleRemoteSchemas() {
        assumeFalse(
                getClass().getName().contains("Jackson")
                        || getClass().getName().contains("Confluent")
                        || getClass().getName().contains("Skema")
                        || getClass().getName().contains("Vert"),
                "Exclude impls that don't support this");

        // Given:
        final Implementation.JsonValidator validator =
                impl.prepare(testData.remoteSchema, testData.spec, additionalSchemas);

        // Then:
        validator.validate("100");
        assertThrows(RuntimeException.class, () -> validator.validate("abc"));
    }

    @Test
    void shouldRoundTrip() {
        // Given:
        final Implementation.JsonValidator validator =
                impl.prepare(testData.schema, testData.spec, additionalSchemas);

        // When:
        final byte[] bytes = validator.serialize(ModelState.TEST_MODEL, true);
        final TestModel result = validator.deserialize(bytes);

        // Then:
        assertThat(result, is(ModelState.TEST_MODEL));
    }

    @SuppressFBWarnings("RV_EXCEPTION_NOT_THROWN")
    @Test
    void shouldValidateOnSerialize() {
        assumeFalse(getClass().getName().contains("Jackson"), "Exclude the raw Jackson serde");

        // Given:
        final Implementation.JsonValidator validator =
                impl.prepare(testData.schema, testData.spec, additionalSchemas);

        // Then:
        assertThrows(RuntimeException.class, () -> validator.serialize(BAD_DECIMAL, true));
    }

    @Test
    void shouldNotValidateOnSerialize() {
        // Given:
        final Implementation.JsonValidator validator =
                impl.prepare(testData.schema, testData.spec, additionalSchemas);

        // When:
        validator.serialize(BAD_DECIMAL, false);

        // Then: did not throw.
    }

    @SuppressFBWarnings("RV_EXCEPTION_NOT_THROWN")
    @Test
    void shouldValidateOnDeserialize() {
        assumeFalse(getClass().getName().contains("Jackson"), "Exclude the raw Jackson serde");

        // Given:
        final Implementation.JsonValidator validator =
                impl.prepare(testData.schema, testData.spec, additionalSchemas);
        final byte[] serialized = validator.serialize(BAD_DECIMAL, false);

        // Then:
        assertThrows(RuntimeException.class, () -> validator.deserialize(serialized));
    }

    private Implementation instantiateSerde() throws Exception {
        final String testName = getClass().getName();
        final String serdeName = testName.substring(0, testName.length() - "Test".length());
        final Class<?> serdeType = getClass().getClassLoader().loadClass(serdeName);
        if (!Implementation.class.isAssignableFrom(serdeType)) {
            throw new AssertionError("Not a serde type: " + serdeType);
        }
        return (Implementation) serdeType.getDeclaredConstructor().newInstance();
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
