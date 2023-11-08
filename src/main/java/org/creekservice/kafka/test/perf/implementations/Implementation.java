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

import static java.util.Objects.requireNonNull;

import java.util.Set;
import org.creekservice.kafka.test.perf.model.TestModel;
import org.creekservice.kafka.test.perf.testsuite.AdditionalSchemas;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;

public interface Implementation {

    interface JsonValidator {
        /**
         * Validate a JSON document.
         *
         * @param json the JSON to validate
         */
        void validate(String json);

        /**
         * Serialize the model to bytes
         *
         * @param model the model to serialize
         * @param validate flag indicating if JSON should be validated against the schema. Setting
         *     this to {@code false} allows the serializer to generate invalid JSON, which allows
         *     testing of negative path in the deserializer.
         * @return the serialized bytes.
         */
        byte[] serialize(TestModel model, boolean validate);

        /**
         * Deserialise a model from the supplied bytes.
         *
         * <p>Implementations should <i>always</i> validate the supplied JSON.
         *
         * @param data the bytes containing the JSON document to deserialize.
         * @return the deserialized object.
         */
        TestModel deserialize(byte[] data);
    }

    class MetaData {
        public final String longName;
        public final String shortName;
        public final Set<SchemaSpec> supported;

        /**
         * @param longName a more expressive name.
         * @param shortName the short name, as used in reports.
         * @param supported the set of supported JSON schema draft specifications.
         */
        public MetaData(
                final String longName, final String shortName, final Set<SchemaSpec> supported) {
            this.longName = requireNonNull(longName, "longName").trim();
            this.shortName = requireNonNull(shortName, "shortName").trim();
            this.supported = Set.copyOf(requireNonNull(supported, "supported"));

            if (longName.isBlank()) {
                throw new IllegalArgumentException("Long name blank");
            }

            if (shortName.isBlank()) {
                throw new IllegalArgumentException("Short name blank");
            }
        }

        // Final, empty finalize method stops spotbugs CT_CONSTRUCTOR_THROW
        // Can be moved to base type after https://github.com/spotbugs/spotbugs/issues/2665
        @Override
        @SuppressWarnings({"deprecation", "Finalize"})
        protected final void finalize() {}
    }

    /**
     * @return metadata about the implementation
     */
    MetaData metadata();

    /**
     * Test if impl supports the supplied {@code spec}.
     *
     * @param spec the spec to test.
     * @return {@code true} if the impl supports the supplied {@code spec}.
     */
    default boolean supports(final SchemaSpec spec) {
        return metadata().supported.contains(spec);
    }

    /**
     * Prepare a {@link JsonValidator} that is capable of validating JSON conforming to the supplied
     * {@code schema}.
     *
     * <p>Implementations should do as much work upfront as possible, e.g. parsing schemas and
     * building validators, as the time spent in this method does not currently form part of the
     * benchmarked code, i.e. time spent in this method is ignored in the benchmarking.
     *
     * @param schema the schema to validate with
     * @param spec the spec of the schema
     * @param additionalSchemas accessor to meta-schemas and JSON-Schema-Test-Suite 'remote'
     *     schemas.
     * @return a validator instance that be can be used to validate, serialise and deserialise JSON.
     */
    JsonValidator prepare(String schema, SchemaSpec spec, AdditionalSchemas additionalSchemas);
}
