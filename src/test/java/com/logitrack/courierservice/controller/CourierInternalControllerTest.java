package com.logitrack.courierservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.courierservice.dto.CourierInternalStatusResponse;
import com.logitrack.courierservice.entity.CourierStatus;
import com.logitrack.courierservice.service.CourierService;
import com.logitrack.courierservice.service.implementation.CourierServiceImp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourierInternalController.class)
@Import(ObjectMapper.class)
class CourierInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourierServiceImp courierService;

    @Test
    void getInternalStatusReturnsOkWhenCourierExists() throws Exception {
        UUID courierId = UUID.randomUUID();
        CourierInternalStatusResponse response = new CourierInternalStatusResponse(
                courierId, CourierStatus.ACTIVO
        );

        when(courierService.getInternalStatus(courierId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/internal/couriers/{id}/status", courierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(courierId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVO"));
    }
}