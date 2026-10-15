package com.mandi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mandi.config.MandiProperties;
import com.mandi.dto.HistoricalPriceResponse;
import com.mandi.dto.PriceType;
import com.mandi.entity.MandiPrice;
import com.mandi.exception.BadRequestException;
import com.mandi.repository.MandiPriceRepository;
import com.mandi.repository.MandiPriceRepository.CoverageView;
import com.mandi.repository.MandiPriceRepository.MarketCountView;
import com.mandi.repository.MandiPriceRepository.VariantCount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoricalPriceServiceTest {
    MandiPriceRepository repo = mock(MandiPriceRepository.class);
    MandiProperties props = new MandiProperties(null, List.of(), null, new MandiProperties.Trend(3.0, 4), null, null, List.of());
    HistoricalPriceService service = new HistoricalPriceService(repo, props);

    static VariantCount variant(String v, String g, long n) {
        VariantCount x = mock(VariantCount.class);
        when(x.getVariety()).thenReturn(v); when(x.getGrade()).thenReturn(g); when(x.getCnt()).thenReturn(n);
        return x;
    }
    static MarketCountView market(String m, long n) {
        MarketCountView x = mock(MarketCountView.class);
        when(x.getMarket()).thenReturn(m); when(x.getCnt()).thenReturn(n);
        return x;
    }
    static CoverageView cov(LocalDate a, LocalDate b) {
        CoverageView x = mock(CoverageView.class);
        when(x.getFirstDate()).thenReturn(a); when(x.getLastDate()).thenReturn(b);
        return x;
    }
    static MandiPrice row(String market, LocalDate d, int modal) {
        MandiPrice p = new MandiPrice();
        p.setMarket(market); p.setArrivalDate(d); p.setModalPrice(BigDecimal.valueOf(modal));
        return p;
    }
    HistoricalPriceService.Request req(LocalDate from, LocalDate to) {
        return new HistoricalPriceService.Request("Maharashtra", "Latur", "Tur", null, List.of(), null, null, from, to, PriceType.MODAL);
    }

    @Test
    void noStoredHistoryGivesEmptyResponseWithMessage() {
        when(repo.variantCounts(any(), any(), any(), any(), any())).thenReturn(List.of());
        HistoricalPriceResponse r = service.history(req(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));
        assertThat(r.records()).isEmpty();
        assertThat(r.coverage().sufficient()).isFalse();
    }

    @Test
    void usesRequestedDateRangeAndReportsInsufficientHistory() {
        LocalDate to = LocalDate.of(2026, 10, 15), from = to.minusDays(179);   // 6M request
        when(repo.variantCounts(any(), any(), any(), any(), any())).thenReturn(List.of(variant("Other", "FAQ", 5)));
        when(repo.marketCounts(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(market("A", 5)));
        when(repo.findSeries(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(
                row("A", to.minusDays(4), 100), row("A", to.minusDays(3), 101), row("A", to.minusDays(1), 102), row("A", to, 103)));
        when(repo.coverage(any(), any(), any(), any(), any(), any(), any())).thenReturn(cov(to.minusDays(4), to));

        HistoricalPriceResponse r = service.history(req(from, to));

        verify(repo).findSeries(eq("Maharashtra"), eq("Latur"), eq("Tur"), eq("Other"), eq("FAQ"), eq(from), eq(to), anyCollection());
        assertThat(r.records()).hasSize(4);                         // exactly what exists; no filled days
        assertThat(r.coverage().sufficient()).isFalse();            // only ~5 days of history for a 180-day request
        assertThat(r.coverage().message()).contains("Not enough history");
        assertThat(r.stats()).hasSize(1);
    }

    @Test
    void multipleVarietiesAreNotMerged() {
        LocalDate to = LocalDate.of(2026, 10, 15);
        when(repo.variantCounts(any(), any(), any(), any(), any()))
                .thenReturn(List.of(variant("Hybrid", "FAQ", 9), variant("Local", "FAQ", 3)));
        when(repo.marketCounts(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(repo.coverage(any(), any(), any(), any(), any(), any(), any())).thenReturn(cov(null, null));
        HistoricalPriceResponse r = service.history(req(to.minusDays(29), to));
        assertThat(r.variety()).isEqualTo("Hybrid");               // single series chosen by explicit rule
        assertThat(r.aggregationNote()).contains("never combined");
        assertThat(r.availableVariants()).hasSize(2);
    }

    @Test
    void invalidRangeIsRejected() {
        assertThatThrownBy(() -> service.history(req(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 1))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void coverageIsSufficientWhenHistoryStartsBeforePeriod() {
        LocalDate start = LocalDate.of(2026, 9, 16);
        var c = HistoricalPriceService.coverage(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 15), 24, 30, start);
        assertThat(c.sufficient()).isTrue();
        assertThat(c.message()).contains("24 of 30");
    }
}
