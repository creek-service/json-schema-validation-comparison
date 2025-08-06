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

package org.creekservice.kafka.test.perf.performance.util.model;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class Metric {

    private final BigDecimal score;
    private final Optional<BigDecimal> scoreError;
    private final String scoreUnit;

    @JsonCreator
    public Metric(
            @JsonProperty(value = "score", required = true) final BigDecimal score,
            @JsonProperty(value = "scoreError", required = true) final Object scoreError,
            @JsonProperty(value = "scoreUnit", required = true) final String scoreUnit) {
        this.score = requireNonNull(score, "score");
        this.scoreError = optionalDecimal(requireNonNull(scoreError, "scoreError"));
        this.scoreUnit = requireNonNull(scoreUnit, "scoreUnit");
    }

    public BigDecimal score() {
        return score;
    }

    public Optional<BigDecimal> scoreError() {
        return scoreError;
    }

    public String scoreUnit() {
        return scoreUnit;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Metric metric = (Metric) o;
        return Objects.equals(score, metric.score)
                && Objects.equals(scoreError, metric.scoreError)
                && Objects.equals(scoreUnit, metric.scoreUnit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(score, scoreError, scoreUnit);
    }

    private static Optional<BigDecimal> optionalDecimal(final Object decimal) {
        return BigDecimal.class.isAssignableFrom(decimal.getClass())
                ? Optional.of((BigDecimal) decimal)
                : Optional.empty(); // Handles "NaN" case.
    }
}
