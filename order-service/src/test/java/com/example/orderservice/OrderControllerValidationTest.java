package com.example.orderservice;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderControllerValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void createOrder_invalidPayload_hasErrorsForUserIdAndRestaurantId() {
        var req = new OrderController.CreateOrderRequest(0L, 0L, List.of());

        var violations = validator.validate(req);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("userId", "restaurantId");
    }

    @Test
    void createOrder_invalidItems_hasErrorsForMenuItemIdAndQuantity() {
        var req = new OrderController.CreateOrderRequest(
                1L,
                1L,
                List.of(new OrderController.CreateOrderItemRequest(0L, 0))
        );

        var violations = validator.validate(req);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("items[0].menuItemId", "items[0].quantity");
    }

    @Test
    void pay_invalidPayload_hasErrorForAmount() {
        var req = new OrderController.PayOrderRequest(0.0, null);

        var violations = validator.validate(req);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("amount");
    }

    @Test
    void pay_invalidDriverId_hasErrorForDriverId() {
        var req = new OrderController.PayOrderRequest(10.0, 0L);

        var violations = validator.validate(req);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("driverId");
    }
}
