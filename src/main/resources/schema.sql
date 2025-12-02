CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL, -- PERSONAL | BUSINESS
    
    -- Owner
    owner_id UUID NOT NULL,
    owner_email VARCHAR(255) NOT NULL,
    
    -- Business specific
    business_name VARCHAR(255),
    nit VARCHAR(50),
    
    -- Plan & Limits
    plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    max_users INT NOT NULL DEFAULT 1,
    max_projects INT NOT NULL DEFAULT 1,
    max_domains INT NOT NULL DEFAULT 1,
    max_repos INT NOT NULL DEFAULT 0,
    blockchain_enabled BOOLEAN DEFAULT FALSE,
    
    -- Billing
    subscription_id UUID,
    next_billing_date TIMESTAMP,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_type CHECK (type IN ('PERSONAL', 'BUSINESS')),
    CONSTRAINT chk_plan CHECK (plan IN ('FREE', 'BASIC', 'PRO', 'ENTERPRISE')),
    CONSTRAINT chk_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_tenants_owner_id ON tenants(owner_id);
CREATE INDEX idx_tenants_slug ON tenants(slug);
CREATE INDEX idx_tenants_status ON tenants(status);