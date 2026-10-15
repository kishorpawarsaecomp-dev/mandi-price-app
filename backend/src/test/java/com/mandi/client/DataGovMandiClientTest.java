package com.mandi.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.mandi.config.MandiProperties;
import com.mandi.exception.ExternalApiException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DataGovMandiClientTest {
    MockRestServiceServer server;
    DataGovMandiClient client;

    static MandiProperties props(String key) {
        return new MandiProperties(new MandiProperties.Api(key, "https://api.test/resource", "rid", 1000, 1000, 100),
                List.of("Maharashtra"), new MandiProperties.Sync("* * * * * *", "UTC", List.of(), 5, false, ""),
                new MandiProperties.Trend(3.0, 4), new MandiProperties.Discovery(5000),
                new MandiProperties.Cache(10, 360), List.of());
    }

    @BeforeEach
    void setUp() {
        RestClient.Builder b = RestClient.builder();
        server = MockRestServiceServer.bindTo(b).build();
        client = new DataGovMandiClient(b.build(), props("test-key"));
    }

    @Test
    void parsesValidRecordAndSendsFiltersWithoutLeakingAnythingElse() {
        String json = """
            {"total":1,"records":[{"state":"Maharashtra","district":"Latur","market":"APMC Aurad Shahajani",
            "commodity":"Red gram/Arhar/Tur(whole)","variety":"Other","grade":"FAQ","arrival_date":"15/10/2026",
            "min_price":"9400","max_price":"10200","modal_price":"9800","new_unknown_field":"x"}]}""";
        server.expect(requestTo(containsString("filters%5Bstate.keyword%5D=Maharashtra")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        DataGovPage page = client.fetch(Map.of("state", "Maharashtra"), 20, 0);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.records()).hasSize(1);
        assertThat(page.records().get(0).modalPrice()).isEqualByComparingTo("9800");
        assertThat(page.records().get(0).arrivalDate().toString()).isEqualTo("2026-10-15");
    }

    @Test
    void emptyRecordsIsNotAnError() {
        server.expect(requestTo(containsString("api.test"))).andRespond(withSuccess("{\"total\":0,\"records\":[]}", MediaType.APPLICATION_JSON));
        DataGovPage page = client.fetch(Map.of("state", "Maharashtra"), 20, 0);
        assertThat(page.records()).isEmpty();
    }

    @Test
    void invalidPricesAndDatesAreDropped() {
        String json = """
            {"total":5,"records":[
             {"state":"S","district":"D","market":"M","commodity":"C","arrival_date":"01/10/2026","modal_price":"0"},
             {"state":"S","district":"D","market":"M","commodity":"C","arrival_date":"01/10/2026","modal_price":"-5"},
             {"state":"S","district":"D","market":"M","commodity":"C","arrival_date":"01/10/2026","modal_price":"abc"},
             {"state":"S","district":"D","market":"M","commodity":"C","arrival_date":"not-a-date","modal_price":"100"},
             {"state":"S","district":"D","market":"M","commodity":"C","arrival_date":"01/10/2026","modal_price":null}]}""";
        server.expect(requestTo(containsString("api.test"))).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        DataGovPage page = client.fetch(Map.of("state", "S"), 20, 0);
        assertThat(page.records()).isEmpty();
        assertThat(page.invalidCount()).isEqualTo(5);
    }

    @Test
    void malformedResponseThrows() {
        server.expect(requestTo(containsString("api.test"))).andRespond(withSuccess("{\"status\":\"error\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.fetch(Map.of("state", "S"), 20, 0)).isInstanceOf(ExternalApiException.class);
    }

    @Test
    void serverErrorIsRetriedOnceThenThrows() {
        server.expect(ExpectedCount.twice(), requestTo(containsString("api.test"))).andRespond(withServerError());
        assertThatThrownBy(() -> client.fetch(Map.of("state", "S"), 20, 0)).isInstanceOf(ExternalApiException.class);
        server.verify();
    }

    @Test
    void missingApiKeyFailsWithoutCallingUpstream() {
        DataGovMandiClient noKey = new DataGovMandiClient(RestClient.builder().build(), props(""));
        assertThatThrownBy(() -> noKey.fetch(Map.of("state", "S"), 20, 0)).isInstanceOf(ExternalApiException.class);
    }
}
