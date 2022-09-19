package com.learnwiremock.service;

import com.learnwiremock.dto.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class MoviesRestClientTest {
    MoviesRestClient moviesRestClient;
    WebClient webClient;

    @BeforeEach
    void setUp(){
        String baseUrl = "http://localhost:8081";
        webClient = WebClient.create(baseUrl);
        moviesRestClient = new MoviesRestClient(webClient);
    }

    @Test
    void retrieveAllMovies(){
        List<Movie> movies = moviesRestClient.retrieveAllMovies();
        assertTrue(movies.size() > 0);
    }

    @Test
    void retrieveMovieById(){
        Movie movie = moviesRestClient.retrieveMovieById(1);
        assertEquals("Batman Begins", movie.getName());
    }

    @Test
    void retrieveMovieByIdNotFound(){
        assertThrows(
                WebClientResponseException.class,
                () -> moviesRestClient.retrieveMovieById(100)
        );
    }

//    @Test
//    void addMovie(){
//        Movie addingMovie = new Movie(
//                null,
//                "Çakallarla Dans",
//                "Şevket Çoryh",
//                2015,
//                LocalDate.of(2015, 7, 21)
//        );
//
//        Movie addedMovie= moviesRestClient.addMovie(addingMovie);
//        assertNotNull(addedMovie.getMovie_id());
//    }

    @Test
    void removeMovieById(){
        Movie addingMovie = new Movie(
                null,
                "Kabadayı",
                "Şener Şen",
                2008,
                LocalDate.of(2008, 7, 21)
        );

        Movie addedMovie= moviesRestClient.addMovie(addingMovie);
        String actualMsg = moviesRestClient.deleteMovie(addedMovie.getMovie_id().intValue());
        assertEquals(actualMsg, "Movie Deleted Successfully");
    }

}
