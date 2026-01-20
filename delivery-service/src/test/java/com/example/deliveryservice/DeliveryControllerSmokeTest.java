package com.example.deliveryservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryControllerSmokeTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

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

    @Test
    void createAndUpdateDeliveryStatus() throws Exception {
        var createBody = mapper.writeValueAsString(new DeliveryController.CreateDeliveryRequest(1L, 1L));

        String createdJson = mvc.perform(post("/deliveries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.driverId").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = mapper.readTree(createdJson).get("id").asLong();

        // can fetch by orderId
        mvc.perform(get("/deliveries/by-order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        var updateBody = mapper.writeValueAsString(new DeliveryController.UpdateDeliveryStatusRequest("PICKED_UP"));

        mvc.perform(patch("/deliveries/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));
    }
}
