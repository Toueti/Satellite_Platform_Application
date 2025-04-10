package com.enit.satellite_platform.shared.storage.storageImp;
// package com.enit.satellite_platform.shared.storage;

// import com.amazonaws.services.s3.AmazonS3;
// import com.amazonaws.services.s3.model.ObjectMetadata;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.springframework.web.multipart.MultipartFile;

// import java.io.IOException;
// import java.io.InputStream;
// import java.util.Map;
// import java.util.UUID;

// @Service("s3StorageService")
// public class S3StorageService implements StorageService {

//     @Autowired
//     private AmazonS3 s3Client;

//     @Value("${storage.s3.bucket}")
//     private String bucketName;

//     @Override
//     public String store(MultipartFile file, Map<String, Object> metadata) throws IOException {
//         String uniqueKey = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
//         ObjectMetadata s3Metadata = new ObjectMetadata();
//         s3Metadata.setContentLength(file.getSize());
//         if (metadata != null) {
//             metadata.forEach(s3Metadata::addUserMetadata);
//         }
//         s3Client.putObject(bucketName, uniqueKey, file.getInputStream(), s3Metadata);
//         return "s3://" + bucketName + "/" + uniqueKey; // Returns S3 URI
//     }

//     @Override
//     public InputStream retrieve(String identifier) throws IOException {
//         String key = identifier.replace("s3://" + bucketName + "/", "");
//         return s3Client.getObject(bucketName, key).getObjectContent();
//     }

//     @Override
//     public void delete(String identifier) throws IOException {
//         String key = identifier.replace("s3://" + bucketName + "/", "");
//         s3Client.deleteObject(bucketName, key);
//     }

//     @Override
//     public String getStorageType() {
//         return "s3";
//     }
// }