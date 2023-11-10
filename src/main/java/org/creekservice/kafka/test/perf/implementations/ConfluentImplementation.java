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

import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_04;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_06;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_07;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializerConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

@SuppressWarnings("resource")
public class ConfluentImplementation implements Implementation {

    private static final MetaData METADATA =
            new MetaData(
                    "Confluent validating JSON serde",
                    "Confluent",
                    Language.Java,
                    Licence.Apache_v2_0,
                    Set.of(DRAFT_04, DRAFT_06, DRAFT_07),
                    "https://docs.confluent.io/platform/current/schema-registry/fundamentals/serdes-develop/serdes-json.html");

    private static final String TOPIC_NAME = "t";

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {
        try {
            final Optional<ParsedSchema> parsedSchema =
                    new JsonSchemaProvider().parseSchema(schema, List.of());
            final MockSchemaRegistryClient srClient = new MockSchemaRegistryClient();
            final int schemaId =
                    srClient.register(TOPIC_NAME + "-value", parsedSchema.orElseThrow());

            final Map<String, Object> validating = new HashMap<>();
            validating.put(KafkaJsonSchemaSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "ignored");
            validating.put(KafkaJsonSchemaSerializerConfig.FAIL_INVALID_SCHEMA, true);
            validating.put(KafkaJsonSchemaSerializerConfig.AUTO_REGISTER_SCHEMAS, false);
            validating.put(KafkaJsonSchemaSerializerConfig.USE_SCHEMA_ID, schemaId);
            validating.put(KafkaJsonSchemaSerializerConfig.ID_COMPATIBILITY_STRICT, false);

            final Serializer<TestModel> serializer =
                    new KafkaJsonSchemaSerializer<>(srClient, validating);
            final Deserializer<TestModel> deserializer =
                    new KafkaJsonSchemaDeserializer<>(srClient, validating, TestModel.class);

            final Map<String, Object> nonValidating = new HashMap<>(validating);
            nonValidating.put(KafkaJsonSchemaSerializerConfig.FAIL_INVALID_SCHEMA, false);
            final Serializer<TestModel> nonValidatingSerializer =
                    new KafkaJsonSchemaSerializer<>(srClient, nonValidating);

            return new JsonValidator() {
                @Override
                public void validate(final String json) {
                    throw new UnsupportedOperationException("Not under test");
                }

                @Override
                public byte[] serialize(final TestModel model, final boolean validate) {
                    return validate
                            ? serializer.serialize(TOPIC_NAME, model)
                            : nonValidatingSerializer.serialize(TOPIC_NAME, model);
                }

                @Override
                public TestModel deserialize(final byte[] data) {
                    return deserializer.deserialize(TOPIC_NAME, data);
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Final, empty finalize method stops spotbugs CT_CONSTRUCTOR_THROW
    // Can be moved to base type after https://github.com/spotbugs/spotbugs/issues/2665
    @Override
    @SuppressWarnings({"deprecation", "Finalize"})
    protected final void finalize() {}
}
