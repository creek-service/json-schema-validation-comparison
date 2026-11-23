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

import java.util.ArrayList;
import java.util.List;

public final class Compatibility {

    private static final Compatibility ALL = new Compatibility(List.of());

    private final List<Range> ranges;

    private Compatibility(final List<Range> ranges) {
        this.ranges = List.copyOf(ranges);
    }

    public static Compatibility parse(final String compatibility) {
        if (compatibility == null || compatibility.isBlank()) {
            return ALL;
        }

        final String[] tokens = compatibility.split(",");
        final List<Range> ranges = new ArrayList<>();
        Range current = new Range();

        try {
            for (final String token : tokens) {
                final String trimmed = token.trim();
                if (trimmed.startsWith("<=")) {
                    current.max = Integer.parseInt(trimmed.substring(2));
                } else if (trimmed.startsWith("=")) {
                    final int value = Integer.parseInt(trimmed.substring(1));
                    if (current.hasConstraint()) {
                        ranges.add(current);
                        current = new Range();
                    }
                    current.min = value;
                    current.max = value;
                } else {
                    final int value = Integer.parseInt(trimmed);
                    if (current.hasConstraint()) {
                        ranges.add(current);
                        current = new Range();
                    }
                    current.min = value;
                }
            }
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid compatibility string: '" + compatibility + "'", e);
        }
        ranges.add(current);

        return new Compatibility(ranges);
    }

    public boolean check(final SchemaSpec spec) {
        if (ranges.isEmpty()) {
            return true;
        }
        final int token = spec.compatibilityToken();
        for (final Range range : ranges) {
            if (range.matches(token)) {
                return true;
            }
        }
        return false;
    }

    private static final class Range {
        private int min = Integer.MIN_VALUE;
        private int max = Integer.MAX_VALUE;

        boolean hasConstraint() {
            return min != Integer.MIN_VALUE || max != Integer.MAX_VALUE;
        }

        boolean matches(final int token) {
            return token >= min && token <= max;
        }
    }
}
