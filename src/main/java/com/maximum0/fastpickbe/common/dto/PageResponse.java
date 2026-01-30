package com.maximum0.fastpickbe.common.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import org.springframework.data.domain.Page;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public record PageResponse<T>(
        List<T> content,
        int size,
        int number,
        int numberOfElements,
        long totalElements,
        int totalPages,
        boolean last,
        boolean first
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getSize(),
                page.getNumber(),
                page.getNumberOfElements(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }
}
