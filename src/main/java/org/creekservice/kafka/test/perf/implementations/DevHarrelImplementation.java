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

import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_2019_09;
import static org.creekservice.kafka.test.perf.testsuite.SchemaSpec.DRAFT_2020_12;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.harrel.jsonschema.Dialects;
import dev.harrel.jsonschema.JsonNode;
import dev.harrel.jsonschema.SchemaResolver;
import dev.harrel.jsonschema.SpecificationVersion;
import dev.harrel.jsonschema.Validator;
import dev.harrel.jsonschema.providers.JacksonNode;
import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class DevHarrelImplementation implements Implementation {

    private static final MetaData METADATA =
            new MetaData(
                    "json-schema (dev.harrel)",
                    "DevHarrel",
                    Language.Java,
                    Licence.MIT,
                    Set.of(DRAFT_2020_12, DRAFT_2019_09),
                    "https://github.com/harrel56/json-schema",
                    new Color(22, 99, 0),
                    dev.harrel.jsonschema.ValidatorFactory.class,
                    MetaData.ACTIVE_PROJECT);

    private ObjectMapper mapper = JsonMapper.builder().build();

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema, final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {

        final Validator validator = validator(spec, additionalSchemas);
        final URI schemaUri = validator.registerSchema(schema);

        return new JsonValidator() {

            @Override
            public void validate(final String json) {
                final Validator.Result result = validator.validate(schemaUri, json);
                if (!result.isValid()) {
                    throw new RuntimeException(result.getErrors().get(0).getError());
                }
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    final com.fasterxml.jackson.databind.JsonNode node =
                            mapper.convertValue(
                                    model, com.fasterxml.jackson.databind.JsonNode.class);
                    final Validator.Result result = validator.validate(schemaUri, node);
                    if (validate && !result.isValid()) {
                        throw new RuntimeException(result.getErrors().get(0).getError());
                    }
                    return mapper.writeValueAsBytes(node);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                try {
                    final com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(data);
                    final Validator.Result result = validator.validate(schemaUri, node);
                    if (!result.isValid()) {
                        throw new RuntimeException(result.getErrors().get(0).getError());
                    }
                    return mapper.convertValue(node, TestModel.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private Validator validator(final SchemaSpec spec, final AdditionalSchemas additionalSchemas) {
        final JacksonNode.Factory nodeFactory = new JacksonNode.Factory();
        final Map<String, JsonNode> remotes =
                additionalSchemas.remotes().entrySet().stream()
                        .collect(
                                Collectors.toMap(
                                        e -> e.getKey().toString(),
                                        e -> nodeFactory.create(e.getValue())));
        final SchemaResolver resolver =
                uri -> {
                    final JsonNode schema = remotes.get(uri);
                    if (schema != null) {
                        return SchemaResolver.Result.fromJsonNode(schema);
                    }
                    return SchemaResolver.Result.empty();
                };
        switch (spec) {
            case DRAFT_2020_12:
                final Validator validator2020 =
                        new dev.harrel.jsonschema.ValidatorFactory()
                                .withDisabledSchemaValidation(true)
                                .withDialect(new Dialects.Draft2020Dialect())
                                .withJsonNodeFactory(nodeFactory)
                                .withSchemaResolver(resolver)
                                .createValidator();
                /* Validate against meta-schema in order to parse it eagerly */
                validator2020.validate(URI.create(SpecificationVersion.DRAFT2020_12.getId()), "{}");
                return validator2020;
            case DRAFT_2019_09:
                final Validator validator2019 =
                        new dev.harrel.jsonschema.ValidatorFactory()
                                .withDisabledSchemaValidation(true)
                                .withDialect(new Dialects.Draft2019Dialect())
                                .withJsonNodeFactory(nodeFactory)
                                .withSchemaResolver(resolver)
                                .createValidator();
                /* Validate against meta-schema in order to parse it eagerly */
                validator2019.validate(URI.create(SpecificationVersion.DRAFT2019_09.getId()), "{}");
                return validator2019;
            default:
                throw new RuntimeException("Unsupported Spec:" + spec);
        }
    }
}
