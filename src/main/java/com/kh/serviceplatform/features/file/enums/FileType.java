package com.kh.serviceplatform.features.file.enums;

public enum FileType {
    AVATAR("avatars"),
    PROVIDER_DOCUMENT("provider-documents"),
    SERVICE_IMAGE("service-images"),
    REQUEST_IMAGE("request-images"),
    PORTFOLIO_IMAGE("portfolio-images"),
    CATEGORY_ICON("category-icons"),
    OTHER("other");

    private final String directory;

    FileType(String directory) {
        this.directory = directory;
    }

    public String getDirectory() {
        return directory;
    }
}
