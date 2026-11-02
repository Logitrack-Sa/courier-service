package com.logitrack.courierservice.repository;

import com.logitrack.courierservice.entity.Courier;
import com.logitrack.courierservice.entity.CourierStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourierRepository extends JpaRepository<Courier, UUID> {

    Page<Courier> findByStatus(CourierStatus status, Pageable pageable);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, UUID id);
}
