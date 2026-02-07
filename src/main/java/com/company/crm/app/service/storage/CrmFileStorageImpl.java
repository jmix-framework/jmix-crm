package com.company.crm.app.service.storage;

import io.jmix.core.FileRef;
import io.jmix.localfs.LocalFileStorage;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@Primary
public class CrmFileStorageImpl extends LocalFileStorage implements CrmFileStorage {

    @Override
    public void save(FileRef fileRef, InputStream inputStream) {
        saveStream(fileRef, inputStream);
    }

    @Override
    public String getStorageName() {
        return STORAGE_NAME;
    }
}
