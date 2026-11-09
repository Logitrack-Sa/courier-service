package com.logitrack.courierservice.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Mixin para vincular interfaces de paginación con {@link RestPageImpl} durante la deserialización de Redis.
 */
@JsonDeserialize(as = RestPageImpl.class)
public interface PageMixin {
}
