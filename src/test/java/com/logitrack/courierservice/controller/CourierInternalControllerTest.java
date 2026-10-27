package com.logitrack.courierservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logitrack.courierservice.dto.CourierStatusResponse;
import com.logitrack.courierservice.entity.CourierStatus;
import com.logitrack.courierservice.service.CourierService;
import com.logitrack.courierservice.service.CourierStatusQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.test.context.ActiveProfiles("test")
@WebMvcTest(InternalCourierController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class CourierInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourierStatusQueryService courierService;

    @Test
    void getInternalStatusReturnsOkWhenCourierExists() throws Exception {
        UUID courierId = UUID.randomUUID();
        CourierStatusResponse response = new CourierStatusResponse(
                courierId, CourierStatus.ACTIVO
        );

        when(courierService.findStatus(courierId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/internal/couriers/{id}/status", courierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(courierId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVO"));
    }
}