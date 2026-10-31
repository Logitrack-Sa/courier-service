package com.logitrack.courierservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.courierservice.dto.CourierAvailabilityResponse;
import com.logitrack.courierservice.dto.CourierRequest;
import com.logitrack.courierservice.dto.CourierResponse;
import com.logitrack.courierservice.dto.CourierStatusRequest;
import com.logitrack.courierservice.entity.CourierStatus;
import com.logitrack.courierservice.exception.BusinessRuleException;
import com.logitrack.courierservice.exception.ResourceNotFoundException;
import com.logitrack.courierservice.service.implementation.CourierServiceImp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
@WebMvcTest(CourierController.class)
@Import(ObjectMapper.class)
class CourierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourierServiceImp courierService;

    @Test
    void createCourierReturnsCreated() throws Exception {
        UUID courierId = UUID.randomUUID();
        CourierRequest request = new CourierRequest("Luis Pérez", "MOTO");
        CourierResponse response = new CourierResponse(courierId, "Luis Pérez", "MOTO", CourierStatus.ACTIVO, Instant.now());

        when(courierService.create(any(CourierRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(courierId.toString()))
                .andExpect(jsonPath("$.name").value("Luis Pérez"))
                .andExpect(jsonPath("$.status").value("ACTIVO"));
    }

    @Test
    void createCourierReturnsBadRequestWhenDniInvalid() throws Exception {
        CourierRequest request = new CourierRequest("Luis Pérez", "ABC123", "MOTO");

        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createCourierReturnsUnprocessableEntityWhenDniDuplicated() throws Exception {
        CourierRequest request = new CourierRequest("Luis Pérez", "38456789", "MOTO");

        when(courierService.create(any(CourierRequest.class)))
                .thenThrow(new BusinessRuleException("DUPLICATE_DNI", "Ya existe un repartidor registrado con el DNI 38456789"));

        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DUPLICATE_DNI"));
    }

    @Test
    void getCourierByIdReturnsOk() throws Exception {
        UUID courierId = UUID.randomUUID();
        CourierResponse response = new CourierResponse(courierId, "Carlos Gómez", "AUTO", CourierStatus.ACTIVO, Instant.now());

        when(courierService.findById(courierId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/couriers/{id}", courierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courierId.toString()))
                .andExpect(jsonPath("$.name").value("Carlos Gómez"));
    }

    @Test
    void getCourierByIdReturnsNotFoundWhenNotExists() throws Exception {
        UUID courierId = UUID.randomUUID();

        when(courierService.findById(courierId)).thenThrow(new ResourceNotFoundException("Repartidor ", courierId));

        mockMvc.perform(get("/api/v1/couriers/{id}", courierId))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeStatusReturnsOk() throws Exception {
        UUID courierId = UUID.randomUUID();
        CourierStatusRequest request = new CourierStatusRequest(CourierStatus.INACTIVO);
        CourierResponse response = new CourierResponse(courierId, "Luis Pérez", "MOTO", CourierStatus.INACTIVO, Instant.now());

        when(courierService.changeStatus(eq(courierId), eq(CourierStatus.INACTIVO))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/couriers/{id}/status", courierId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVO"));
    }

    @Test
    void getAvailabilityReturnsOkWhenCourierExists() throws Exception {
        UUID courierId = UUID.randomUUID();
        CourierAvailabilityResponse response = new CourierAvailabilityResponse(
                courierId, CourierStatus.ACTIVO, true
        );

        when(courierService.getAvailability(courierId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/couriers/{id}/availability", courierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(courierId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVO"))
                .andExpect(jsonPath("$.available").value(true));
    }
}
