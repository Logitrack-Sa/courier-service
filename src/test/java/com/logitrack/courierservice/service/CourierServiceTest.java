package com.logitrack.courierservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.logitrack.courierservice.dto.CourierRequest;
import com.logitrack.courierservice.dto.CourierResponse;
import com.logitrack.courierservice.entity.Courier;
import com.logitrack.courierservice.entity.CourierStatus;
import com.logitrack.courierservice.exception.ResourceNotFoundException;
import com.logitrack.courierservice.mapper.CourierMapper;
import com.logitrack.courierservice.repository.CourierRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.logitrack.courierservice.service.implementation.CourierServiceImp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CourierServiceTest {

    @Mock
    private CourierRepository courierRepository;

    @Spy
    private CourierMapper courierMapper = new CourierMapper();

    @InjectMocks
    private CourierServiceImp courierService;

    @Test
    void createReturnsCourierActiveByDefault() {
        when(courierRepository.save(any(Courier.class))).thenAnswer(inv -> inv.getArgument(0));

        CourierResponse response = courierService.create(new CourierRequest("Luis", "MOTO"));

        assertThat(response.name()).isEqualTo("Luis");
        assertThat(response.vehicleType()).isEqualTo("MOTO");
        assertThat(response.status()).isEqualTo(CourierStatus.ACTIVO);
    }

    @Test
    void findByIdThrowsWhenCourierDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(courierRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courierService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findAllWithoutStatusReturnsEveryCourier() {
        Pageable pageable = Pageable.ofSize(20);
        when(courierRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        courierService.findAll(null, pageable);

        verify(courierRepository, never()).findByStatus(any(), any());
    }

    @Test
    void findAllWithStatusFiltersByStatus() {
        Pageable pageable = Pageable.ofSize(20);
        when(courierRepository.findByStatus(CourierStatus.ACTIVO, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        courierService.findAll(CourierStatus.ACTIVO, pageable);

        verify(courierRepository).findByStatus(CourierStatus.ACTIVO, pageable);
    }

    @Test
    void changeStatusUpdatesCourierToInactive() {
        UUID id = UUID.randomUUID();
        Courier courier = new Courier("Luis", "MOTO");
        when(courierRepository.findById(id)).thenReturn(Optional.of(courier));

        CourierResponse response = courierService.changeStatus(id, CourierStatus.INACTIVO);

        assertThat(response.status()).isEqualTo(CourierStatus.INACTIVO);
        assertThat(courier.isAvailable()).isFalse();
    }

    @Test
    void changeStatusThrowsWhenCourierDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(courierRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courierService.changeStatus(id, CourierStatus.INACTIVO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAvailabilityReturnsTrueWhenCourierIsActive() {
        UUID id = UUID.randomUUID();
        Courier courier = new Courier("Luis", "MOTO"); // ACTIVO por defecto
        when(courierRepository.findById(id)).thenReturn(Optional.of(courier));

        var response = courierService.getAvailability(id);

        assertThat(response.status()).isEqualTo(CourierStatus.ACTIVO);
        assertThat(response.available()).isTrue();
    }

    @Test
    void getAvailabilityThrowsWhenCourierDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(courierRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courierService.getAvailability(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getInternalStatusReturnsStatusWhenCourierExists() {
        UUID id = UUID.randomUUID();
        Courier courier = new Courier("Luis", "MOTO");
        when(courierRepository.findById(id)).thenReturn(Optional.of(courier));

        var response = courierService.getInternalStatus(id);

        assertThat(response.status()).isEqualTo(CourierStatus.ACTIVO);
    }

    @Test
    void getInternalStatusThrowsWhenCourierDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(courierRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courierService.getInternalStatus(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
