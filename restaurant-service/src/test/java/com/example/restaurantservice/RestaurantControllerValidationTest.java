package com.example.restaurantservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestaurantController.class)
class RestaurantControllerValidationTest {

    @TestConfiguration
    static class StubConfig {
        @Bean
        RestaurantService restaurantService() {
            // Not used in this test (validation fails before controller calls service)
            return new RestaurantService(null) {
            };
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void createRestaurant_invalidPayload_returns400WithFields() throws Exception {
        mvc.perform(post("/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"cuisine\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.name").exists())
                .andExpect(jsonPath("$.fields.cuisine").exists());
    }

    @Test
    void listRestaurants_missingCuisine_returns400() throws Exception {
        mvc.perform(get("/restaurants"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("cuisine query param is required"));
    }
}
