package com.mandi.service;

import com.mandi.client.DataGovMandiClient;
import com.mandi.client.DataGovPage;
import com.mandi.config.MandiProperties;
import com.mandi.dto.IngestResult;
import com.mandi.dto.SyncSummary;
import com.mandi.exception.ExternalApiException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Pulls the whole dataset state-by-state (paged), validates, upserts into the history table. */
@Service
public class HistorySyncService {
    private static final Logger log = LoggerFactory.getLogger(HistorySyncService.class);
    private final DataGovMandiClient client;
    private final HistoryIngestService ingest;
    private final AlertService alerts;
    private final MandiProperties props;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public HistorySyncService(DataGovMandiClient client, HistoryIngestService ingest,
                              AlertService alerts, MandiProperties props) {
        this.client = client; this.ingest = ingest; this.alerts = alerts; this.props = props;
    }

    public boolean isRunning() { return running.get(); }

    public boolean startAsync(Collection<String> states) {
        if (running.get()) return false;
        CompletableFuture.runAsync(() -> syncAll(states));
        return true;
    }

    public SyncSummary syncAll(Collection<String> statesOverride) {
        if (!running.compareAndSet(false, true)) {
            log.warn("Mandi sync already running; skipping");
            return null;
        }
        try {
            List<String> states = (statesOverride != null && !statesOverride.isEmpty()) ? List.copyOf(statesOverride)
                    : (props.sync().states() != null && !props.sync().states().isEmpty()) ? props.sync().states()
                    : props.states();
            log.info("Mandi sync started for {} state(s)", states.size());
            long received = 0, invalid = 0, ins = 0, upd = 0, skip = 0;
            int done = 0;
            List<String> failures = new ArrayList<>();
            int pageSize = props.api().pageSize();
            for (String state : states) {
                try {
                    int offset = 0;
                    for (int page = 0; page < props.sync().maxPagesPerState(); page++) {
                        DataGovPage p = client.fetch(Map.of("state", state), pageSize, offset);
                        received += p.rawCount(); invalid += p.invalidCount();
                        IngestResult r = ingest.upsert(p.records());
                        ins += r.inserted(); upd += r.updated(); skip += r.skipped();
                        offset += pageSize;
                        if (p.rawCount() == 0 || offset >= p.total()) break;
                    }
                    done++;
                } catch (ExternalApiException e) {
                    log.error("Sync failed for state {}: {}", state, e.getMessage());
                    failures.add(state);
                }
            }
            try { alerts.evaluateAll(); } catch (Exception e) { log.warn("Alert evaluation failed: {}", e.getMessage()); }
            SyncSummary s = new SyncSummary(done, received, invalid, ins, upd, skip, failures);
            log.info("Mandi sync finished: {}", s);
            return s;
        } finally {
            running.set(false);
        }
    }
}
