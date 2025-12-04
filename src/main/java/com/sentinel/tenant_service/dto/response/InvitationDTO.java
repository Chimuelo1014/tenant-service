package com.sentinel.tenant_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDTO {
    
    private UUID id;
    private UUID tenantId;
    private String tenantName;
    private String invitedByEmail;
    private String invitedEmail;
    private String role;
    private String status;
    private String invitationToken;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
