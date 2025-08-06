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

import java.util.List;

/**
 * God-class referencing all the implementations under test.
 *
 * <p>New implementations need registering with this class.
 */
public final class Implementations {

    private static final List<Implementation> IMPLS =
            List.of(
                    new JacksonImplementation(),
                    new EveritImplementation(),
                    new JustifyImplementation(),
                    new MedeiaImplementation(),
                    new NetworkNtImplementation(),
                    new SchemaFriendImplementation(),
                    new SkemaImplementation(),
                    new SnowImplementation(),
                    new VertxImplementation(),
                    new DevHarrelImplementation());

    public static List<Implementation> all() {
        return IMPLS;
    }

    private Implementations() {}
}
