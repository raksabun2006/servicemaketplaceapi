CREATE TABLE IF NOT EXISTS stored_files (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    original_filename VARCHAR(255),
    stored_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stored_files_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_stored_files_owner_id ON stored_files(owner_id);
CREATE INDEX IF NOT EXISTS idx_stored_files_file_type ON stored_files(file_type);
CREATE INDEX IF NOT EXISTS idx_stored_files_created_at ON stored_files(created_at);
