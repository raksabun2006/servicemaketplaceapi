package com.kh.serviceplatform.features.file;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String upload(MultipartFile file, String directory, String filename);

    void delete(String storageKey);

    Resource download(String storageKey);

    boolean exists(String storageKey);
}
