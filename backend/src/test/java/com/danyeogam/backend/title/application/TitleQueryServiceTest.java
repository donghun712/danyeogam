package com.danyeogam.backend.title.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.danyeogam.backend.title.domain.ActorTitle;
import com.danyeogam.backend.title.domain.TitleDefinition;
import com.danyeogam.backend.title.repository.ActorTitleRepository;
import com.danyeogam.backend.title.repository.TitleDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TitleQueryServiceTest {

    private TitleDefinitionRepository definitionRepository;
    private ActorTitleRepository actorTitleRepository;
    private TitleProgressCalculator progressCalculator;
    private TitleQueryService service;

    @BeforeEach
    void setUp() {
        definitionRepository = mock(TitleDefinitionRepository.class);
        actorTitleRepository = mock(ActorTitleRepository.class);
        progressCalculator = mock(TitleProgressCalculator.class);
        service = new TitleQueryService(
                definitionRepository, actorTitleRepository, progressCalculator
        );
    }

    @Test
    void returnsEveryActiveDefinitionWithOwnershipAndProgress() {
        TitleDefinition first = definition(1L, "FIRST");
        TitleDefinition second = definition(2L, "SECOND");
        Instant awardedAt = Instant.parse("2026-09-11T00:00:00Z");
        ActorTitle award = new ActorTitle(42L, first, 90L, awardedAt);
        when(definitionRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc())
                .thenReturn(List.of(first, second));
        when(actorTitleRepository.findAllByActorId(42L)).thenReturn(List.of(award));
        when(progressCalculator.calculate(42L, List.of(first, second))).thenReturn(Map.of(
                1L, new TitleProgress(1, 1, TitleProgressUnit.VISITS),
                2L, new TitleProgress(3, 10, TitleProgressUnit.VISITS)
        ));

        var result = service.getTitles(42L);

        assertThat(result.titles()).hasSize(2);
        assertThat(result.titles().get(0).earned()).isTrue();
        assertThat(result.titles().get(0).awardedAt()).isEqualTo(awardedAt);
        assertThat(result.titles().get(1).earned()).isFalse();
        assertThat(result.titles().get(1).currentValue()).isEqualTo(3);
        assertThat(result.titles().get(1).targetValue()).isEqualTo(10);
    }

    private static TitleDefinition definition(long id, String code) {
        TitleDefinition definition = TitleDefinition.visitCount(code, code, code, (int) id, (int) id);
        ReflectionTestUtils.setField(definition, "id", id);
        return definition;
    }
}
