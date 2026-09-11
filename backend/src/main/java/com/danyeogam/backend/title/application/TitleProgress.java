package com.danyeogam.backend.title.application;

public record TitleProgress(
        long currentValue,
        long targetValue,
        TitleProgressUnit unit
) {
    public boolean achieved() {
        return currentValue >= targetValue;
    }
}
