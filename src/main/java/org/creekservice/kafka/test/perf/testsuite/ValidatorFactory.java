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

package org.creekservice.kafka.test.perf.testsuite;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public interface ValidatorFactory {

    /**
     * @return the set of supported specs.
     */
    Set<SchemaSpec> supports();

    /**
     * @param schema the schema to validate with
     * @param spec the spec of the schema
     * @param additionalSchemas accessor to meta-schemas and JSON-Schema-Test-Suite 'remote'
     *     schemas.
     * @throws RuntimeException on failure
     */
    JsonValidator prepare(String schema, SchemaSpec spec, AdditionalSchemas additionalSchemas);

    interface JsonValidator {
        /**
         * @param json the JSON to validate
         */
        void validate(String json);
    }

    /**
     * Interface for impls to use to load meta-schemas and JSON-Schema-Test-Suite 'remote' schemas,
     * <i>without</i> IO operations, (Which would mess with performance results).
     */
    interface AdditionalSchemas {

        /**
         * Load a remote schema.
         *
         * @param uri the schema id to load.
         * @return the schema content
         * @throws RuntimeException on unknown schema.
         */
        default String load(String uri) {
            return load(URI.create(uri));
        }

        /**
         * Load a remote schema.
         *
         * @param uri the schema id to load.
         * @return the schema content
         * @throws RuntimeException on unknown schema.
         */
        String load(URI uri);

        /**
         * @return content of JSON-Schema-Test-Suite 'remote' schemas
         */
        Map<URI, String> remotes();

        /**
         * @return location where remotes are being loaded from.
         */
        Path remotesDir();
    }
}
