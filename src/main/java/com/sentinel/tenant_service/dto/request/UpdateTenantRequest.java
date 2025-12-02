package com.sentinel.tenant_service.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantRequest {
    
    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    private String name;
    
    @Size(max = 255, message = "Business name must not exceed 255 characters")
    private String businessName;
    
    @Pattern(
        regexp = "^[0-9]{9}-[0-9]{1}$",
        message = "NIT must follow format XXX-XXXXXX-X"
    )
    private String nit;
}