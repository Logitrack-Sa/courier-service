package com.logitrack.courierservice.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de {@link PageImpl} compatible con deserialización JSON en Redis y clientes REST.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestPageImpl<T> extends PageImpl<T> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestPageImpl(
            @JsonProperty("content") List<T> content,
            @JsonProperty("number") int number,
            @JsonProperty("size") int size,
            @JsonProperty("totalElements") Long totalElements,
            @JsonProperty("pageable") JsonNode pageable,
            @JsonProperty("last") boolean last,
            @JsonProperty("totalPages") int totalPages,
            @JsonProperty("sort") JsonNode sort,
            @JsonProperty("first") boolean first,
            @JsonProperty("numberOfElements") Integer numberOfElements,
            @JsonProperty("empty") boolean empty
    ) {
        super(content != null ? content : new ArrayList<>(),
                PageRequest.of(number, size > 0 ? size : 20),
                totalElements != null ? totalElements : 0L);
    }

    public RestPageImpl(List<T> content, Pageable pageable, long total) {
        super(content != null ? content : new ArrayList<>(), pageable, total);
    }

    public RestPageImpl(List<T> content) {
        super(content != null ? content : new ArrayList<>());
    }

    public RestPageImpl() {
        super(new ArrayList<>());
    }
}
