package com.company.crm.app.service.storage;

import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;

import java.io.InputStream;

public interface CrmFileStorage extends FileStorage {

    String STORAGE_NAME = "crm";
    String IMAGES_FOLDER_PATH = "2026/01/01/";

    void save(FileRef fileRef, InputStream inputStream);
}
