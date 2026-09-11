package com.danyeogam.backend.sync.config;

import com.danyeogam.backend.sync.application.TourSyncCommand;
import com.danyeogam.backend.sync.application.TourSyncResult;
import com.danyeogam.backend.sync.application.TourSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

@Component
@Profile("!test")
@Order(0)
class TourSyncStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TourSyncStartupRunner.class);

    private final TourSyncProperties properties;
    private final TourSyncService syncService;

    TourSyncStartupRunner(TourSyncProperties properties, TourSyncService syncService) {
        this.properties = properties;
        this.syncService = syncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        TourSyncResult result = syncService.synchronize(new TourSyncCommand(
                properties.getAreaCode(),
                properties.getPageSize(),
                properties.getMaxPages(),
                properties.isHydrateDetails()
        ));
        log.info("Startup TourAPI sync finished: syncRunId={}, status={}", result.syncRunId(), result.status());
    }
}
