package com.example.deliveryservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryControllerSmokeTest {

    @Autowired
    MockMvc mvc;

    @Test
    void infoEndpointReturns200() throws Exception {
        mvc.perform(get("/info"))
                .andExpect(status().isOk());
    }

    @Test
    void getNonExistingDeliveryReturns404() throws Exception {
        mvc.perform(get("/deliveries/999999"))
                .andExpect(status().isNotFound());
    }
}
