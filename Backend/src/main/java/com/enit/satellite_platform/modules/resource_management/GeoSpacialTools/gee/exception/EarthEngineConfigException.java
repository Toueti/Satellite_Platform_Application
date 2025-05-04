package com.enit.satellite_platform.modules.resource_management.GeoSpacialTools.gee.exception;

import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.ImageException;

public class EarthEngineConfigException extends ImageException {
    public EarthEngineConfigException(String message) {
        super(message);
    }

    public EarthEngineConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
