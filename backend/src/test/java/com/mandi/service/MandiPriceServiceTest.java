package com.mandi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mandi.client.DataGovMandiClient;
import com.mandi.client.DataGovPage;
import com.mandi.config.MandiProperties;
import com.mandi.dto.MandiPriceResponse;
import com.mandi.dto.PriceQuery;
import com.mandi.exception.BadRequestException;
import com.mandi.exception.ExternalApiException;
import java.util.List;
import org.junit.jupiter.api.Test;

class MandiPriceServiceTest {
    MandiProperties props = new MandiProperties(null, List.of("Maharashtra", "Karnataka"), null, null, null, null, List.of());
    FilterValidator validator = new FilterValidator(props);
    DataGovMandiClient client = mock(DataGovMandiClient.class);
    HistoryIngestService ingest = mock(HistoryIngestService.class);
    MandiPriceService service = new MandiPriceService(client, ingest);

    @Test
    void validatorCanonicalisesState() {
        assertThat(validator.requireState(" maharashtra ")).isEqualTo("Maharashtra");
    }

    @Test
    void invalidStateIsRejected() {
        assertThatThrownBy(() -> validator.requireState("Atlantis")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.requireState(" ")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void paginationBoundsAreValidated() {
        assertThat(validator.limit(null)).isEqualTo(20);
        assertThat(validator.offset(null)).isZero();
        assertThatThrownBy(() -> validator.limit(0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.limit(5000)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.offset(-1)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void allMeansNoFilter() {
        assertThat(validator.optional("All")).isNull();
        assertThat(validator.optional("  ")).isNull();
        assertThat(validator.optional(" Latur ")).isEqualTo("Latur");
    }

    @Test
    void emptyUpstreamResultIsAnEmptyResponseNotAnError() {
        when(client.fetch(anyMap(), eq(20), eq(0))).thenReturn(new DataGovPage(0, List.of(), 0, 0));
        MandiPriceResponse r = service.getPrices(new PriceQuery("Maharashtra", "Latur", null, "Soybean", null, null, 20, 0));
        assertThat(r.records()).isEmpty();
        assertThat(r.total()).isZero();
        verifyNoInteractions(ingest);
    }

    @Test
    void upstreamFailurePropagatesAsExternalApiException() {
        when(client.fetch(anyMap(), anyInt(), anyInt())).thenThrow(new ExternalApiException("down"));
        assertThatThrownBy(() -> service.getPrices(new PriceQuery("Maharashtra", null, null, null, null, null, 20, 0)))
                .isInstanceOf(ExternalApiException.class);
    }
}
