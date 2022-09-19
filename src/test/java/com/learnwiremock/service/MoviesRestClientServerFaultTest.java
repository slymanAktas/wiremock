package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.learnwiremock.constants.MoviesAppConstants.GET_ALL_MOVIES_V1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(WireMockExtension.class)
public class MoviesRestClientServerFaultTest {
    MoviesRestClient moviesRestClient;
    WebClient webClient;

    @InjectServer
    WireMockServer wireMockServer;

    @ConfigureWireMock
    Options options = wireMockConfig()
            .port(8088)
            .notifier(new ConsoleNotifier(true))
            .extensions(new ResponseTemplateTransformer(true));

    @BeforeEach
    void setUp() {
        int port = wireMockServer.port();
        String baseUrl = String.format("http://localhost:%s", port);

        webClient = WebClient.create(baseUrl);
        moviesRestClient = new MoviesRestClient(webClient);
    }

    @Test
    void retrieveAllMovies_500() {
        stubFor(
                get(anyUrl())
                        .willReturn(
                                serverError()
                        )
        );

        Exception exception = assertThrows(
                WebClientResponseException.class,
                () -> moviesRestClient.retrieveAllMovies()
        );

        assertEquals(exception.getMessage(), "500 Internal Server Error");
    }

    @Test
    void retrieveAllMovies_503() {
        stubFor(
                get(urlEqualTo(GET_ALL_MOVIES_V1))
                        .willReturn(
                                serverError()
                                        .withStatus(HttpStatus.SERVICE_UNAVAILABLE.value())
                                        .withBody("sdgsdfgsdfgsdfg")
                        )
        );

        WebClientResponseException exception = assertThrows(
                WebClientResponseException.class,
                () -> moviesRestClient.retrieveAllMovies()
        );

        assertEquals(exception.getMessage(), "503 Service Unavailable");

        verify(
                moreThan(1),
                getRequestedFor(
                        urlEqualTo(GET_ALL_MOVIES_V1)
                )
        );
    }
    @Test
    void retrieveAllMovies_networkError() {
        stubFor(
                get(urlEqualTo(GET_ALL_MOVIES_V1))
                        .willReturn(
                                aResponse().withFault(Fault.EMPTY_RESPONSE)
                        )
        );

        Exception exception = assertThrows(
                Exception.class,
                () -> moviesRestClient.retrieveAllMovies()
        );

        assertEquals(
                exception.getMessage()
                , "reactor.netty.http.client.PrematureCloseException: Connection prematurely closed BEFORE response"
        );

        verify(
                moreThan(1),
                getRequestedFor(
                        urlEqualTo(GET_ALL_MOVIES_V1)
                )
        );
    }
}
