package com.enit.satellite_platform.resources_management.models;

import lombok.Data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Document(collection = "gee_results")
public class GeeResults {

    @Id
    private ObjectId resultsId;

    @Field("data")
    private String data;

    @Field("date")
    private LocalDateTime date;

    @Field("type")
    private String type;

    @DBRef
    @Field("image")
    private Image image;

    protected void onCreate(){
        date = LocalDateTime.now();
    }
}
