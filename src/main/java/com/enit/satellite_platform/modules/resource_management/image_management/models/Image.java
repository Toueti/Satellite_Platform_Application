package com.enit.satellite_platform.modules.resource_management.image_management.models;

import com.enit.satellite_platform.modules.project_management.model.Project;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Document(collection = "images")
@Data
public class Image {
    @Id
    private String imageId;

    @Indexed
    @Field("imageName")
    private String imageName;

    @Field("imageData")
    private byte[] imageData;

    @Field("requestTime")
    private Date requestTime;

    @Field("updatedAt")
    private Date updatedAt;

    @Field("mettadata")
    private Map<String, Object> mettadata;

    @DBRef(lazy = true)
    @Field("results")
    private List<ProcessingResults> results;

    @DBRef
    @Field("project")
    private Project project;

    protected void onCreate() {
        requestTime = new Date();
    }

    protected void onUpdate() {
        updatedAt = new Date();
    }

}
