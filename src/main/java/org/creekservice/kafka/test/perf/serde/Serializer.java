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

import org.creekservice.kafka.test.perf.model.TestModel;

/** A Kafka-like serializer interface */
public interface Serializer {

    /**
     * Serialize the model to bytes
     *
     * @param model the model to serialize
     * @param validate flag indicating if JSON should be validated against the schema. Setting this
     *     to {@code false} allows the serializer to generate invalid JSON, which allows testing of
     *     negative path in the deserializer.
     * @return the serialized bytes.
     */
    byte[] serialize(TestModel model, boolean validate);
}
