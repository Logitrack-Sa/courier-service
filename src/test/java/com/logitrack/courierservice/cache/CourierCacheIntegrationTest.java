package com.logitrack.courierservice.cache;

import com.logitrack.courierservice.config.RedisCacheConfig;
import com.logitrack.courierservice.dto.CourierRequest;
import com.logitrack.courierservice.dto.CourierResponse;
import com.logitrack.courierservice.dto.ShipmentStatusChangedEvent;
import com.logitrack.courierservice.entity.Courier;
import com.logitrack.courierservice.entity.CourierStatus;
import com.logitrack.courierservice.listener.ShipmentStatusListener;
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
    private ShipmentStatusListener shipmentStatusListener;

    @Autowired
    private CourierRepository courierRepository;

    @Autowired
    private CacheManager cacheManager;

    private Courier courier;

    @BeforeEach
    void setUp() {
        cleanAll();
        courier = new Courier("Repartidor Cache", "39999001", "MOTO");
        courier.changeStatus(CourierStatus.AVAILABLE);
        courier = courierRepository.saveAndFlush(courier);
    }

    @AfterEach
    void tearDown() {
        cleanAll();
    }

    private void cleanAll() {
        courierRepository.deleteAll();
        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void whenFindAllWithStatusAvailable_thenCachedInCouriersAvailable() {
        PageRequest pageable = PageRequest.of(0, 20);

        Page<CourierResponse> firstCall = courierService.findAll(CourierStatus.AVAILABLE, pageable);
        assertThat(firstCall).isNotEmpty();

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();

        // Segunda llamada: recupera de caché
        Page<CourierResponse> secondCall = courierService.findAll(CourierStatus.AVAILABLE, pageable);
        assertThat(secondCall.getContent()).isEqualTo(firstCall.getContent());
    }

    @Test
    void whenFindAllWithNonAvailableStatus_thenNotCached() {
        PageRequest pageable = PageRequest.of(0, 20);

        courierService.findAll(CourierStatus.BUSY, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();
        // Clave de status BUSY no debe existir en couriers_available
        String busyKey = "status:BUSY:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(busyKey)).isNull();
    }

    @Test
    void whenCreateCourier_thenCacheIsEvicted() {
        PageRequest pageable = PageRequest.of(0, 20);
        courierService.findAll(CourierStatus.AVAILABLE, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();

        // Al dar de alta un repartidor se desaloja la caché
        courierService.create(new CourierRequest("Nuevo Repartidor", "39999002", "AUTO"));

        String cacheKey = "status:AVAILABLE:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(cacheKey)).isNull();
    }

    @Test
    void whenChangeStatus_thenCacheIsEvicted() {
        PageRequest pageable = PageRequest.of(0, 20);
        courierService.findAll(CourierStatus.AVAILABLE, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();

        // Cambio manual de estado desaloja la caché
        courierService.changeStatus(courier.getId(), CourierStatus.BUSY);

        String cacheKey = "status:AVAILABLE:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(cacheKey)).isNull();
    }

    @Test
    void whenUpdateCourier_thenCacheIsEvicted() {
        PageRequest pageable = PageRequest.of(0, 20);
        courierService.findAll(CourierStatus.AVAILABLE, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();

        courierService.update(courier.getId(), new CourierRequest("Nombre Actualizado", "39999001", "UTILITARIO"));

        String cacheKey = "status:AVAILABLE:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(cacheKey)).isNull();
    }

    @Test
    void whenRabbitMqEnRutaEventReceived_thenCacheIsEvicted() {
        PageRequest pageable = PageRequest.of(0, 20);
        courierService.findAll(CourierStatus.AVAILABLE, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();

        ShipmentStatusChangedEvent event = new ShipmentStatusChangedEvent(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                "TRK-TEST-CACHE-1",
                courier.getId().toString(),
                "PENDIENTE",
                "EN_RUTA",
                Instant.now()
        );

        shipmentStatusListener.handleShipmentStatusChanged(event);

        String cacheKey = "status:AVAILABLE:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(cacheKey)).isNull();

        // Verificar que el repartidor quedó en BUSY
        assertThat(courierRepository.findById(courier.getId()).orElseThrow().getStatus())
                .isEqualTo(CourierStatus.BUSY);
    }

    @Test
    void whenRabbitMqEntregadoEventReceived_thenCacheIsEvicted() {
        courierService.changeStatus(courier.getId(), CourierStatus.BUSY);
        PageRequest pageable = PageRequest.of(0, 20);
        courierService.findAll(CourierStatus.AVAILABLE, pageable);

        Cache cache = cacheManager.getCache(RedisCacheConfig.COURIERS_AVAILABLE_CACHE);
        assertThat(cache).isNotNull();

        ShipmentStatusChangedEvent event = new ShipmentStatusChangedEvent(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                "TRK-TEST-CACHE-2",
                courier.getId().toString(),
                "EN_RUTA",
                "ENTREGADO",
                Instant.now()
        );

        shipmentStatusListener.handleShipmentStatusChanged(event);

        String cacheKey = "status:AVAILABLE:page:0:size:20:sort:UNSORTED";
        assertThat(cache.get(cacheKey)).isNull();

        assertThat(courierRepository.findById(courier.getId()).orElseThrow().getStatus())
                .isEqualTo(CourierStatus.AVAILABLE);
    }
}
