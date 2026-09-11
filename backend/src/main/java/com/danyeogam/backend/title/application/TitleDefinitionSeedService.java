package com.danyeogam.backend.title.application;

import com.danyeogam.backend.title.repository.TitleDefinitionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!test")
public class TitleDefinitionSeedService {

    private final TitleDefinitionRepository definitionRepository;

    public TitleDefinitionSeedService(TitleDefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
    }

    @Transactional
    public void synchronizeRegionTitles() {
        definitionRepository.alignRegionTitleActivity();
        definitionRepository.upsertActiveRegionTitles();
    }
}
