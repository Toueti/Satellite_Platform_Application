package com.enit.satellite_platform.modules.resource_management.image_management.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.enit.satellite_platform.modules.resource_management.image_management.exceptions.ImageDownloadException;

import jakarta.annotation.PreDestroy;

@Service
@RefreshScope
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final ExecutorService executor;
    private final String imageStoragePath;

    public FileService(
            @Value("${thread.pool.size}") int threadPoolSize,
            @Value("${storage.filesystem.directory}") String imageStoragePath) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
        this.imageStoragePath = imageStoragePath;
    }

    /** Shutdown the thread pool when the application is shutting down */
    @PreDestroy
    public void cleanup() {
        executor.shutdown();
    }

    /**
     * Downloads an image from the given URL and saves it to the given output file
     * path.
     *
     * <p>
     * This method will retry the download up to three times if it fails due to a
     * timeout or an
     * exception. If it still fails after three retries, it will throw a {@link
     * ImageDownloadException}.
     *
     * @param downloadUrl    The URL of the image to download.
     * @param outputFilePath The path of the file to save the image to.
     * @throws IllegalArgumentException If either parameter is null.
     * @throws SecurityException        If the output file path is not within the
     *                                  allowed directory.
     * @throws ImageDownloadException   If the download fails after three retries.
     */
    @Async
    public CompletableFuture<String> downloadImage(String downloadUrl, String outputFilePath)
            throws ImageDownloadException {
        if (downloadUrl == null || outputFilePath == null) {
            throw new IllegalArgumentException("Download URL and output path cannot be null");
        }

        // Sanitize the filename
        String sanitizedFilename = sanitizeFilename(outputFilePath);

        // Security check: Ensure outputFilePath is within the allowed directory
        Path outputPath = Path.of(sanitizedFilename);

        try {
            if (!outputPath.toAbsolutePath().normalize()
                    .startsWith(Path.of(imageStoragePath).toAbsolutePath().normalize())) {
                throw new SecurityException("Invalid file path");
            }
        } catch (InvalidPathException e) {
            throw new SecurityException("Invalid file path", e);
        }

        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                downloadImageTask(downloadUrl, sanitizedFilename);
                return CompletableFuture.completedFuture(sanitizedFilename);

            } catch (Exception e) {
                logger.warn("❌ Download failed ({} retries left): {}", maxRetries - retryCount - 1, e.getMessage());
            }
            retryCount++;
            if (retryCount == maxRetries) {
                throw new ImageDownloadException("❌ Failed to download image after " + maxRetries + " retries");
            }
            performExponentialBackoff(retryCount);

        }
        return null; // Should never reach here, exception thrown above
    }

    /**
     * Downloads an image from the given URL and saves it to the given output file
     * path. This method
     * will also log a message indicating that the image has been downloaded
     * successfully.
     *
     * @param downloadUrl    The URL of the image to download.
     * @param outputFilePath The path of the file to save the image to.
     * @throws IOException            If the image download fails.
     * @throws ImageDownloadException If the image download fails after being
     *                                wrapped in an
     *                                IOException.
     */
    private void downloadImageTask(String downloadUrl, String outputFilePath) {
        HttpURLConnection connection = null;
        try {
            logger.info("📥 Downloading image from URL: {}", downloadUrl);
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            setupConnection(connection);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP " + responseCode);
            }

            Path outputPath = Path.of(outputFilePath);
            Files.createDirectories(outputPath.getParent());

            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("✅ Image downloaded successfully to: {}", outputFilePath);

        } catch (IOException e) {
            throw new ImageDownloadException("❌ Failed to download image", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Configures the provided HttpURLConnection with standard settings.
     *
     * <p>
     * Sets the request method to "GET" and applies default connection and read
     * timeouts.
     *
     * @param connection The HttpURLConnection to configure.
     * @throws IOException If an I/O error occurs when setting up the connection.
     */
    private void setupConnection(HttpURLConnection connection) throws IOException {
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
    }
        /**
         * Performs an exponential backoff before retrying a download operation.
         *
         * <p>
         * If the download operation fails due to a timeout or an exception, this method
         * will be
         * called to wait for a certain amount of time before retrying the download. The
         * amount of time
         * to wait is determined by the retry count, and follows an exponential backoff
         * strategy (i.e.
         * 1s, 2s, 4s, 8s, etc.).
         *
         * <p>
         * If the thread is interrupted while waiting, a RuntimeException will be
         * thrown.
         *
         * @param retryCount The number of times the download has been retried.
         * @throws RuntimeException If the download is interrupted during the backoff.
         */

    private void performExponentialBackoff(int retryCount) {
        try {
            long delay = (long) Math.pow(2, retryCount) * 1000;
            logger.info("⏳ Retrying in {} ms...", delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Download interrupted during backoff", e);
        }
    }

    /**
     * Creates a thumbnail of the given image file and returns the path of the
     * created thumbnail.
     * The thumbnail will be saved in the same directory as the original image, but
     * with a "_thumb"
     * suffix added to the file name.
     *
     * @param imagePath The path of the image file to create a thumbnail for.
     * @return The path of the created thumbnail.
     * @throws RuntimeException If the image file cannot be read.
     */
    public String createThumbnail(String imagePath) {
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            throw new RuntimeException("❌ Cannot read image: " + imagePath);
        }

        Mat thumbnail = new Mat();
        Size size = new Size(100, 100);
        Imgproc.resize(image, thumbnail, size);

        int lastDotIndex = imagePath.lastIndexOf(".");
        String thumbnailPath;
        if (lastDotIndex != -1) {
            thumbnailPath = imagePath.substring(0, lastDotIndex) + "_thumb" + imagePath.substring(lastDotIndex);
        } else {
            thumbnailPath = imagePath + "_thumb"; // Handle cases without an extension
        }
        Imgcodecs.imwrite(thumbnailPath, thumbnail);
        logger.info("🖼️ Thumbnail created: {}", thumbnailPath);

        return thumbnailPath;
    }

    /**
     * Deletes the image file at the given path. If the file does not exist, no
     * exception will be
     * thrown.
     *
     * @param imagePath The path of the image file to delete.
     * @throws IOException If an I/O error occurs when deleting the image file.
     */
    public void deleteImageFile(String imagePath) {
        try {
            Path path = Path.of(imagePath);
            Files.deleteIfExists(path);
            logger.info("🗑️ Deleted image file: {}", imagePath);
        } catch (IOException e) {
            logger.warn("⚠ Failed to delete image file: {}", imagePath, e);
        }
    }

    private String sanitizeFilename(String filename) {
        // Define a regular expression that matches any character that is NOT:
        // - a word character (alphanumeric and underscore)
        // - a period (.)
        // - a hyphen (-)
        String regex = "[^\\w.-]";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(filename);

        // Replace all matching characters with an underscore
        return matcher.replaceAll("_");
    }
}
