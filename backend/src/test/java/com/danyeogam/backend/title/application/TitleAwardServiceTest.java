package com.danyeogam.backend.title.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

class TitleAwardServiceTest {

    private TitleDefinitionRepository definitionRepository;
    private ActorTitleRepository actorTitleRepository;
    private TitleProgressCalculator progressCalculator;
    private TitleAwardService service;

    @BeforeEach
    void setUp() {
        definitionRepository = mock(TitleDefinitionRepository.class);
        actorTitleRepository = mock(ActorTitleRepository.class);
        progressCalculator = mock(TitleProgressCalculator.class);
        service = new TitleAwardService(
                definitionRepository, actorTitleRepository, progressCalculator
        );
    }

    @Test
    void awardsOnlyNewlyAchievedDefinitionsAndLinksThemToAttempt() {
        TitleDefinition alreadyOwned = definition(1L, "OWNED");
        TitleDefinition newlyAchieved = definition(2L, "NEW");
        TitleDefinition incomplete = definition(3L, "INCOMPLETE");
        List<TitleDefinition> definitions = List.of(alreadyOwned, newlyAchieved, incomplete);
        Instant now = Instant.parse("2026-09-11T00:00:00Z");
        when(definitionRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc())
                .thenReturn(definitions);
        when(actorTitleRepository.findTitleDefinitionIdsByActorId(42L)).thenReturn(List.of(1L));
        when(progressCalculator.calculate(42L, definitions)).thenReturn(Map.of(
                1L, new TitleProgress(1, 1, TitleProgressUnit.VISITS),
                2L, new TitleProgress(1, 1, TitleProgressUnit.VISITS),
                3L, new TitleProgress(0, 1, TitleProgressUnit.VISITS)
        ));
        when(actorTitleRepository.saveAllAndFlush(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Long> result = service.evaluateAndAward(42L, 99L, now);

        assertThat(result).containsExactly(2L);
        verify(actorTitleRepository).saveAllAndFlush(anyList());
    }

    @Test
    void returnsAwardsRecordedForIdempotentAttemptReplay() {
        when(actorTitleRepository.findTitleDefinitionIdsByActorIdAndVerificationAttemptId(42L, 99L))
                .thenReturn(List.of(2L, 7L));

        assertThat(service.findAwardedByAttempt(42L, 99L)).containsExactly(2L, 7L);
    }

    private static TitleDefinition definition(long id, String code) {
        TitleDefinition definition = TitleDefinition.visitCount(code, code, code, 1, (int) id);
        ReflectionTestUtils.setField(definition, "id", id);
        return definition;
    }
}
