package com.enit.satellite_platform.modules.user_management.normal_user_service.controllers;

import java.io.File;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/storage")
public class StorageController {

    private final String storagePath = "/storage/landsat_images";

    @Operation(summary = "Get storage usage information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Storage usage retrieved successfully")
    })
    @GetMapping("/usage")
    public ResponseEntity<String> getStorageUsage() {
        File directory = new File(storagePath);
        long size = getFolderSize(directory);
        return ResponseEntity.ok("Storage Used: " + size / (1024 * 1024) + " MB");
    }

    private long getFolderSize(File folder) {
        long length = 0;
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                length += file.length();
            } else {
                length += getFolderSize(file);
            }
        }
        return length;
    }
}
