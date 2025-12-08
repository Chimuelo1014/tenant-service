package com.sentinel.tenant_service.dto.request;

import com.sentinel.tenant_service.enums.TenantType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequest {

    @NotBlank(message = "Tenant name is required")
    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    private String name;

    @NotNull(message = "Tenant type is required")
    private TenantType type;

    // Business specific fields (obligatorios si type = BUSINESS)
    @Size(max = 255, message = "Business name must not exceed 255 characters")
    private String businessName;

    @Pattern(regexp = "^[0-9]{9}-[0-9]{1}$", message = "NIT must follow format XXX-XXXXXX-X")
    private String nit;

    // Owner info (solo para creación interna desde auth-service)
    private UUID ownerId;

    private String ownerEmail;

    // Plan inicial - Se asigna desde billing-service después del registro
    // No hay valor por defecto (FREE plan eliminado)
    private String plan;

    /**
     * Valida que los campos de negocio estén completos si es tipo BUSINESS.
     */
    public boolean isBusinessFieldsComplete() {
        if (type == TenantType.BUSINESS) {
            return businessName != null && !businessName.isBlank()
                    && nit != null && !nit.isBlank();
        }
        return true;
    }
}