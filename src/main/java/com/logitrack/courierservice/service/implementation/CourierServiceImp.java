package com.logitrack.courierservice.service.implementation;

import com.logitrack.courierservice.config.RedisCacheConfig;
import com.logitrack.courierservice.dto.CourierRequest;
import com.logitrack.courierservice.dto.CourierResponse;
import com.logitrack.courierservice.dto.RestPageImpl;
import com.logitrack.courierservice.entity.Courier;
import com.logitrack.courierservice.entity.CourierStatus;
import com.logitrack.courierservice.exception.BusinessRuleException;
import com.logitrack.courierservice.exception.ResourceNotFoundException;
import com.logitrack.courierservice.mapper.CourierMapper;
import com.logitrack.courierservice.repository.CourierRepository;
import com.logitrack.courierservice.service.CourierService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CourierServiceImp implements CourierService {

    private static final Logger log = LoggerFactory.getLogger(CourierService.class);

    private final CourierRepository courierRepository;
    private final CourierMapper courierMapper;

    @Transactional(readOnly = true)
    public com.logitrack.courierservice.dto.CourierAvailabilityResponse getAvailability(UUID id) {
        log.debug("Consultando disponibilidad para repartidor id={}", id);
        Courier courier = findEntity(id);
        boolean available = courier.isAvailable();
        return new com.logitrack.courierservice.dto.CourierAvailabilityResponse(
                courier.getId(),
                courier.getStatus(),
                available
        );
    }

    @Transactional
    @CacheEvict(value = RedisCacheConfig.COURIERS_AVAILABLE_CACHE, allEntries = true)
    public CourierResponse update(UUID id, CourierRequest request) {
        Courier courier = findEntity(id);
        if (courierRepository.existsByDniAndIdNot(request.dni(), id)) {
            throw new BusinessRuleException("DUPLICATE_DNI",
                    "Ya existe otro repartidor registrado con el DNI " + request.dni());
        }
        courier.updateInfo(request.name(), request.dni(), request.vehicleType());
        log.info("Repartidor actualizado id={}", id);
        return courierMapper.toResponse(courier);
    }

    @Transactional
    @CacheEvict(value = RedisCacheConfig.COURIERS_AVAILABLE_CACHE, allEntries = true)
    public CourierResponse create(CourierRequest request) {
        if (courierRepository.existsByDni(request.dni())) {
            throw new BusinessRuleException("DUPLICATE_DNI",
                    "Ya existe un repartidor registrado con el DNI " + request.dni());
        }
        Courier saved = courierRepository.save(courierMapper.toEntity(request));
        log.info("Repartidor registrado id={}", saved.getId());
        return courierMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CourierResponse findById(UUID id) {
        log.debug("Buscando repartidor id={}", id);
        return courierMapper.toResponse(findEntity(id));
    }

    /**
     * Lista repartidores. Si {@code status} es nulo, devuelve todos.
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = RedisCacheConfig.COURIERS_AVAILABLE_CACHE,
            key = "'status:' + #status + ':page:' + #pageable.pageNumber + ':size:' + #pageable.pageSize + ':sort:' + #pageable.sort",
            condition = "#status != null && #status.name() == 'ACTIVO' && @redisCacheConfig.isCacheReliable()"
    )
    public Page<CourierResponse> findAll(CourierStatus status, Pageable pageable) {
        log.debug("Listando repartidores status={} page={} size={}",
                status, pageable.getPageNumber(), pageable.getPageSize());

        Page<Courier> page = (status == null)
                ? courierRepository.findAll(pageable)
                : courierRepository.findByStatus(status, pageable);

        List<CourierResponse> content = page.getContent().stream()
                .map(courierMapper::toResponse)
                .toList();

        return new RestPageImpl<>(content, pageable, page.getTotalElements());
    }

    /**
     * Activa o desactiva un repartidor. Como la entidad está gestionada por la
     * transacción, el cambio se persiste al hacer commit (dirty checking).
     */
    @Transactional
    @CacheEvict(value = RedisCacheConfig.COURIERS_AVAILABLE_CACHE, allEntries = true)
    public CourierResponse changeStatus(UUID id, CourierStatus newStatus) {
        Courier courier = findEntity(id);
        CourierStatus previous = courier.getStatus();

        courier.changeStatus(newStatus);
        log.info("Estado de repartidor actualizado id={} {} -> {}", id, previous, newStatus);

        return courierMapper.toResponse(courier);
    }

    private Courier findEntity(UUID id) {
        return courierRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Repartidor no encontrado id={}", id);
                    return new ResourceNotFoundException("Repartidor", id);
                });
    }
}
