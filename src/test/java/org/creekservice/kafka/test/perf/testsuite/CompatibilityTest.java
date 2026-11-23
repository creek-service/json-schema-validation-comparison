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

package org.creekservice.kafka.test.perf.testsuite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class CompatibilityTest {

    @Test
    void shouldMatchAllSpecsWhenEmpty() {
        // Given:
        final Compatibility compat = Compatibility.parse("");

        // When / Then:
        for (final SchemaSpec spec : SchemaSpec.values()) {
            assertThat(spec.name(), compat.check(spec), is(true));
        }
    }

    @Test
    void shouldMatchAllSpecsWhenNull() {
        // Given:
        final Compatibility compat = Compatibility.parse(null);

        // When / Then:
        for (final SchemaSpec spec : SchemaSpec.values()) {
            assertThat(spec.name(), compat.check(spec), is(true));
        }
    }

    @Test
    void shouldMatchMinimumBound() {
        // Given:
        final Compatibility compat = Compatibility.parse("7");

        // When / Then:
        assertThat(compat.check(SchemaSpec.DRAFT_03), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_04), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_06), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_07), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_2019_09), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_2020_12), is(true));
    }

    @Test
    void shouldMatchMaximumBound() {
        // Given:
        final Compatibility compat = Compatibility.parse("<=7");

        // When / Then:
        assertThat(compat.check(SchemaSpec.DRAFT_03), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_04), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_06), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_07), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_2019_09), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_2020_12), is(false));
    }

    @Test
    void shouldMatchExactBound() {
        // Given:
        final Compatibility compat = Compatibility.parse("=3");

        // When / Then:
        assertThat(compat.check(SchemaSpec.DRAFT_03), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_04), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_06), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_07), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_2019_09), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_2020_12), is(false));
    }

    @Test
    void shouldMatchRange() {
        // Given:
        final Compatibility compat = Compatibility.parse("4,<=7");

        // When / Then:
        assertThat(compat.check(SchemaSpec.DRAFT_03), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_04), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_06), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_07), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_2019_09), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_2020_12), is(false));
    }

    @Test
    void shouldMatchDisjointRanges() {
        // Given:
        final Compatibility compat = Compatibility.parse("<=3,7");

        // When / Then:
        assertThat(compat.check(SchemaSpec.DRAFT_03), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_04), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_06), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_07), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_2019_09), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_2020_12), is(true));
    }

    @Test
    void shouldMatchExactPlusRange() {
        // Given:
        final Compatibility compat = Compatibility.parse("=3,7");

        // When / Then:
        assertThat(compat.check(SchemaSpec.DRAFT_03), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_04), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_06), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_07), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_2019_09), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_2020_12), is(true));
    }

    @Test
    void shouldMatchMultipleRangesWithGap() {
        // Given:
        final Compatibility compat = Compatibility.parse("4,<=6,9999");

        // When / Then:
        assertThat(compat.check(SchemaSpec.DRAFT_03), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_04), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_06), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_07), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_2019_09), is(false));
        assertThat(compat.check(SchemaSpec.DRAFT_2020_12), is(false));
    }

    @Test
    void shouldMatchFutureDraftToken() {
        // Given:
        final Compatibility compat = Compatibility.parse("9999");

        // When / Then:
        for (final SchemaSpec spec : SchemaSpec.values()) {
            assertThat(spec.name(), compat.check(spec), is(false));
        }
    }

    @Test
    void shouldMatchLargeRange() {
        // Given:
        final Compatibility compat = Compatibility.parse("3,<=2019");

        // When / Then:
        assertThat(compat.check(SchemaSpec.DRAFT_03), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_04), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_06), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_07), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_2019_09), is(true));
        assertThat(compat.check(SchemaSpec.DRAFT_2020_12), is(false));
    }
}
