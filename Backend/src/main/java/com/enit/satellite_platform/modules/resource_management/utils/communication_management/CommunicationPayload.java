package com.enit.satellite_platform.modules.resource_management.utils.communication_management;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommunicationPayload {
    private Map<String, Object> dataMap = new HashMap<>();

    public void addData(String key, Object data) {
        dataMap.put(key, data);
    }

    public Object getData(String key) {
        return dataMap.get(key);
    }

    public boolean hasData(String key) {
        return dataMap.containsKey(key);
    }

    public Map<String, Object> getAllData() {
        return new HashMap<>(dataMap);
    }

    // Helper methods for common data types
    public void addFile(String key, byte[] imageData) {
        dataMap.put(key, imageData);
    }

    public byte[] getFile(String key) {
        Object data = dataMap.get(key);
        return (data instanceof byte[]) ? (byte[]) data : null;
    }

    public void addText(String key, String text) {
        dataMap.put(key, text);
    }

    public String getText(String key) {
        Object data = dataMap.get(key);
        return (data instanceof String) ? (String) data : null;
    }

    // Add metadata about the payload
    public void setContentType(String key, String contentType) {
        dataMap.put(key + "_content_type", contentType);
    }

    public String getContentType(String key) {
        Object data = dataMap.get(key + "_content_type");
        return (data instanceof String) ? (String) data : null;
    }
}
