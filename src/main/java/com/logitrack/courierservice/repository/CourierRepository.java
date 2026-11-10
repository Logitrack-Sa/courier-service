package com.logitrack.courierservice.repository;

import com.logitrack.courierservice.entity.Courier;
import com.logitrack.courierservice.entity.CourierStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourierRepository extends JpaRepository<Courier, UUID> {

    @Query("""
            select c from Courier c where c.status = :status
            or (:status in (com.logitrack.courierservice.entity.CourierStatus.AVAILABLE,
                            com.logitrack.courierservice.entity.CourierStatus.ACTIVO)
                and c.status in (com.logitrack.courierservice.entity.CourierStatus.AVAILABLE,
                                 com.logitrack.courierservice.entity.CourierStatus.ACTIVO))
            or (:status in (com.logitrack.courierservice.entity.CourierStatus.INACTIVE,
                            com.logitrack.courierservice.entity.CourierStatus.INACTIVO)
                and c.status in (com.logitrack.courierservice.entity.CourierStatus.INACTIVE,
                                 com.logitrack.courierservice.entity.CourierStatus.INACTIVO))
            """)
    Page<Courier> findByStatus(CourierStatus status, Pageable pageable);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, UUID id);
}
