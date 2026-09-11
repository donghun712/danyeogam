package com.danyeogam.backend.title.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.danyeogam.backend.title.api.TitleItemResponse;
import com.danyeogam.backend.title.api.TitleListResponse;
import com.danyeogam.backend.title.domain.ActorTitle;
import com.danyeogam.backend.title.domain.TitleDefinition;
import com.danyeogam.backend.title.repository.ActorTitleRepository;
import com.danyeogam.backend.title.repository.TitleDefinitionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class TitleQueryService {

    private final TitleDefinitionRepository definitionRepository;
    private final ActorTitleRepository actorTitleRepository;
    private final TitleProgressCalculator progressCalculator;

    public TitleQueryService(
            TitleDefinitionRepository definitionRepository,
            ActorTitleRepository actorTitleRepository,
            TitleProgressCalculator progressCalculator
    ) {
        this.definitionRepository = definitionRepository;
        this.actorTitleRepository = actorTitleRepository;
        this.progressCalculator = progressCalculator;
    }

    @Transactional(readOnly = true)
    public TitleListResponse getTitles(long actorId) {
        List<TitleDefinition> definitions =
                definitionRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc();
        Map<Long, Instant> awards = actorTitleRepository.findAllByActorId(actorId).stream()
                .collect(Collectors.toMap(
                        award -> award.getTitleDefinition().getId(),
                        ActorTitle::getAwardedAt
                ));
        Map<Long, TitleProgress> progress = progressCalculator.calculate(actorId, definitions);

        List<TitleItemResponse> titles = definitions.stream()
                .map(definition -> response(
                        definition,
                        awards.get(definition.getId()),
                        progress.get(definition.getId())
                ))
                .toList();
        return new TitleListResponse(titles);
    }

    private static TitleItemResponse response(
            TitleDefinition definition,
            Instant awardedAt,
            TitleProgress progress
    ) {
        return new TitleItemResponse(
                definition.getId(),
                definition.getCode(),
                definition.getName(),
                definition.getDescription(),
                awardedAt != null,
                awardedAt,
                progress.currentValue(),
                progress.targetValue(),
                progress.unit().name()
        );
    }
}
