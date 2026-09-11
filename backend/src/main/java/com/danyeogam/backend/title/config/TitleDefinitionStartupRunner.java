package com.danyeogam.backend.title.config;

import com.danyeogam.backend.title.application.TitleDefinitionSeedService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Order(100)
public class TitleDefinitionStartupRunner implements ApplicationRunner {

    private final TitleDefinitionSeedService seedService;

    public TitleDefinitionStartupRunner(TitleDefinitionSeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedService.synchronizeRegionTitles();
    }
}
