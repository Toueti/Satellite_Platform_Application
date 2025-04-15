package com.enit.satellite_platform.shared.mapper;

import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public class ObjectIdMapper {

    @Named("stringToObjectId")
    public ObjectId stringToObjectId(String id) {
        return id != null ? new ObjectId(id) : null;
    }

    @Named("objectIdToString")
    public String objectIdToString(ObjectId id) {
        return id != null ? id.toString() : null;
    }
}
