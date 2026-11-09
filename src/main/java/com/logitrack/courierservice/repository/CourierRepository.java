package com.logitrack.courierservice.repository;

import com.logitrack.courierservice.entity.Courier;
import com.logitrack.courierservice.entity.CourierStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CourierRepository extends JpaRepository<Courier, UUID> {

    Page<Courier> findByStatus(CourierStatus status, Pageable pageable);

    boolean existsByDni(String dni);

    boolean existsByDniAndIdNot(String dni, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select courier from Courier courier where courier.id = :id")
    Optional<Courier> findByIdForUpdate(@Param("id") UUID id);
}
