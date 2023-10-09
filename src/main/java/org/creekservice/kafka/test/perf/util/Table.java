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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Table {

    private final List<String> headers;
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, Integer> widths = new LinkedHashMap<>();

    public Table(final List<String> headers) {
        this.headers = List.copyOf(requireNonNull(headers, "headers"));
    }

    public List<String> headers() {
        return List.copyOf(headers);
    }

    public Row addRow() {
        final Row row = new Row(headers);
        rows.add(row);
        widths.clear();
        return row;
    }

    @Override
    public String toString() {
        ensureWidths();

        final String format =
                widths.values().stream()
                        .map(width -> "%-" + width + "s")
                        .collect(joining(" | ", "| ", " |" + System.lineSeparator()));

        final String div =
                widths.values().stream()
                        .map(width -> "-".repeat(Math.max(3, width + 2)))
                        .collect(joining("|", "|", "|" + System.lineSeparator()));

        final String columnHeaders = String.format(format, headers.toArray());

        final String formattedRows =
                rows.stream().map(row -> formattedRows(format, row)).collect(joining());

        return columnHeaders + div + formattedRows;
    }

    private static String formattedRows(final String format, final Row row) {
        final List<Iterator<String>> its =
                row.values().stream()
                        .map(Object::toString)
                        .map(s -> Arrays.asList(s.split(System.lineSeparator())).iterator())
                        .collect(Collectors.toList());

        final StringBuilder all = new StringBuilder();
        while (its.stream().anyMatch(Iterator::hasNext)) {

            final Object[] values =
                    its.stream().map(it -> it.hasNext() ? it.next() : "").toArray(String[]::new);

            final String line = String.format(format, values);
            all.append(line);
        }

        return all.toString();
    }

    @SuppressWarnings("DataFlowIssue")
    private void ensureWidths() {
        if (!widths.isEmpty()) {
            return;
        }

        headers.forEach(h -> widths.put(h, h.length()));

        rows.forEach(
                row ->
                        row.forEach(
                                (header, value) ->
                                        widths.compute(
                                                header,
                                                (ignored, existing) ->
                                                        Math.max(existing, width(value)))));
    }

    private static int width(final Object value) {
        final String text = value.toString();
        return Arrays.stream(text.split(System.lineSeparator()))
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    public void removeIf(final Predicate<Row> p) {
        rows.removeIf(p);
        widths.clear();
    }

    public void sort(final Comparator<? super Row> c) {
        rows.sort(c);
    }

    public void map(final Consumer<Row> c) {
        rows.forEach(c);
        widths.clear();
    }

    public static final class Row {

        private final List<String> headers;
        private final Map<String, Object> values = new LinkedHashMap<>();

        private Row(final List<String> headers) {
            this.headers = List.copyOf(headers);
            headers.forEach(header -> values.put(header, ""));
        }

        public void put(final String header, final Object value) {
            validateHeader(header);
            values.put(header, requireNonNull(value, "value"));
        }

        public void compute(
                final String header, final BiFunction<? super String, ? super Object, ?> updater) {
            values.compute(
                    header,
                    (h, existing) -> {
                        if (existing == null) {
                            validateHeader(h);
                        }
                        return requireNonNull(updater.apply(h, existing), "updater returned null");
                    });
        }

        public Collection<Object> values() {
            return values.values();
        }

        public void forEach(final BiConsumer<? super String, ? super Object> consumer) {
            values.forEach(consumer);
        }

        public Object get(final String header) {
            final Object v = values.get(header);
            if (v == null) {
                throw new IllegalArgumentException("Unknown header: " + header);
            }
            return v;
        }

        private void validateHeader(final String header) {
            if (!headers.contains(header)) {
                throw new IllegalArgumentException("Not a valid header: " + header);
            }
        }
    }
}
