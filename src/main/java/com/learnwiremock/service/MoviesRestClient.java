package com.learnwiremock.service;

import com.learnwiremock.constants.MoviesAppConstants;
import com.learnwiremock.dto.Movie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class MoviesRestClient {

    private final WebClient webClient;

    public MoviesRestClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<Movie> retrieveAllMovies() {
        return checkException(
                () -> webClient.get().uri(MoviesAppConstants.GET_ALL_MOVIES_V1)
                        .retrieve()
                        .bodyToFlux(Movie.class)
                        .collectList()
                        .block()
        );
    }

    public Movie retrieveMovieById(int movieId) {
        return checkException(
                () -> webClient.get().uri(MoviesAppConstants.MOVIE_BY_ID_V1, movieId)
                        .retrieve()
                        .bodyToMono(Movie.class)
                        .block()
        );
    }

    public List<Movie> retrieveMovieByName(String name) {
        String retrieveByNameUri = UriComponentsBuilder.fromUriString(MoviesAppConstants.MOVIE_BY_NAME_QUERY_PARAM_V1)
                .queryParam("movie_name", name)
                .buildAndExpand()
                .toUriString();

        return checkException(
                () -> webClient.get().uri(retrieveByNameUri)
                        .retrieve()
                        .bodyToFlux(Movie.class)
                        .collectList()
                        .block()
        );
    }

    public Movie addMovie(Movie movie) {
        return checkException(
                () -> webClient.post().uri(MoviesAppConstants.ADD_MOVIE_V1)
                        .syncBody(movie)
                        .retrieve() // invoke the endpoint
                        .bodyToMono(Movie.class) // Extract then model response
                        .block()
        );
    }

    public String deleteMovie(int movieId){
        return checkException(
                () -> webClient.delete().uri(MoviesAppConstants.MOVIE_BY_ID_V1, movieId)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block()
        );
    }

    public <T> T checkException(Supplier<T> supplier) {

        try {
            return supplier.get();
        } catch (WebClientResponseException exc) {
            log.error("WebClientResponseException status is {}", exc.getStatusCode());
            throw exc;
        } catch (Exception exc) {
            log.error("Exception in retrieveMovieById is {}", exc.getMessage());
            throw exc;
        }
    }
}
