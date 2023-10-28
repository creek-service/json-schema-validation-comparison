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
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.creekservice.kafka.test.perf.TestSchemas;
import org.creekservice.kafka.test.perf.model.TestModel;

@SuppressWarnings("resource")
public class ConfluentSerde extends SerdeImpl {

    private static final String TOPIC_NAME = "t";
    private final Serializer<TestModel> serializer;
    private final Deserializer<TestModel> deserializer;
    private final Serializer<TestModel> nonValidatingSerializer;
    public MockSchemaRegistryClient srClient = new MockSchemaRegistryClient();

    public ConfluentSerde() {
        try {
            final Optional<ParsedSchema> parsedSchema =
                    new JsonSchemaProvider().parseSchema(TestSchemas.DRAFT_7_SCHEMA, List.of());
            final int schemaId =
                    srClient.register(TOPIC_NAME + "-value", parsedSchema.orElseThrow());

            final Map<String, Object> validating = new HashMap<>();
            validating.put(KafkaJsonSchemaSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "ignored");
            validating.put(KafkaJsonSchemaSerializerConfig.FAIL_INVALID_SCHEMA, true);
            validating.put(KafkaJsonSchemaSerializerConfig.AUTO_REGISTER_SCHEMAS, false);
            validating.put(KafkaJsonSchemaSerializerConfig.USE_SCHEMA_ID, schemaId);
            validating.put(KafkaJsonSchemaSerializerConfig.ID_COMPATIBILITY_STRICT, false);

            serializer = new KafkaJsonSchemaSerializer<>(srClient, validating);
            deserializer = new KafkaJsonSchemaDeserializer<>(srClient, validating, TestModel.class);

            final Map<String, Object> nonValidating = new HashMap<>(validating);
            nonValidating.put(KafkaJsonSchemaSerializerConfig.FAIL_INVALID_SCHEMA, false);
            nonValidatingSerializer = new KafkaJsonSchemaSerializer<>(srClient, nonValidating);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public org.creekservice.kafka.test.perf.serde.Serializer serializer() {
        return (model, validate) ->
                validate
                        ? serializer.serialize(TOPIC_NAME, model)
                        : nonValidatingSerializer.serialize(TOPIC_NAME, model);
    }

    @Override
    public org.creekservice.kafka.test.perf.serde.Deserializer deserializer() {
        return data -> deserializer.deserialize(TOPIC_NAME, data);
    }

    // Final, empty finalize method stops spotbugs CT_CONSTRUCTOR_THROW
    // Can be moved to base type after https://github.com/spotbugs/spotbugs/issues/2665
    @Override
    @SuppressWarnings({"deprecation", "Finalize"})
    protected final void finalize() {}
}
