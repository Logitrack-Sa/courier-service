package com.logitrack.courierservice.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourierRepositoryTest {

    @Mock
    private CourierRepository courierRepository;

    @Test
    void repositoryMockDebeEstarDisponible() {
        // Verificamos que Mockito creó el mock.
        org.junit.jupiter.api.Assertions.assertNotNull(courierRepository);
    }
}
