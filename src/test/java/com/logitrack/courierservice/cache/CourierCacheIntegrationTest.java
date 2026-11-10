package com.logitrack.courierservice.cache;

import com.logitrack.courierservice.config.RedisCacheConfig;
import com.logitrack.courierservice.dto.CourierRequest;
import com.logitrack.courierservice.dto.CourierResponse;
import com.logitrack.courierservice.messaging.ShipmentStatusChangedEvent;
import com.logitrack.courierservice.messaging.ShipmentStatus;
import com.logitrack.courierservice.entity.Courier;
import com.logitrack.courierservice.entity.CourierStatus;
import com.logitrack.courierservice.messaging.ShipmentStatusEventListener;
import com.logitrack.courierservice.repository.CourierRepository;
import com.logitrack.courierservice.service.CourierService;
import com.logitrack.courierservice.support.JwtTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CourierCacheIntegrationTest extends JwtTestSupport {

    @Autowired
    private CourierService courierService;

    @Autowired
    private ShipmentStatusEventListener shipmentStatusListener;

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisCacheConfig redisCacheConfig;

    private Courier courier;

    @BeforeEach
    void setUp() {
        cleanAll();
        courier = new Courier("Repartidor Cache", "39999001", "MOTO");
        courier.changeStatus(CourierStatus.ACTIVO);
        courier = courierRepository.saveAndFlush(courier);
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        courierRepository.deleteAll();
        if (redisCacheConfig != null) {
            redisCacheConfig.resetDegradation();
        }
        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void whenFindAllWithStatusAvailable_thenCachedInCouriersAvailable() {
        PageRequest pageable = PageRequest.of(0, 20);

        Page<CourierResponse> firstCall = courierService.findAll(CourierStatus.ACTIVO, pageable);
        assertThat(firstCall).isNotEmpty();

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();

        // Segunda llamada: recupera de caché
        Page<CourierResponse> secondCall = courierService.findAll(CourierStatus.ACTIVO, pageable);
        assertThat(secondCall.getContent()).isEqualTo(firstCall.getContent());
    }

    @Test
    void whenFindAllWithNonAvailableStatus_thenNotCached() {
        PageRequest pageable = PageRequest.of(0, 20);

        courierService.findAll(CourierStatus.INACTIVO, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();
        // Clave de status INACTIVO no debe existir en couriers_available
        String busyKey = "status:INACTIVO:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(busyKey)).isNull();
    }

    @Test
    void whenCreateCourier_thenCacheIsEvicted() {
        PageRequest pageable = PageRequest.of(0, 20);
        courierService.findAll(CourierStatus.ACTIVO, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();

        // Al dar de alta un repartidor se desaloja la caché
        courierService.create(new CourierRequest("Nuevo Repartidor", "39999002", "AUTO"));

        String cacheKey = "status:ACTIVO:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(cacheKey)).isNull();
    }

    @Test
    void whenChangeStatus_thenCacheIsEvicted() {
        PageRequest pageable = PageRequest.of(0, 20);
        courierService.findAll(CourierStatus.ACTIVO, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();

        // Cambio manual de estado desaloja la caché
        courierService.changeStatus(courier.getId(), CourierStatus.INACTIVO);

        String cacheKey = "status:ACTIVO:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(cacheKey)).isNull();
    }

    @Test
    void whenUpdateCourier_thenCacheIsEvicted() {
        PageRequest pageable = PageRequest.of(0, 20);
        courierService.findAll(CourierStatus.ACTIVO, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();

        courierService.update(courier.getId(), new CourierRequest("Nombre Actualizado", "39999001", "UTILITARIO"));

        String cacheKey = "status:ACTIVO:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(cacheKey)).isNull();
    }

    @Test
    void whenRabbitMqEnRutaEventReceived_thenCacheIsEvicted() {
        PageRequest pageable = PageRequest.of(0, 20);
        courierService.findAll(CourierStatus.ACTIVO, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();

        ShipmentStatusChangedEvent event = new ShipmentStatusChangedEvent(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                "LT-CACHE1",
                courier.getId().toString(),
                ShipmentStatus.PENDIENTE,
                ShipmentStatus.EN_RUTA,
                Instant.now()
        );

        shipmentStatusListener.consume(event);

        String cacheKey = "status:ACTIVO:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(cacheKey)).isNull();

        // La ocupación no cambia la habilitación administrativa.
        assertThat(courierRepository.findById(courier.getId()).orElseThrow().getStatus())
                .isEqualTo(CourierStatus.ACTIVO);
        assertThat(courierRepository.findById(courier.getId()).orElseThrow().isAvailable()).isFalse();
    }

    @Test
    void whenRabbitMqEntregadoEventReceived_thenCacheIsEvicted() {
        courierService.changeStatus(courier.getId(), CourierStatus.INACTIVO);
        PageRequest pageable = PageRequest.of(0, 20);
        courierService.findAll(CourierStatus.ACTIVO, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();

        ShipmentStatusChangedEvent event = new ShipmentStatusChangedEvent(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                "LT-CACHE2",
                courier.getId().toString(),
                ShipmentStatus.EN_RUTA,
                ShipmentStatus.ENTREGADO,
                Instant.now()
        );

        shipmentStatusListener.consume(event);

        String cacheKey = "status:ACTIVO:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(cacheKey)).isNull();

        assertThat(courierRepository.findById(courier.getId()).orElseThrow().getStatus())
                .isEqualTo(CourierStatus.INACTIVO);
    }

    @Test
    void whenEvictionFails_thenCacheIsMarkedUnreliableAndBypassed() {
        redisCacheConfig.markEvictionFailed();
        assertThat(redisCacheConfig.isCacheReliable()).isFalse();

        PageRequest pageable = PageRequest.of(0, 20);
        courierService.findAll(CourierStatus.ACTIVO, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();
        String cacheKey = "status:ACTIVO:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(cacheKey)).isNull();
    }
}
