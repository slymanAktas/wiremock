package com.learnwiremock.service;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.learnwiremock.constants.MoviesAppConstants;
import com.learnwiremock.dto.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(WireMockExtension.class) // Spin up and shut down wiremock server
public class MoviesMockRestClientTest {

    MoviesRestClient moviesRestClient;
    WebClient webClient;

    @InjectServer
    WireMockServer wireMockServer;

    // Default wiremock getting up on 8080
    @ConfigureWireMock
    Options options = wireMockConfig()
            .port(8088)
            .notifier(new ConsoleNotifier(true)) // Printing log detail to console
//            .notifier(new ConsoleNotifier(false)); // if set as false lo longer log on console
            .extensions(new ResponseTemplateTransformer(true)); // This conf is enable response templating functionality

    @BeforeEach
    void setUp() {
        int port = wireMockServer.port();
        String baseUrl = String.format("http://localhost:%s/", port);
        System.out.println("Base Url: " + baseUrl + "");

        webClient = WebClient.create(baseUrl);
        moviesRestClient = new MoviesRestClient(webClient);
    }

    @Test
    void retrieveAllMovies() {
        stubFor(
                get(anyUrl())
                        .willReturn(WireMock.aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBodyFile("all-movies.json")
                        )
        );

        List<Movie> movies = moviesRestClient.retrieveAllMovies();
        assertTrue(movies.size() > 0);
    }

    @Test
    void retrieveAllMoviesMatchesUrl() {
        stubFor(
                get(urlPathEqualTo(MoviesAppConstants.GET_ALL_MOVIES_V1))
                        .willReturn(WireMock.aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBodyFile("all-movies.json")
                        )
        );

        List<Movie> movies = moviesRestClient.retrieveAllMovies();
        assertTrue(movies.size() > 0);
    }

    @Test
    void retrieveMovieById_notFound() {
        //Given
        stubFor(
                get(urlPathMatching("/movieservice/v1/movie/[0-9]+"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(HttpStatus.NOT_FOUND.value())
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("404-movie-id.json")
                        )
        );

        //When
        int movieId = 100;
        assertThrows(
                WebClientResponseException.class,
                () -> moviesRestClient.retrieveMovieById(movieId)
        );
    }

    @Test
    void retrieveMovieByName() {
        //Given
        String movieName = "Avengers";

//        // Classical way of query params
//        stubFor(
//                get(urlEqualTo(MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=" + movieName)) // urlEqualTo when using single url text
//                        .willReturn(
//                                WireMock.aResponse()
//                                        .withStatus(HttpStatus.OK.value())
//                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                                        .withBodyFile("avengers.json")
//                        )
//        );

        // Handsome way of query params
        stubFor(
                get(urlPathEqualTo(MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1)) // urlPathEqualTo when using withQueryParam
                        .withQueryParam("movie_name", equalTo(movieName))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(HttpStatus.OK.value())
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("avengers.json")
                        )
        );

        //When
        List<Movie> movies = moviesRestClient.retrieveMovieByName(movieName);
        assertEquals(movies.size(), 4);
    }

    @Test
    void retrieveMovieByName_responseTemplating() {
        //Given
        String movieName = "Avengers";

        stubFor(
                get(urlEqualTo(MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1 + "?movie_name=" + movieName))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(HttpStatus.OK.value())
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("movie-byName-template.json")
                        )
        );

        //When
        List<Movie> movies = moviesRestClient.retrieveMovieByName(movieName);
        System.out.println(movies);
        assertEquals(movies.size(), 4);
        assertEquals(movies.get(0).getName(), movieName);
    }

    @Test
    void addMovieByName_responseTemplating() {
        Movie addingMovie = new Movie(
                null,
                "Av Mevsimi",
                "Şener Şen",
                2012,
                LocalDate.of(2012, 7, 21)
        );

        stubFor(
                post(urlPathEqualTo(MoviesAppConstants.ADD_MOVIE_V1))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(HttpStatus.OK.value())
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("add-movie-template.json")
                        )
        );

        Movie addedMovie = moviesRestClient.addMovie(addingMovie);
        assertEquals(addedMovie.getName(), "Av Mevsimi");
        System.out.println(addedMovie);
    }

    @Test
    void addMovieByName_badRequest() {
        Movie addingMovie = new Movie(
                null,
                "Av Mevsimi",
                "Şener Şen",
                2012,
                LocalDate.of(2012, 7, 21)
        );

        stubFor(
                post(urlPathEqualTo(MoviesAppConstants.ADD_MOVIE_V1))
                        .withRequestBody(matchingJsonPath("$.cast", containing("Şener")))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(HttpStatus.BAD_REQUEST.value())
                                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .withBodyFile("400-bad-request.json")
                        )
        );


        Exception exception = assertThrows(
                WebClientResponseException.class,
                () -> moviesRestClient.addMovie(addingMovie)
        );

        assertEquals("400 Bad Request", exception.getMessage());
    }
}
