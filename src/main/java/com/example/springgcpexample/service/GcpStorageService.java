package com.example.springgcpexample.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GcpStorageService {

    private final Storage storage;
    private final String bucketName;

    public GcpStorageService(Storage storage, @Value("${spring.cloud.gcp.storage.bucket}") String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }
    public List<String> listObjects() {
        List<String> objectNames = new ArrayList<>();
        for (Blob blob : storage.list(bucketName).iterateAll()) {
            objectNames.add(blob.getName());
        }
        return objectNames;
    }
    public byte[] downloadFile(String fileName) {
        Blob blob = storage.get(BlobId.of(bucketName, fileName));
        return blob != null ? blob.getContent() : null;
    }
}