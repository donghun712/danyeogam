package com.danyeogam.backend.stamp.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class GeoDistanceCalculatorTest {

    private final GeoDistanceCalculator calculator = new GeoDistanceCalculator();

    @Test
    void calculatesServerSideHaversineDistance() {
        assertThat(calculator.meters(
                new BigDecimal("37.0"), new BigDecimal("127.0"), 37.0, 127.0
        )).isEqualByComparingTo("0.00");
        assertThat(calculator.meters(
                new BigDecimal("37.001"), new BigDecimal("127.0"), 37.0, 127.0
        )).isBetween(new BigDecimal("111.00"), new BigDecimal("112.00"));
    }
}
