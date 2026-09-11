package com.danyeogam.backend.title.application;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.danyeogam.backend.title.domain.ActorTitle;
import com.danyeogam.backend.title.domain.TitleDefinition;
import com.danyeogam.backend.title.repository.ActorTitleRepository;
import com.danyeogam.backend.title.repository.TitleDefinitionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class TitleAwardService {

    private final TitleDefinitionRepository definitionRepository;
    private final ActorTitleRepository actorTitleRepository;
    private final TitleProgressCalculator progressCalculator;

    public TitleAwardService(
            TitleDefinitionRepository definitionRepository,
            ActorTitleRepository actorTitleRepository,
            TitleProgressCalculator progressCalculator
    ) {
        this.definitionRepository = definitionRepository;
        this.actorTitleRepository = actorTitleRepository;
        this.progressCalculator = progressCalculator;
    }

    @Transactional
    public List<Long> evaluateAndAward(
            long actorId,
            long verificationAttemptId,
            Instant awardedAt
    ) {
        List<TitleDefinition> definitions =
                definitionRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc();
        Set<Long> ownedDefinitionIds = new HashSet<>(
                actorTitleRepository.findTitleDefinitionIdsByActorId(actorId)
        );
        Map<Long, TitleProgress> progressByDefinition =
                progressCalculator.calculate(actorId, definitions);

        List<ActorTitle> newAwards = definitions.stream()
                .filter(definition -> !ownedDefinitionIds.contains(definition.getId()))
                .filter(definition -> progressByDefinition.get(definition.getId()).achieved())
                .map(definition -> new ActorTitle(
                        actorId, definition, verificationAttemptId, awardedAt
                ))
                .toList();
        if (newAwards.isEmpty()) {
            return List.of();
        }

        actorTitleRepository.saveAllAndFlush(newAwards);
        return newAwards.stream()
                .map(award -> award.getTitleDefinition().getId())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> findAwardedByAttempt(long actorId, long verificationAttemptId) {
        return actorTitleRepository.findTitleDefinitionIdsByActorIdAndVerificationAttemptId(
                actorId, verificationAttemptId
        );
    }
}
