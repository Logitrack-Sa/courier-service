package com.logitrack.courierservice.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.logitrack.courierservice.entity.Courier;
import com.logitrack.courierservice.repository.CourierRepository;
import com.logitrack.courierservice.support.JwtTestSupport;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica de punta a punta la cadena de filtros, la matriz de roles y el
 * aislamiento del endpoint interno de courier-service.
 *
 * <p>Los JWT se firman localmente con claves efímeras. No se invoca
 * security-service ni se usan credenciales reales.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourierSecurityIntegrationTest extends JwtTestSupport {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CourierRepository couriers;

    private Courier courierA;
    private Courier courierB;

    @BeforeEach
    void setUp() {
        couriers.deleteAll();
        courierA = couriers.saveAndFlush(new Courier("Repartidor A", "MOTO"));
        courierB = couriers.saveAndFlush(new Courier("Repartidor B", "AUTO"));
    }

    @Test
    void requiresTokenAndReturnsStructured401() throws Exception {
        mvc.perform(get("/api/v1/couriers"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/v1/internal/couriers/{id}/status", courierA.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsBadSignatureExpiryIssuerAudienceKidAndClaims() throws Exception {
        var valid = token("admin");
        var pieces = valid.split("[.]");
        var signature = Base64.getUrlDecoder().decode(pieces[2]);
        signature[0] ^= 1;
        var tampered = pieces[0] + "." + pieces[1] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

        for (String invalid : List.of(
                "bad.jwt",
                tampered,
                token("admin", Map.of("exp", Date.from(Instant.now().minusSeconds(300)))),
                token("admin", Map.of("iss", "https://other.test")),
                token("admin", Map.of("aud", List.of("other"))),
                token("admin", Map.of("roles", List.of("OWNER"))),
                token("admin", Map.of("roles", "ADMIN")),
                token("admin", Map.of("sub", "bad-uuid")),
                token("admin", Collections.singletonMap("exp", null)),
                token("admin", Collections.singletonMap("iat", null)),
                signedToken("admin", Map.of(), com.nimbusds.jose.JWSAlgorithm.RS256, "wrong-kid"),
                signedToken("admin", Map.of(), com.nimbusds.jose.JWSAlgorithm.RS512, KID))) {
            mvc.perform(get("/api/v1/couriers").header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized());
        }

        var changedHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"alg\":\"RS256\",\"kid\":\"unknown\"}".getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/v1/couriers")
                        .header("Authorization", "Bearer " + changedHeader + "." + pieces[1] + "." + pieces[2]))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanUseCrudAndInternalStatus() throws Exception {
        var json = "{\"name\":\"Nuevo\",\"vehicleType\":\"BICI\"}";
        mvc.perform(post("/api/v1/couriers").header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/couriers").header("Authorization", bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/v1/couriers/{id}", courierA.getId()).header("Authorization", bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courierA.getId().toString()));
        mvc.perform(patch("/api/v1/couriers/{id}/status", courierA.getId())
                        .header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVO"));
        mvc.perform(get("/api/v1/internal/couriers/{id}/status", courierA.getId())
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(courierA.getId().toString()))
                .andExpect(jsonPath("$.status").value("INACTIVO"))
                .andExpect(jsonPath("$.name").doesNotExist())
                .andExpect(jsonPath("$.vehicleType").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"cliente-a", "repartidor-a"})
    void nonAdminRolesCannotUseCrudOrAvailability(String identity) throws Exception {
        mvc.perform(get("/api/v1/couriers").header("Authorization", bearer(identity)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(get("/api/v1/couriers/{id}", courierA.getId()).header("Authorization", bearer(identity)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/couriers").header("Authorization", bearer(identity))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"vehicleType\":\"MOTO\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/couriers/{id}/status", courierA.getId())
                        .header("Authorization", bearer(identity))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVO\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/couriers/{id}/availability", courierA.getId())
                        .header("Authorization", bearer(identity)))
                .andExpect(status().isForbidden());
    }

    @Test
    void repartidorCanOnlyReadOwnInternalStatusAndSensitiveFieldsAreHidden() throws Exception {
        mvc.perform(get("/api/v1/internal/couriers/{id}/status", courierA.getId())
                        .header("Authorization", bearerForCourier("repartidor-a", courierA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(courierA.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVO"))
                .andExpect(jsonPath("$.name").doesNotExist())
                .andExpect(jsonPath("$.vehicleType").doesNotExist());

        mvc.perform(get("/api/v1/internal/couriers/{id}/status", courierB.getId())
                        .header("Authorization", bearerForCourier("repartidor-a", courierA.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void repartidorWithoutValidAssociationCannotReadInternalStatus() throws Exception {
        for (String claim : List.of("", "not-a-uuid")) {
            mvc.perform(get("/api/v1/internal/couriers/{id}/status", courierA.getId())
                            .header("Authorization", bearer("repartidor-a", Map.of("courierId", claim))))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/internal/couriers/{id}/status", courierA.getId())
                        .header("Authorization", bearer("repartidor-a", Collections.singletonMap("courierId", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientAndAdminInternalPathsAreDeniedByDefault() throws Exception {
        mvc.perform(get("/api/v1/internal/couriers/{id}/status", courierA.getId())
                        .header("Authorization", bearer("cliente-a")))
                .andExpect(status().isForbidden());

        for (String path : List.of(
                "/api/v1/internal/couriers",
                "/api/v1/internal/couriers/",
                "/api/v1/internal/couriers/" + courierA.getId(),
                "/api/v1/internal/other",
                "/api/v1/not-declared")) {
            mvc.perform(get(path).header("Authorization", bearer("admin")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminGets404ForUnknownCourierAndCannotUseOtherMethodsInternally() throws Exception {
        mvc.perform(get("/api/v1/internal/couriers/{id}/status", UUID.randomUUID())
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(post("/api/v1/internal/couriers/{id}/status", courierA.getId())
                        .header("Authorization", bearer("admin")))
                .andExpect(status().isForbidden());
    }

    private String bearer(String identity) {
        return "Bearer " + token(identity);
    }

    private String bearer(String identity, Map<String, Object> overrides) {
        return "Bearer " + token(identity, overrides);
    }

    private String bearerForCourier(String identity, UUID courierId) {
        return bearer(identity, Map.of("courierId", courierId.toString()));
    }
}
