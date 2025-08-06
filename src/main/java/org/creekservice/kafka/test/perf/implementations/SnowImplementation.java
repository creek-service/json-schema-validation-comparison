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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.gson.JsonElement;
import com.qindesign.json.schema.Error;
import com.qindesign.json.schema.JSON;
import com.qindesign.json.schema.JSONPath;
import com.qindesign.json.schema.MalformedSchemaException;
import com.qindesign.json.schema.Option;
import com.qindesign.json.schema.Options;
import com.qindesign.json.schema.Specification;
import com.qindesign.json.schema.Validator;
import com.qindesign.json.schema.net.URI;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

@SuppressWarnings("FieldMayBeFinal") // not final to avoid folding.
public class SnowImplementation implements Implementation {

    private static final Map<SchemaSpec, Specification> SUPPORTED =
            Map.of(
                    SchemaSpec.DRAFT_06, Specification.DRAFT_06,
                    SchemaSpec.DRAFT_07, Specification.DRAFT_07,
                    SchemaSpec.DRAFT_2019_09, Specification.DRAFT_2019_09);

    private static final MetaData METADATA =
            new MetaData(
                    "Snow",
                    "Snow",
                    Language.Java,
                    Licence.GNU_Affero_General_Public_v3_0,
                    SUPPORTED.keySet(),
                    "https://github.com/ssilverman/snowy-json",
                    new Color(75, 192, 192),
                    com.qindesign.json.schema.Validator.class,
                    MetaData.ACTIVE_PROJECT);

    private ObjectMapper mapper = JsonMapper.builder().build();

    @Override
    public MetaData metadata() {
        return METADATA;
    }

    @Override
    public JsonValidator prepare(
            final String schema,
            final SchemaSpec spec,
            final AdditionalSchemas additionalSchemas,
            final boolean enableFormatAssertions) {
        final Validator validator =
                createValidator(
                        schema,
                        spec,
                        Optional.of(additionalSchemas.remotesDir()),
                        enableFormatAssertions);

        return new JsonValidator() {
            @Override
            public void validate(final String json) {
                try {
                    final JsonElement toValidate = parse(json.getBytes(UTF_8));
                    final Map<JSONPath, Map<JSONPath, Error<?>>> errors = new HashMap<>();
                    if (!validator.validate(toValidate, new HashMap<>(), errors)) {
                        throw new RuntimeException(errors.toString());
                    }
                } catch (MalformedSchemaException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public byte[] serialize(final TestModel model, final boolean validate) {
                try {
                    final byte[] bytes = mapper.writeValueAsBytes(model);

                    if (validate) {
                        validate(bytes);
                    }

                    return bytes;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TestModel deserialize(final byte[] data) {
                try {
                    validate(data);

                    return mapper.readValue(data, TestModel.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            private void validate(final byte[] bytes) throws MalformedSchemaException {
                final JsonElement json = parse(bytes);

                final Map<JSONPath, Map<JSONPath, Error<?>>> errors = new HashMap<>();
                if (!validator.validate(json, new HashMap<>(), errors)) {
                    throw new RuntimeException(errors.toString());
                }
            }
        };
    }

    private static JsonElement parse(final byte[] bytes) {
        return JSON.parse(new ByteArrayInputStream(bytes));
    }

    @SuppressWarnings({"CollectionContainsUrl", "OptionalUsedAsFieldOrParameterType"})
    private Validator createValidator(
            final String schema,
            final SchemaSpec spec,
            final Optional<Path> remotesDir,
            final boolean enableFormatAssertions) {
        try {
            final Map<URI, URL> knownURLs =
                    remotesDir.isPresent()
                            ? Map.of(
                                    URI.parseUnchecked("http://localhost:1234"),
                                    remotesDir.get().toUri().toURL())
                            : Map.of();

            final Options opts = new Options();
            opts.set(Option.FORMAT, enableFormatAssertions);
            opts.set(Option.CONTENT, true);
            opts.set(Option.DEFAULT_SPECIFICATION, schemaVersion(spec));

            return new Validator(
                    parse(schema.getBytes(UTF_8)),
                    URI.parseUnchecked("https://something.com/"),
                    Map.of(),
                    knownURLs,
                    opts);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Specification schemaVersion(final SchemaSpec spec) {
        final Specification ver = SUPPORTED.get(spec);
        if (ver == null) {
            throw new IllegalArgumentException("Unsupported: " + spec);
        }
        return ver;
    }
}
