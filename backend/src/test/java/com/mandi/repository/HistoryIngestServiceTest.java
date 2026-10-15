package com.mandi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mandi.dto.IngestResult;
import com.mandi.dto.MandiPriceRecord;
import com.mandi.service.HistoryIngestService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class HistoryIngestServiceTest {
    @Autowired MandiPriceRepository repo;

    static MandiPriceRecord rec(String market, LocalDate d, int modal) {
        return new MandiPriceRecord("Maharashtra", "Latur", market, "Tur", "Other", "FAQ", d,
                BigDecimal.valueOf(modal - 100), BigDecimal.valueOf(modal + 100), BigDecimal.valueOf(modal));
    }

    @Test
    void duplicatesAreSkippedAndCorrectionsUpdateInPlace() {
        HistoryIngestService ingest = new HistoryIngestService(repo);
        LocalDate d = LocalDate.of(2026, 10, 15);

        IngestResult first = ingest.upsert(List.of(rec("A", d, 9800), rec("B", d, 9700)));
        assertThat(first.inserted()).isEqualTo(2);

        IngestResult again = ingest.upsert(List.of(rec("A", d, 9800), rec("B", d, 9700)));
        assertThat(again.inserted()).isZero();
        assertThat(again.skipped()).isEqualTo(2);
        assertThat(repo.count()).isEqualTo(2);                      // no duplicate rows

        IngestResult corrected = ingest.upsert(List.of(rec("A", d, 9900)));
        assertThat(corrected.updated()).isEqualTo(1);
        assertThat(repo.count()).isEqualTo(2);                      // updated, not inserted
        assertThat(repo.findAll().stream().filter(p -> p.getMarket().equals("A")).findFirst().orElseThrow()
                .getModalPrice()).isEqualByComparingTo("9900");
    }

    @Test
    void duplicateInsideOneBatchIsCollapsed() {
        HistoryIngestService ingest = new HistoryIngestService(repo);
        LocalDate d = LocalDate.of(2026, 10, 15);
        IngestResult r = ingest.upsert(List.of(rec("A", d, 9800), rec("A", d, 9800)));
        assertThat(r.inserted()).isEqualTo(1);
        assertThat(r.skipped()).isEqualTo(1);
    }

    @Test
    void dateRangeFilteringReturnsOnlyRealRowsInRange() {
        HistoryIngestService ingest = new HistoryIngestService(repo);
        for (int day : new int[]{1, 2, 4, 5}) ingest.upsert(List.of(rec("A", LocalDate.of(2026, 10, day), 9000 + day)));
        var rows = repo.findSeries("Maharashtra", "Latur", "Tur", "Other", "FAQ",
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 5), List.of("A"));
        assertThat(rows).extracting(p -> p.getArrivalDate().getDayOfMonth()).containsExactly(2, 4, 5);  // Oct 3 not invented
    }
}
