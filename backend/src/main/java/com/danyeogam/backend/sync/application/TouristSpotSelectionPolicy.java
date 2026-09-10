package com.danyeogam.backend.sync.application;

import java.util.List;
import java.util.Objects;

import com.danyeogam.backend.tourapi.TouristSummary;

final class TouristSpotSelectionPolicy {

    private static final List<TourCategorySelection> SELECTIONS = List.of(
            new TourCategorySelection("12", "HS", "HS01", null),
            new TourCategorySelection("12", "HS", "HS02", null),
            new TourCategorySelection("12", "VE", "VE07", "VE070100"),
            new TourCategorySelection("14", "VE", "VE07", "VE070100"),
            new TourCategorySelection("12", "VE", "VE07", "VE070200"),
            new TourCategorySelection("14", "VE", "VE07", "VE070200"),
            new TourCategorySelection("12", "VE", "VE07", "VE070600"),
            new TourCategorySelection("14", "VE", "VE07", "VE070600")
    );

    private TouristSpotSelectionPolicy() {
    }

    static List<TourCategorySelection> selections() {
        return SELECTIONS;
    }

    static boolean includes(TouristSummary summary) {
        return SELECTIONS.stream().anyMatch(selection -> selection.matches(summary));
    }
}

record TourCategorySelection(
        String contentTypeId,
        String classificationLevel1,
        String classificationLevel2,
        String classificationLevel3
) {
    boolean matches(TouristSummary summary) {
        return Objects.equals(contentTypeId, summary.contentTypeId())
                && Objects.equals(classificationLevel1, summary.classificationLevel1())
                && Objects.equals(classificationLevel2, summary.classificationLevel2())
                && (classificationLevel3 == null
                        || Objects.equals(classificationLevel3, summary.classificationLevel3()));
    }
}
