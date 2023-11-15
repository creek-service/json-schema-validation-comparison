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

package org.creekservice.kafka.test.perf.performance.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;

import java.math.BigDecimal;
import org.creekservice.kafka.test.perf.performance.util.model.Metric;
import org.creekservice.kafka.test.perf.performance.util.model.PerformanceResult;
import org.junit.jupiter.api.Test;

class PerformanceJsonReaderTest {

    private static final String JSON_RESULT =
            "[\n"
                    +
                    // One complete one:
                    "    {\n"
                  + "        \"jmhVersion\" : \"1.36\",\n"
                  + "        \"benchmark\" :"
                  + " \"org.creekservice.kafka.test.perf.performance.JsonValidateBenchmark.measureDraft_4_Medeia\",\n"
                  + "        \"mode\" : \"avgt\",\n"
                  + "        \"threads\" : 4,\n"
                  + "        \"forks\" : 4,\n"
                  + "        \"jvm\" :"
                  + " \"/opt/homebrew/Cellar/openjdk@17/17.0.9/libexec/openjdk.jdk/Contents/Home/bin/java\",\n"
                  + "        \"jvmArgs\" : [\n"
                  + "            \"-Dfile.encoding=UTF-8\",\n"
                  + "            \"-Duser.country=GB\",\n"
                  + "            \"-Duser.language=en\",\n"
                  + "            \"-Duser.variant\"\n"
                  + "        ],\n"
                  + "        \"jdkVersion\" : \"17.0.9\",\n"
                  + "        \"vmName\" : \"OpenJDK 64-Bit Server VM\",\n"
                  + "        \"vmVersion\" : \"17.0.9+0\",\n"
                  + "        \"warmupIterations\" : 5,\n"
                  + "        \"warmupTime\" : \"10 s\",\n"
                  + "        \"warmupBatchSize\" : 1,\n"
                  + "        \"measurementIterations\" : 5,\n"
                  + "        \"measurementTime\" : \"10 s\",\n"
                  + "        \"measurementBatchSize\" : 1,\n"
                  + "        \"primaryMetric\" : {\n"
                  + "            \"score\" : 0.34276444437738995,\n"
                  + "            \"scoreError\" : 0.0038394222791281593,\n"
                  + "            \"scoreConfidence\" : [\n"
                  + "                0.3389250220982618,\n"
                  + "                0.3466038666565181\n"
                  + "            ],\n"
                  + "            \"scorePercentiles\" : {\n"
                  + "                \"0.0\" : 0.3341750081828707,\n"
                  + "                \"50.0\" : 0.3432545406943275,\n"
                  + "                \"90.0\" : 0.3482507649509782,\n"
                  + "                \"95.0\" : 0.34973757670327116,\n"
                  + "                \"99.0\" : 0.34981339737699124,\n"
                  + "                \"99.9\" : 0.34981339737699124,\n"
                  + "                \"99.99\" : 0.34981339737699124,\n"
                  + "                \"99.999\" : 0.34981339737699124,\n"
                  + "                \"99.9999\" : 0.34981339737699124,\n"
                  + "                \"100.0\" : 0.34981339737699124\n"
                  + "            },\n"
                  + "            \"scoreUnit\" : \"ms/op\",\n"
                  + "            \"rawData\" : [\n"
                  + "                [\n"
                  + "                    0.3465202925610167,\n"
                  + "                    0.34249890961701457,\n"
                  + "                    0.34450357678071153,\n"
                  + "                    0.34075528555055234,\n"
                  + "                    0.3425288988276069\n"
                  + "                ],\n"
                  + "                [\n"
                  + "                    0.34829698390259,\n"
                  + "                    0.34396076146988674,\n"
                  + "                    0.3433999185464469,\n"
                  + "                    0.34549801455831286,\n"
                  + "                    0.34390681847990284\n"
                  + "                ],\n"
                  + "                [\n"
                  + "                    0.3430043540612691,\n"
                  + "                    0.33511882592058606,\n"
                  + "                    0.33611264096826354,\n"
                  + "                    0.3341750081828707,\n"
                  + "                    0.33599361359531954\n"
                  + "                ],\n"
                  + "                [\n"
                  + "                    0.3478347943864718,\n"
                  + "                    0.34221756760803823,\n"
                  + "                    0.3460400623117395,\n"
                  + "                    0.34981339737699124,\n"
                  + "                    0.343109162842208\n"
                  + "                ]\n"
                  + "            ]\n"
                  + "        }\n"
                  + "    },\n"
                    // One minimal:
                    + "   {\n"
                    + "        \"benchmark\" :"
                    + " \"org.creekservice.kafka.test.perf.performance.JsonValidateBenchmark.measureDraft_7_Medeia\",\n"
                    + "        \"mode\" : \"diff\",\n"
                    + "        \"primaryMetric\" : {\n"
                    + "            \"score\" : 0.893598359837538,\n"
                    + "            \"scoreError\" : 0.0035983789573,\n"
                    + "            \"scoreUnit\" : \"ms/op\"\n"
                    + "        },\n"
                    + "        \"secondaryMetrics\" : {\n"
                    + "        }\n"
                    + "    }\n"
                    + "]";

    private static final String JSON_RESULT_WITH_NAN =
            "[\n"
                + "   {\n"
                + "        \"benchmark\" :"
                + " \"org.creekservice.kafka.test.perf.performance.JsonValidateBenchmark.measureDraft_7_Medeia\",\n"
                + "        \"mode\" : \"avgt\",\n"
                + "        \"primaryMetric\" : {\n"
                + "            \"score\" : 0.893598359837538,\n"
                + "            \"scoreError\" : \"NaN\",\n"
                + "            \"scoreUnit\" : \"ms/op\"\n"
                + "        },\n"
                + "        \"secondaryMetrics\" : {\n"
                + "        }\n"
                + "    }\n"
                + "]";

    @Test
    void shouldParseJson() {
        // When:
        final PerformanceResult[] results = PerformanceJsonReader.parseJson(JSON_RESULT);

        // Then:
        assertThat(
                results,
                arrayContaining(
                        new PerformanceResult(
                                "org.creekservice.kafka.test.perf.performance.JsonValidateBenchmark.measureDraft_4_Medeia",
                                "avgt",
                                new Metric(
                                        new BigDecimal("0.34276444437738995"),
                                        new BigDecimal("0.0038394222791281593"),
                                        "ms/op")),
                        new PerformanceResult(
                                "org.creekservice.kafka.test.perf.performance.JsonValidateBenchmark.measureDraft_7_Medeia",
                                "diff",
                                new Metric(
                                        new BigDecimal("0.893598359837538"),
                                        new BigDecimal("0.0035983789573"),
                                        "ms/op"))));
    }

    @Test
    void shouldHandleNaN() {
        // When:
        final PerformanceResult[] results = PerformanceJsonReader.parseJson(JSON_RESULT_WITH_NAN);

        // Then:
        assertThat(
                results,
                arrayContaining(
                        new PerformanceResult(
                                "org.creekservice.kafka.test.perf.performance.JsonValidateBenchmark.measureDraft_7_Medeia",
                                "avgt",
                                new Metric(new BigDecimal("0.893598359837538"), "NaN", "ms/op"))));
    }
}
