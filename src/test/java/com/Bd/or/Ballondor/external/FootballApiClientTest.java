package com.Bd.or.Ballondor.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.Bd.or.Ballondor.config.FootballApiProperties;
import com.Bd.or.Ballondor.exception.ApiException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FootballApiClientTest {
    @Test
    void searchesPlayersUsingApiFootballV3SearchEndpoint() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.test/players?search=Vinicius&league=2&season=2024&page=1"))
                .andExpect(header("x-apisports-key", "secret-for-test"))
                .andRespond(withSuccess("""
                        {"errors":[],"response":[{"player":{"id":7,"name":"Vinicius Junior"},"statistics":[]}],
                        "paging":{"current":1,"total":1}}
                        """, MediaType.APPLICATION_JSON));
        FootballApiClient client = new FootballApiClient(builder.build(), properties());

        assertEquals(7, client.searchPlayers("Vinicius").getFirst().player().id());
        server.verify();
    }

    @Test
    void parsesApiV3PlayerStatisticsAndMissingValues() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.test/players?id=7&league=2&season=2024"))
                .andExpect(header("x-apisports-key", "secret-for-test"))
                .andRespond(withSuccess("""
                        {"errors":[],"response":[{"player":{"id":7,"name":"Test Player","age":24,"nationality":"Country","photo":"https://photo"},
                        "statistics":[{"team":{"id":4,"name":"Test Club"},"games":{"appearances":8,"minutes":620,"position":"Attacker","rating":"7.2"},
                        "goals":{"total":3,"assists":2},"shots":{"total":14,"on":7},"passes":{"key":9,"accuracy":"84%"},
                        "tackles":{"total":2,"interceptions":1}}]}],"paging":{"current":1,"total":1}}
                        """, MediaType.APPLICATION_JSON));
        FootballApiProperties properties = properties();
        FootballApiClient client = new FootballApiClient(builder.build(), properties);

        PlayerSeasonData data = client.getPlayerStatistics(7, 2024);

        assertEquals(7, data.player().id());
        assertEquals("Test Club", data.player().clubName());
        assertEquals(3, data.statistics().goals());
        assertEquals(84, data.statistics().passAccuracy());
        assertNull(data.statistics().saves());
        server.verify();
    }

    @Test
    void reportsProviderRateLimitAsAUsefulApiError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.test/leagues?id=2"))
                .andRespond(withSuccess("{\"errors\":{\"requests\":\"rate limit exceeded\"},\"response\":[]}", MediaType.APPLICATION_JSON));
        FootballApiClient client = new FootballApiClient(builder.build(), properties());

        ApiException error = assertThrows(ApiException.class, client::getSupportedSeasons);

        assertEquals("FOOTBALL_API_RATE_LIMIT", error.getCode());
        server.verify();
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void logsUpstreamStatusAndRedactsKeyFromResponseBody(CapturedOutput output) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.test/players?search=Vinicius&league=2&season=2024&page=1"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"rejected secret-for-test\"}"));
        FootballApiClient client = new FootballApiClient(builder.build(), properties());

        ApiException error = assertThrows(ApiException.class, () -> client.searchPlayers("Vinicius"));

        assertEquals("FOOTBALL_API_AUTHENTICATION_FAILED", error.getCode());
        assertTrue(output.getAll().contains("HTTP 401"));
        assertTrue(output.getAll().contains("{\"error\":\"rejected [REDACTED]\"}"));
        assertFalse(output.getAll().contains("secret-for-test"));
        server.verify();
    }

    private FootballApiProperties properties() {
        FootballApiProperties properties = new FootballApiProperties();
        properties.setBaseUrl("https://api.test");
        properties.setKey("secret-for-test");
        properties.setCompetitionId(2);
        properties.setSeason(2024);
        return properties;
    }
}
