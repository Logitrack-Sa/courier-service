package com.logitrack.courierservice.service;

import com.logitrack.courierservice.dto.CourierStatusResponse;
import com.logitrack.courierservice.entity.Courier;
import com.logitrack.courierservice.exception.ResourceNotFoundException;
import com.logitrack.courierservice.repository.CourierRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve la consulta interna mínima de estado de un repartidor.
 *
 * <p>Es una consulta de solo lectura y deliberadamente separada del CRUD para
 * no exponer el resto del agregado en la ruta interna.</p>
 */
@Service
@RequiredArgsConstructor
public class CourierStatusQueryService {

    private final CourierRepository courierRepository;

    /**
     * Obtiene únicamente el identificador y el estado operativo solicitados.
     *
     * @param courierId identificador del repartidor.
     * @return respuesta mínima con {@code courierId} y {@code status}.
     * @throws ResourceNotFoundException si el repartidor no existe.
     */
    @Transactional(readOnly = true)
    public CourierStatusResponse findStatus(UUID courierId) {
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Repartidor", courierId));
        return new CourierStatusResponse(courier.getId(), courier.getStatus());
    }
}
