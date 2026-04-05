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

import java.awt.Color;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.sjf4j.Sjf4j;
import org.sjf4j.facade.FacadeFactory;
import org.sjf4j.facade.JsonFacade;
import org.sjf4j.schema.JsonSchema;
import org.sjf4j.schema.ObjectSchema;
import org.sjf4j.schema.SchemaStore;

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

    private final JsonFacade<?, ?> facade;

    public Sjf4jImplementation() {
        this.facade = FacadeFactory.createFastjson2Facade();
    }

    @Override
    public JsonValidator prepare(
            final String schema,
            final SchemaSpec spec,
            final AdditionalSchemas additionalSchemas,
            final boolean enableFormatAssertions) {

        final JsonSchema jsonSchema = JsonSchema.fromJson(schema);

        final SchemaStore store = new SchemaStore();
        for (Map.Entry<URI, String> entry : additionalSchemas.remotes().entrySet()) {
            store.register(entry.getKey(), (ObjectSchema) JsonSchema.fromJson(entry.getValue()));
        }
        jsonSchema.compile(store);

        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                final Object node = facade.readNode(json, Object.class);
                jsonSchema.validateOrThrow(node);
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                final byte[] result = Sjf4j.toJsonBytes(model);
                if (validate) {
                    jsonSchema.validateOrThrow(facade.readNode(result, Object.class));
                }
                return result;
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                final TestModel tm = Sjf4j.fromJson(data, TestModel.class);
                jsonSchema.validateOrThrow(facade.readNode(data, Object.class));
                return tm;
            }
        };
    }
}
