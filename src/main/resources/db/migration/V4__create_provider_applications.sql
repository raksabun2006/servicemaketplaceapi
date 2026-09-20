CREATE TABLE IF NOT EXISTS provider_applications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    business_name VARCHAR(150) NOT NULL,
    bio VARCHAR(2000),
    experience_years INTEGER NOT NULL,
    service_area VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    address VARCHAR(500),
    city VARCHAR(100),
    district VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    application_status VARCHAR(50) NOT NULL,
    identity_document_file_id UUID,
    profile_photo_file_id UUID,
    rejection_reason VARCHAR(1000),
    reviewed_by UUID,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_provider_applications_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_provider_applications_identity_doc
        FOREIGN KEY (identity_document_file_id)
        REFERENCES stored_files(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_provider_applications_profile_photo
        FOREIGN KEY (profile_photo_file_id)
        REFERENCES stored_files(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_provider_applications_reviewed_by
        FOREIGN KEY (reviewed_by)
        REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_provider_applications_user_id ON provider_applications(user_id);
CREATE INDEX IF NOT EXISTS idx_provider_applications_status ON provider_applications(application_status);
CREATE INDEX IF NOT EXISTS idx_provider_applications_created_at ON provider_applications(created_at);
