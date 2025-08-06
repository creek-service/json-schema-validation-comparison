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

package org.creekservice.kafka.test.perf.util;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Table {

    @JsonProperty("headings")
    private final List<String> headings;

    @JsonProperty("rows")
    private final List<Row> rows = new ArrayList<>();

    public Table(final List<String> headings) {
        this.headings = List.copyOf(requireNonNull(headings, "headers"));
    }

    public Row addRow() {
        final Row row = new Row(headings);
        rows.add(row);
        return row;
    }

    public String toMarkdown() {
        final Map<String, Integer> widths = calcWidths();

        final String format =
                widths.values().stream()
                        .map(width -> "%-" + width + "s")
                        .collect(joining(" | ", "| ", " |" + System.lineSeparator()));

        final String div =
                widths.values().stream()
                        .map(width -> "-".repeat(Math.max(3, width + 2)))
                        .collect(joining("|", "|", "|" + System.lineSeparator()));

        final String columnHeaders = String.format(format, headings.toArray());

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
    private Map<String, Integer> calcWidths() {
        final Map<String, Integer> widths = new LinkedHashMap<>();

        headings.forEach(h -> widths.put(h, h.length()));

        rows.forEach(
                row ->
                        row.forEach(
                                (header, value) ->
                                        widths.compute(
                                                header,
                                                (ignored, existing) ->
                                                        Math.max(existing, width(value)))));

        return widths;
    }

    private static int width(final Object value) {
        final String text = value.toString();
        return Arrays.stream(text.split(System.lineSeparator()))
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    public void map(final Consumer<Row> c) {
        rows.forEach(c);
    }

    public String toJson() {
        try {
            final ObjectMapper mapper = JsonMapper.builder().build();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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

        @JsonValue
        private Collection<Object> jsonValues() {
            return values();
        }
    }
}
