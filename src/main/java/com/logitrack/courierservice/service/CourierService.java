package com.logitrack.courierservice.service;

import com.logitrack.courierservice.dto.CourierRequest;
import com.logitrack.courierservice.dto.CourierResponse;
import com.logitrack.courierservice.entity.CourierStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourierService {


    CourierResponse update(UUID id, CourierRequest request);
    com.logitrack.courierservice.dto.CourierAvailabilityResponse getAvailability(UUID id);
    CourierResponse create(CourierRequest request);
    CourierResponse findById(UUID id);
    Page<CourierResponse> findAll(CourierStatus status, Pageable pageable);
    CourierResponse changeStatus(UUID id, CourierStatus newStatus);
}
