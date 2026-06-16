package com.ecole221.paiement.service.infrastructure.web.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int page,
        int size,
        int totalPages
) {
    public PageResponse(List<T> content, long totalElements, int page, int size) {
        this(content, totalElements, page, size,
                size > 0 ? (int) Math.ceil((double) totalElements / size) : 0);
    }
}
