package com.enit.satellite_platform.resources_management.models;

import com.enit.satellite_platform.project_management.model.Project;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;

@Document(collection = "images")
@Data
public class Image {
    @Id
    private String imageId;

    @Indexed
    @Field("imageName")
    private String imageName;

    @Field("downloadUrl")
    private String downloadUrl;

    @Field("previewUrl")
    private String previewUrl;

    @Field("imagePath")
    private String imagePath;

    @Field("requestTime")
    private Date requestTime;

    @Field("updatedAt")
    private Date updatedAt;

    @DBRef(lazy = true)
    @Field("geeResults")
    private List<GeeResults> geeResults;

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
