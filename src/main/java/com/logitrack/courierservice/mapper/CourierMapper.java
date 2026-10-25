package com.logitrack.courierservice.mapper;

import com.logitrack.courierservice.dto.CourierRequest;
import com.logitrack.courierservice.dto.CourierResponse;
import com.logitrack.courierservice.entity.Courier;
import org.springframework.stereotype.Component;

@Component
public class CourierMapper {

    public Courier toEntity(CourierRequest request) {
        return new Courier(request.name(), request.vehicleType());
    }

    public CourierResponse toResponse(Courier courier) {
        return new CourierResponse(
                courier.getId(),
                courier.getName(),
                courier.getVehicleType(),
                courier.getStatus(),
                courier.getCreatedAt()
        );
    }
}
