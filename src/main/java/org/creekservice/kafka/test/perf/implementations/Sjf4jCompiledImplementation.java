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
import org.sjf4j.CompiledInstances;
import org.sjf4j.Sjf4j;
import org.sjf4j.annotation.binding.Backend;
import org.sjf4j.annotation.binding.CompiledBinder;
import org.sjf4j.annotation.binding.Read;
import org.sjf4j.annotation.binding.Write;
import org.sjf4j.annotation.schema.CompiledValidator;
import org.sjf4j.annotation.schema.ValidJsonSchema;
import org.sjf4j.schema.JsonSchema;
import org.sjf4j.schema.SchemaDialect;
import org.sjf4j.schema.SchemaPlan;
import org.sjf4j.schema.SchemaRegistry;

@SuppressWarnings("FieldMayBeFinal")
public class Sjf4jCompiledImplementation implements Implementation {

    private static final MetaData METADATA =
            new MetaData(
                    "Compile-time generated implementation powered by SJF4J",
                    "Sjf4jCompiled",
                    Language.Java,
                    Licence.MIT,
                    Set.of(SchemaSpec.DRAFT_2020_12, SchemaSpec.DRAFT_2019_09, SchemaSpec.DRAFT_07),
                    "https://github.com/sjf4j-projects/sjf4j",
                    new Color(0, 170, 210),
                    Sjf4j.class,
                    MetaData.ACTIVE_PROJECT);

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    private Sjf4j sjf4j = Sjf4j.global();

    @Override
    public JsonValidator prepare(
            final String schema,
            final SchemaSpec spec,
            final AdditionalSchemas additionalSchemas,
            final boolean enableFormatAssertions) {

        final JsonSchema jsonSchema = JsonSchema.fromJson(schema);

        final SchemaRegistry registry =
                switch (spec) {
                    case DRAFT_2020_12 -> new SchemaRegistry(SchemaDialect.DRAFT_2020_12);
                    case DRAFT_2019_09 -> new SchemaRegistry(SchemaDialect.DRAFT_2019_09);
                    case DRAFT_07 -> new SchemaRegistry(SchemaDialect.DRAFT_07);
                    default -> throw new RuntimeException("Unsupported schema spec: " + spec);
                };

        for (Map.Entry<URI, String> entry : additionalSchemas.remotes().entrySet()) {
            registry.index(entry.getKey(), JsonSchema.fromJson(entry.getValue()));
        }
        final SchemaPlan plan = jsonSchema.createPlan(registry);

        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                final Object node = sjf4j.fromJson(json);
                plan.requireValid(node, enableFormatAssertions);
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                final byte[] rawBytes = TestModelBinder.INSTANCE.serialize(model);
                if (validate) {
                    TestModelValidator.INSTANCE.requireValid(model, enableFormatAssertions);
                }
                return rawBytes;
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                final TestModel model = TestModelBinder.INSTANCE.deserialize(data);
                TestModelValidator.INSTANCE.requireValid(model, enableFormatAssertions);
                return model;
            }
        };
    }

    @CompiledBinder(backend = Backend.JACKSON2)
    interface TestModelBinder {
        TestModelBinder INSTANCE = CompiledInstances.create(TestModelBinder.class);

        @Write
        byte[] serialize(TestModel model);

        @Read
        TestModel deserialize(byte[] data);
    }

    @CompiledValidator
    interface TestModelValidator {
        TestModelValidator INSTANCE = CompiledInstances.create(TestModelValidator.class);

        @ValidJsonSchema(ref = "classpath:/schema-draft-2020-12.json")
        void requireValid(TestModel model, boolean strictFormat);
    }
}
