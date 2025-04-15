package com.enit.satellite_platform.modules.resource_management.utils.storage_management;

import java.io.Serializable;
import java.time.Instant;

public class TemporaryFileMetadata implements Serializable {

    private static final long serialVersionUID = 1L; // Good practice for Serializable classes

    private String fileId;
    private Instant creationTime;
    private Instant lastAccessTime;

    // Default constructor for serialization frameworks like Jackson (used by RedisTemplate)
    public TemporaryFileMetadata() {
    }

    public TemporaryFileMetadata(String fileId, Instant creationTime, Instant lastAccessTime) {
        this.fileId = fileId;
        this.creationTime = creationTime;
        this.lastAccessTime = lastAccessTime;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Instant creationTime) {
        this.creationTime = creationTime;
    }

    public Instant getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Instant lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    @Override
    public String toString() {
        return "TemporaryFileMetadata{" +
               "fileId='" + fileId + '\'' +
               ", creationTime=" + creationTime +
               ", lastAccessTime=" + lastAccessTime +
               '}';
    }
}
