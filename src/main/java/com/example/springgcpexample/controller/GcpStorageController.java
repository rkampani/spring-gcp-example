package com.example.demo;

import com.example.springgcpexample.service.GcpStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/storage")
public class GcpStorageController {

    private final GcpStorageService storageService;

    public GcpStorageController(GcpStorageService storageService) {
        this.storageService = storageService;
    }
    @GetMapping("/objects")
    public ResponseEntity<List<String>> listObjects() {
        return ResponseEntity.ok(storageService.listObjects());
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) {
        byte[] fileContent = storageService.downloadFile(fileName);
        if (fileContent != null) {
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(fileContent);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }
}