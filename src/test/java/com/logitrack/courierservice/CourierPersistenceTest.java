package com.logitrack.courierservice;

import com.logitrack.courierservice.entity.CourierStatus;
import com.logitrack.courierservice.repository.CourierRepository;
import com.logitrack.courierservice.support.JwtTestSupport;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class CourierPersistenceTest extends JwtTestSupport {
    @Autowired MockMvc http;
    @Autowired CourierRepository couriers;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entities;

    @Test void createAndReadWithRealPersistence() throws Exception {
        var body = http.perform(post("/api/v1/couriers").contentType("application/json")
                .content("{\"name\":\"Ana\",\"vehicleType\":\"MOTO\"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        var id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("id").asText();
        entities.flush(); entities.clear();
        assertThat(couriers.findById(UUID.fromString(id))).isPresent();
        http.perform(get("/api/v1/couriers/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Ana"));
        http.perform(get("/api/v1/couriers")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(id));
        var saved = couriers.findById(UUID.fromString(id)).orElseThrow();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("system");
        assertThat(saved.getVersion()).isZero();
    }
    @Test void invalidPayloadDoesNotWrite() throws Exception {
        long before = couriers.count();
        http.perform(post("/api/v1/couriers").contentType("application/json").content("{\"name\":\" \",\"vehicleType\":\"MOTO\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertThat(couriers.count()).isEqualTo(before);
    }
    @Test void callerCannotChooseUuid() throws Exception {
        http.perform(post("/api/v1/couriers").contentType("application/json")
                .content("{\"name\":\"Ana\",\"vehicleType\":\"MOTO\",\"id\":\""+UUID.randomUUID()+"\"}"))
                .andExpect(status().isBadRequest());
        assertThat(couriers.count()).isZero();
    }
    @Test void updatesPersistAndInactiveCourierIsUnavailable() throws Exception {
        var courier = couriers.saveAndFlush(new com.logitrack.courierservice.entity.Courier("Antes", "BICI"));
        var id = courier.getId();
        http.perform(put("/api/v1/couriers/"+id).contentType("application/json")
                .content("{\"name\":\"Después\",\"vehicleType\":\"MOTO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));
        http.perform(patch("/api/v1/couriers/"+id+"/status").contentType("application/json")
                .content("{\"status\":\"INACTIVO\"}")).andExpect(status().isOk());
        entities.flush(); entities.clear();
        assertThat(couriers.findById(id).orElseThrow().getName()).isEqualTo("Después");
        http.perform(get("/api/v1/couriers/"+id+"/availability"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
        http.perform(get("/api/v1/couriers/"+UUID.randomUUID()+"/availability"))
                .andExpect(status().isNotFound());
    }
    @Test void updateRejectsInvalidAndPrivilegedFieldsWithoutWriting() throws Exception {
        var courier = couriers.saveAndFlush(new com.logitrack.courierservice.entity.Courier("Original", "BICI"));
        for (String body : new String[]{"{\"name\":\" \",\"vehicleType\":\"MOTO\"}",
                "{\"name\":\"Otro\",\"vehicleType\":\"MOTO\",\"id\":\""+UUID.randomUUID()+"\"}"}) {
            http.perform(put("/api/v1/couriers/"+courier.getId()).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        entities.flush(); entities.clear();
        assertThat(couriers.findById(courier.getId()).orElseThrow().getName()).isEqualTo("Original");
    }
    @Test void missingAndMalformedIdHaveControlledErrors() throws Exception {
        http.perform(get("/api/v1/couriers/"+UUID.randomUUID())).andExpect(status().isNotFound());
        http.perform(get("/api/v1/couriers/not-a-uuid")).andExpect(status().isBadRequest());
    }
    @Test void importsExistingUuidDataAndAuditWithoutRegeneration() throws Exception {
        var id = UUID.fromString("482f7f31-e36a-4f8f-b480-5156aeabc401");
        var bytes = HexFormat.of().parseHex(id.toString().replace("-", ""));
        var timestamp = Timestamp.from(Instant.parse("2025-01-02T03:04:05Z"));
        jdbc.update("INSERT INTO couriers(id,name,vehicle_type,status,created_at,updated_at,created_by,last_modified_by,version) VALUES(?,?,?,?,?,?,?,?,?)",
                bytes,"Importado","BICI","INACTIVO",timestamp,timestamp,"legacy-admin","legacy-editor",7L);
        entities.clear();
        var imported = couriers.findById(id).orElseThrow();
        assertThat(imported.getName()).isEqualTo("Importado");
        assertThat(imported.getStatus()).isEqualTo(CourierStatus.INACTIVO);
        assertThat(imported.getCreatedAt()).isEqualTo(timestamp.toInstant());
        assertThat(imported.getUpdatedAt()).isEqualTo(timestamp.toInstant());
        assertThat(imported.getCreatedBy()).isEqualTo("legacy-admin");
        assertThat(imported.getLastModifiedBy()).isEqualTo("legacy-editor");
        assertThat(imported.getVersion()).isEqualTo(7);
        http.perform(get("/api/v1/couriers/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()));
    }
}
