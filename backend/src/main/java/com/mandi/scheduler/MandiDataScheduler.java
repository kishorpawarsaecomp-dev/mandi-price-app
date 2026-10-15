package com.mandi.scheduler;

import com.mandi.config.MandiProperties;
import com.mandi.service.HistorySyncService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MandiDataScheduler {
    private final HistorySyncService sync;
    private final MandiProperties props;

    public MandiDataScheduler(HistorySyncService sync, MandiProperties props) {
        this.sync = sync;
        this.props = props;
    }

    /** Schedule comes from MANDI_SYNC_CRON / MANDI_SYNC_ZONE (see application.yml). */
    @Scheduled(cron = "${mandi.sync.cron}", zone = "${mandi.sync.zone}")
    public void daily() { sync.syncAll(null); }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (props.sync().runOnStartup()) sync.startAsync(null);
    }
}
