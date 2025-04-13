package com.enit.satellite_platform.modules.resource_management.utils.storage_management.storageImp;

import com.enit.satellite_platform.modules.resource_management.utils.storage_management.StorageService;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service("gridFsStorageService")
public class GridFsStorageService implements StorageService {

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Override
    public String store(MultipartFile file, Map<String, Object> metadata) throws IOException {
        GridFSUploadOptions options = new GridFSUploadOptions();
        if (metadata != null) {
            options.metadata(new org.bson.Document(metadata));
        }
        ObjectId fileId = gridFsTemplate.store(
            file.getInputStream(),
            file.getOriginalFilename(),
            file.getContentType(),
            options
        );
        return fileId.toString();
    }

    @Override
    public InputStream retrieve(String identifier) throws IOException {
        com.mongodb.client.gridfs.model.GridFSFile gridFSFile = gridFsTemplate.findOne(
            new org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("_id").is(new ObjectId(identifier))
            )
        );
        if (gridFSFile == null) {
            throw new IOException("File not found in GridFS with ID: " + identifier);
        }
        return gridFsTemplate.getResource(gridFSFile).getInputStream();
    }

    @Override
    public boolean delete(String identifier) throws IOException {
        try {
            gridFsTemplate.delete(
                new org.springframework.data.mongodb.core.query.Query(
                    org.springframework.data.mongodb.core.query.Criteria.where("_id").is(new ObjectId(identifier))
                )
            );
            return true;
        } catch (Exception e) {
            throw new IOException("Failed to delete file with ID: " + identifier, e);
        }
    }

    @Override
    public String getStorageType() {
        return "gridfs";
    }
}