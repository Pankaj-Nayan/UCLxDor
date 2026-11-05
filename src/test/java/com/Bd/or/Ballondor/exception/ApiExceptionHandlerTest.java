package com.Bd.or.Ballondor.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiExceptionHandlerTest {
    @Test
    void returnsStructuredNotFoundWithoutInternalDetails() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/players/123");
        ApiExceptionHandler handler = new ApiExceptionHandler();

        var response = handler.handleApiException(
                new ApiException(HttpStatus.NOT_FOUND, "PLAYER_NOT_FOUND", "Player not found"), request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("PLAYER_NOT_FOUND", response.getBody().get("code"));
        assertEquals("/api/v1/players/123", response.getBody().get("path"));
    }
}
