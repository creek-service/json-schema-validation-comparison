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

package org.creekservice.kafka.test.perf.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.util.List;
import java.util.Set;
import org.creekservice.kafka.test.perf.implementations.Implementation;
import org.creekservice.kafka.test.perf.testsuite.SchemaSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImplsJsonFormatterTest {

    private static final Implementation.MetaData MD_A =
            new Implementation.MetaData(
                    "Implementation A",
                    "ImplA",
                    Implementation.Language.Java,
                    Implementation.Licence.Apache_v2_0,
                    Set.of(SchemaSpec.DRAFT_2019_09, SchemaSpec.DRAFT_04),
                    "http://a",
                    Color.BLACK,
                    Test.class,
                    Implementation.MetaData.ACTIVE_PROJECT);

    private static final Implementation.MetaData MD_B =
            new Implementation.MetaData(
                    "Implementation B",
                    "ImplB",
                    Implementation.Language.Java,
                    Implementation.Licence.Apache_v2_0,
                    Set.of(SchemaSpec.DRAFT_07),
                    "http://b",
                    Color.BLUE,
                    Test.class,
                    "No release since dot");

    @Mock private Implementation implA;

    @Mock private Implementation implB;

    @BeforeEach
    void setUp() {
        when(implA.metadata()).thenReturn(MD_A);
        when(implB.metadata()).thenReturn(MD_B);
    }

    @Test
    void shouldFormatAsJson() {
        // Given:

        // When:
        final String json = ImplsJsonFormatter.implDetailsAsJson(List.of(implA, implB));

        // Then:
        assertThat(
                json,
                is(
                        "[{\"longName\":\"Implementation A\","
                                + "\"shortName\":\"ImplA\","
                                + "\"language\":\"Java\","
                                + "\"licence\":\"Apache Licence 2.0\","
                                + "\"supported\":[\"DRAFT_04\","
                                + "\"DRAFT_2019_09\"],"
                                + "\"url\":\"http://a\","
                                + "\"color\":\"rgb(0,0,0)\","
                                + "\"jarSize\":210954},"
                                + "{\"longName\":\"Implementation B\","
                                + "\"shortName\":\"ImplB\","
                                + "\"language\":\"Java\","
                                + "\"licence\":\"Apache Licence 2.0\","
                                + "\"supported\":[\"DRAFT_07\"],"
                                + "\"url\":\"http://b\","
                                + "\"color\":\"rgb(0,0,255)\","
                                + "\"jarSize\":210954,"
                                + "\"inactive\":\"No release since dot\"}]"));
    }
}
