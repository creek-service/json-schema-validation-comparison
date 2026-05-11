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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.sjf4j.Sjf4j;
import org.sjf4j.facade.jackson2.Jackson2JsonFacade;
import org.sjf4j.schema.JsonSchema;
import org.sjf4j.schema.SchemaPlan;
import org.sjf4j.schema.SchemaRegistry;

@SuppressWarnings("FieldMayBeFinal")
public class Sjf4jImplementation implements Implementation {

    private static final MetaData METADATA =
            new MetaData(
                    "Simple JSON Facade for Java",
                    "SJF4J",
                    Language.Java,
                    Licence.MIT,
                    Set.of(SchemaSpec.DRAFT_2020_12),
                    "https://github.com/sjf4j-projects/sjf4j",
                    Color.CYAN,
                    Sjf4j.class,
                    MetaData.ACTIVE_PROJECT);

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    private ObjectMapper objectMapper = new ObjectMapper();
    private Sjf4j sjf4j = Sjf4j.builder().jsonFacadeProvider(Jackson2JsonFacade.provider()).build();

    @Override
    public JsonValidator prepare(
            final String schema,
            final SchemaSpec spec,
            final AdditionalSchemas additionalSchemas,
            final boolean enableFormatAssertions) {

        final JsonSchema jsonSchema = JsonSchema.fromJson(schema);

        final SchemaRegistry registry = new SchemaRegistry();
        for (Map.Entry<URI, String> entry : additionalSchemas.remotes().entrySet()) {
            registry.index(entry.getKey(), sjf4j.fromJson(entry.getValue(), JsonSchema.class));
        }
        final SchemaPlan plan = jsonSchema.createPlan(registry);

        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                final Object node = sjf4j.fromJson(json);
                plan.requireValid(node);
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    final byte[] rawBytes = objectMapper.writeValueAsBytes(model);
                    if (validate) {
                        plan.requireValid(model);
                    }
                    return rawBytes;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                try {
                    final TestModel model = objectMapper.readValue(data, TestModel.class);
                    plan.requireValid(model);
                    return model;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
