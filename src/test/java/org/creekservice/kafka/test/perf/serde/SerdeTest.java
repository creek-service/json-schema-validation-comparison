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

package org.creekservice.kafka.test.perf.serde;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.creekservice.kafka.test.perf.model.ModelState;
import org.creekservice.kafka.test.perf.model.PolyTypeA;
import org.creekservice.kafka.test.perf.model.PolyTypeB;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class SerdeTest {

    private static final TestModel BAD_DECIMAL =
            new TestModel(
                    "some name",
                    new BigDecimal("-0145.000001"),
                    TestModel.AnEnum.THAT,
                    List.of("element"),
                    List.of(new PolyTypeA(UUID.randomUUID()), new PolyTypeB(0.0000000002d)));

    private SerdeImpl serde;

    @BeforeEach
    void setUp() throws Exception {
        this.serde = instantiateSerde();
    }

    @Test
    void shouldRoundTrip() {
        final byte[] bytes = serde.serializer().serialize(ModelState.TEST_MODEL, true);
        final TestModel result = serde.deserializer().deserialize(bytes);

        assertThat(result, is(ModelState.TEST_MODEL));
    }

    @Test
    void shouldValidateOnSerialize() {
        assumeFalse(getClass().getName().contains("JacksonSerde"), "Exclude the raw Jackson serde");

        final Exception e =
                assertThrows(
                        RuntimeException.class,
                        () -> serde.serializer().serialize(BAD_DECIMAL, true));
        System.out.println(e.getMessage());
    }

    @Test
    void shouldValidateOnDeserialize() {
        assumeFalse(getClass().getName().contains("JacksonSerde"), "Exclude the raw Jackson serde");

        final byte[] serialized = serde.serializer().serialize(BAD_DECIMAL, false);
        final Exception e =
                assertThrows(
                        RuntimeException.class, () -> serde.deserializer().deserialize(serialized));
        System.out.println(e.getMessage());
    }

    private SerdeImpl instantiateSerde() throws Exception {
        final String testName = getClass().getName();
        final String serdeName = testName.substring(0, testName.length() - "Test".length());
        final Class<?> serdeType = getClass().getClassLoader().loadClass(serdeName);
        if (!SerdeImpl.class.isAssignableFrom(serdeType)) {
            throw new AssertionError("Not a serde type: " + serdeType);
        }
        return (SerdeImpl) serdeType.getDeclaredConstructor().newInstance();
    }
}
