package com.danyeogam.backend.title.application;

public record TitleProgress(
        long currentValue,
        long targetValue,
        TitleProgressUnit unit,
        Long currentCount,
        Long targetCount
) {
    public TitleProgress(long currentValue, long targetValue, TitleProgressUnit unit) {
        this(currentValue, targetValue, unit, null, null);
    }

    public boolean achieved() {
        return currentValue >= targetValue;
    }
}
