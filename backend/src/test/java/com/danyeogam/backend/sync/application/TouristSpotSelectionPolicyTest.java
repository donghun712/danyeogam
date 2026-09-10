package com.danyeogam.backend.sync.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.danyeogam.backend.tourapi.TouristSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TouristSpotSelectionPolicyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void includesOnlyApprovedHistoryAndCultureClassifications() {
        assertThat(included("12", "HS", "HS01", "HS010100")).isTrue();
        assertThat(included("12", "HS", "HS02", "HS020100")).isTrue();
        assertThat(included("12", "VE", "VE07", "VE070100")).isTrue();
        assertThat(included("14", "VE", "VE07", "VE070200")).isTrue();
        assertThat(included("14", "VE", "VE07", "VE070600")).isTrue();

        assertThat(included("12", "HS", "HS03", "HS030100")).isFalse();
        assertThat(included("12", "HS", "HS04", "HS040100")).isFalse();
        assertThat(included("12", "VE", "VE01", "VE010100")).isFalse();
        assertThat(included("14", "VE", "VE07", "VE070300")).isFalse();
        assertThat(included("14", "VE", "VE09", "VE090100")).isFalse();
    }

    private boolean included(String type, String level1, String level2, String level3) {
        return TouristSpotSelectionPolicy.includes(new TouristSummary(
                "1", type, "관광지", "주소", null, "11", "110",
                "127", "37", null, null, null, null, null,
                level1, level2, level3,
                objectMapper.createObjectNode()
        ));
    }
}
