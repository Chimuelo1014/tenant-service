package com.sentinel.tenant_service.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitValidationResponse {
    
    private boolean allowed;
    private int limit;
    private int current;
    private int remaining;
    private String message;
    private String upgradeSuggestion;

    public static LimitValidationResponse allowed(int limit, int current) {
        return LimitValidationResponse.builder()
                .allowed(true)
                .limit(limit)
                .current(current)
                .remaining(limit - current)
                .build();
    }

    public static LimitValidationResponse denied(int limit, int current, String message, String upgradeSuggestion) {
        return LimitValidationResponse.builder()
                .allowed(false)
                .limit(limit)
                .current(current)
                .remaining(0)
                .message(message)
                .upgradeSuggestion(upgradeSuggestion)
                .build();
    }
}