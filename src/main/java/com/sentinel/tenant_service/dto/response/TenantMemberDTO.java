package com.sentinel.tenant_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMemberDTO {
    
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String role;
    private boolean isOwner;
    private LocalDateTime joinedAt;
}