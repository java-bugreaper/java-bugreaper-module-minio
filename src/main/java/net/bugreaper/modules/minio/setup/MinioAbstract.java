package net.bugreaper.modules.minio.setup;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.Http.Method;
import io.minio.messages.ListAllMyBucketsResult;
import io.minio.messages.Item;
import net.bugreaper.core.assertable.AssertableStringList;
import net.bugreaper.modules.minio.exceptions.MinioHelperException;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.bugreaper.core.allurereporter.AllureReporter.attachFromList;
import static net.bugreaper.core.allurereporter.AllureReporter.attachJson;
import static net.bugreaper.core.assertions.Asserts.*;
import static net.bugreaper.core.filereaders.ResourcesFileReader.getResourceFileSize;
import static net.bugreaper.core.filereaders.pathfinder.ProjectPaths.getTestResourcesPath;
import static net.bugreaper.core.mappers.StringMappers.formatBytes;
import static net.bugreaper.core.mappers.StringMappers.formatMilliseconds;
import static net.bugreaper.core.url.BaseUrl.downloadFile;
import static net.bugreaper.core.url.BaseUrl.readBody;
import static net.bugreaper.core.utils.AwaitUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SuppressWarnings("squid:S5960")
public abstract class MinioAbstract {

    private static final Logger logger = LoggerFactory.getLogger("bugreaper-module-minio");

    protected String url;
    protected int port;
    protected String username;
    protected String password;

    private MinioClient minioCl;

    private final String resPath = getTestResourcesPath();

    /**
     * Default await timeout for tests, in milliseconds.
     */
    protected volatile int awaitMs = 2000;

    /**
     * Default await polling interval in milliseconds for tests.
     */
    protected volatile int awaitPollInterval = 100;

    /**
     * Default buffer size for file downloads, in bytes.
     */
    protected volatile int downloadBufferSize = 1024 * 10;

    /**
     * Default maximum file size for upload, in bytes.
     */
    protected volatile int maxUploadFileSize = 1024 * 20;

    /**
     * Default maximum object size for download, in bytes
     */
    protected volatile int maxDownloadObjectSize = 1024 * 50;

    private static final String SUBJECT = "objects";

    private static final String CONTAINER = "bucket";


    protected MinioAbstract(String url, int port, String username, String password) {
        this.url = url;
        this.port = port;
        this.username = username;
        this.password = password;

        createConnect(this.url, this.port, this.username, this.password);
        Runtime.getRuntime().addShutdownHook(createShutdownHook());
    }

    protected MinioAbstract() {
        Runtime.getRuntime().addShutdownHook(createShutdownHook());
    }

    Thread createShutdownHook() {
        return new Thread(() -> {
            try {
                if (minioCl != null) {
                    minioCl.close();
                    logger.debug("Minio client closed: {}", minioCl);
                }
            } catch (Exception e) {
                logger.warn("Failed to close Minio client: ", e);
            }
        }, "minio-client-shutdown");
    }

    protected void createConnect(String url, int port, String username, String password) {
        this.minioCl = MinioClient.builder()
                .endpoint(url + ":" + port)
                .credentials(username, password)
                .build();
    }


    // Upload

    protected void uploadFileToBucketMethod(String bucketName, String filePathName) {

        Path path = Paths.get(filePathName);
        String fileName = path.getFileName().toString();

        uploadFileToBucketMethod(bucketName, filePathName, fileName);
    }

    private void uploadFileToBucketMethod(String bucketName, String filePathName, String objectName) {
        uploadFileToBucketMethod(bucketName, filePathName, objectName, "text/plain");
    }

    protected void uploadFileToBucketMethod(String bucketName, String filePathName, String objectName, String contentType) {

        String resourceFilePath = resPath + filePathName;

        if (getResourceFileSize(filePathName) > maxUploadFileSize) {
            throw new MinioHelperException(
                    "Upload aborted: file '%s' size (%s bytes) exceeds the configured maximum (%s bytes). Use setMaxUploadSize to change the limit."
                            .formatted(resourceFilePath, getResourceFileSize(filePathName), maxUploadFileSize));
        }
        File file = new File(resourceFilePath);
        try (FileInputStream fis = new FileInputStream(resourceFilePath)) {

            minioCl.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(fis, file.length(), -1L)
                            .contentType(contentType)
                            .build());

            logger.debug("Object '{}' uploaded successfully to bucket '{}'.", objectName, bucketName);

        } catch (MinioException e) {
            throw new MinioHelperException("Error occurred: " + e.getMessage() + "\nHTTP Trace: " + e.httpTrace());
        } catch (IOException e) {
            throw new MinioHelperException("Error: " + e.getMessage());
        }
    }

    protected String shareObjectInBucketMethod(String bucketName, String objectName) {
        try {

            String link = minioCl.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(12, TimeUnit.HOURS)
                            .build());

            attachJson(objectName + " shared link:", link);

            return link;

        } catch (MinioException e) {
            throw new MinioHelperException("Error occurred: " + e.getMessage() + "\nHTTP Trace: " + e.httpTrace());
        }
    }

    // Download/Read

    protected void downloadObjectBySharedLinkMethod(String urlLink, String filePath) {
        downloadFile(urlLink, filePath);
    }

    protected void downloadObjectFromBucketMethod(String bucketName, String objectName, String filePathName) {

        checkDownloadSize(bucketName, objectName);

        try (InputStream stream = minioCl.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
             FileOutputStream fos = new FileOutputStream(resPath + filePathName)) {


            byte[] buffer = new byte[downloadBufferSize];
            int bytesRead;

            while ((bytesRead = stream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            logger.debug("Object '{}' retrieved successfully.", objectName);
        } catch (Exception e) {
            throw new MinioHelperException(e);
        }

    }

    protected String readObjectFromBucketMethod(String bucketName, String objectName) {

        checkDownloadSize(bucketName, objectName);
        String content = readObject(bucketName, objectName);

        attachJson(objectName + " content:", content);

        return content;
    }

    private String readObject(String bucketName, String objectName) {

        try (InputStream stream = minioCl.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build())) {

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new MinioHelperException(e);
        }

    }

    private void checkDownloadSize(String bucketName, String objectName) {
        if (getObjectSizeMethod(bucketName, objectName) > maxDownloadObjectSize) {
            throw new MinioHelperException(
                    "Download aborted: file '%s' size (%s bytes) exceeds the configured maximum (%s bytes). Use setMaxDownloadFileSize to change the limit."
                            .formatted(objectName, getObjectSizeMethod(bucketName, objectName), maxDownloadObjectSize));
        }
    }


    // Create/Delete

    protected void createBucketMethod(String bucketName) {

        try {
            minioCl.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            logger.info("Bucket '{}' successfully created", bucketName);
        } catch (ErrorResponseException e) {
            if ("BucketAlreadyOwnedByYou".equals(e.errorResponse().code())) {
                logger.info("Bucket '{}' already exists", bucketName);
            } else {
                throw new MinioHelperException("Error occurred: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new MinioHelperException("Error: " + e.getMessage());
        }

    }

    protected void deleteEmptyBucketMethod(String bucketName) {

        try {
            minioCl.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
            logger.info("Bucket '{}' successfully deleted", bucketName);

        } catch (ErrorResponseException e) {
            if ("BucketNotEmpty".equals(e.errorResponse().code())) {
                throw new MinioHelperException(String.format("Bucket '%s' not empty", bucketName), e);

            } else if ("NoSuchBucket".equals(e.errorResponse().code())) {
                throw new MinioHelperException(String.format("Bucket '%s' does not exist", bucketName), e);

            } else {
                throw new MinioHelperException(String.format("Error occurred while delete bucket '%s'", bucketName), e);
            }
        } catch (Exception e) {
            throw new MinioHelperException(String.format("Error occurred while delete bucket '%s'", bucketName), e);
        }

    }

    protected void deleteObjectFromBucketMethod(String bucketName, String objectName) {

        try {
            minioCl.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());

            logger.info("Object '{}' successfully deleted from bucket '{}'", objectName, bucketName);
        } catch (Exception e) {
            throw new MinioHelperException(e);
        }

    }

    protected void cleanBucketMethod(String bucketName) {

        Iterable<Result<Item>> results = getObjectsListItemsMethod(bucketName);

        try {
            int count = 0;
            for (Result<Item> result : results) {
                Item item = result.get();
                logger.debug("Object '{}' for delete", item.objectName());
                minioCl.removeObject(
                        RemoveObjectArgs.builder().bucket(bucketName).object(item.objectName()).build());
                count++;
            }
            logger.info("Successfully deleted {} objects from bucket '{}'", count, bucketName);
        } catch (Exception e) {
            throw new MinioHelperException(e);
        }

    }


    // Statuses

    //not need step
    protected boolean objectExistsStatusMethod(String bucketName, String objectName) {
        try {
            minioCl.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            return true;

        } catch (ErrorResponseException e) {

            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;

            } else if ("NoSuchBucket".equals(e.errorResponse().code())) {
                throw new MinioHelperException("Bucket '%s' does not exist".formatted(bucketName), e);

            } else {
                throw new MinioHelperException("Error occurred while checking object existence", e);
            }

        } catch (Exception e) {
            throw new MinioHelperException("Error occurred while checking object existence", e);
        }
    }

    protected AssertableStringList getObjectsListForAssertMethod(String bucketName) {
        return new AssertableStringList(getObjectsListArrayMethod(bucketName, true));
    }

    //no report no step
    private ArrayList<String> getObjectsListArrayNoReportMethod(String bucketName) {
        return getObjectsListArrayMethod(bucketName, false);
    }

    //not need step
    protected ArrayList<String> getObjectsListArrayMethod(String bucketName, boolean allure) {

        Iterable<Result<Item>> results = getObjectsListItemsMethod(bucketName);

        ArrayList<String> objectsList = new ArrayList<>();

        try {
            for (Result<Item> result : results) {
                Item item = result.get();

                objectsList.add(item.objectName());

            }
        } catch (Exception e) {
            throw new MinioHelperException(e);
        }

        if (allure) {
            logger.info("Objects list in bucket '{}':\n{}", bucketName, objectsList);
            attachFromList(String.format("Objects(%d) list from bucket '%s':", objectsList.size(), bucketName), objectsList);
        }

        return objectsList;
    }


    // Get info

    //not need step
    protected long getObjectSizeMethod(String bucketName, String objectName) {

        long objectSize = getStatObjectResponse(bucketName, objectName).size();

        logger.debug("Object '{}' size is: {} bytes", objectName, objectSize);

        return objectSize;

    }

    protected AssertableStringList getBucketsListMethod() {

        try {
            List<ListAllMyBucketsResult.Bucket> bucketList = minioCl.listBuckets();

            ArrayList<String> actualList = new ArrayList<>(
                    bucketList.stream()
                            .map(ListAllMyBucketsResult.Bucket::name)
                            .toList()
            );

            if (actualList.isEmpty()) {
                logger.info("No buckets found.");
            } else {
                logger.info("Buckets in MinIO:");
                bucketList.forEach(bucket ->
                        logger.info("Name: '{}', Created: {}", bucket.name(), bucket.creationDate())
                );
            }


            attachFromList(String.format("Buckets(%d) list:", actualList.size()), actualList);

            return new AssertableStringList(actualList);

        } catch (Exception e) {
            throw new MinioHelperException(e);
        }
    }

    //not need step
    protected int getObjectsCountInBucketMethod(String bucketName) {
        return getObjectsListArrayNoReportMethod(bucketName).size();
    }

    protected String getObjectsBySharedLinkMethod(String urlLink) {
        return readBody(urlLink);
    }

    // Asserts

    protected void seeBucketIsEmptyMethod(String bucketName, int providedAwait) {
        awaitIsEmpty(() -> getObjectsCountInBucketMethod(bucketName), providedAwait, awaitPollInterval, SUBJECT, CONTAINER, bucketName);
        //no allure report & info log
    }

    protected void seeBucketIsNotEmptyMethod(String bucketName, int providedAwait) {
        awaitIsNotEmpty(() -> getObjectsCountInBucketMethod(bucketName), providedAwait, awaitPollInterval, SUBJECT, CONTAINER, bucketName);
    }

    protected void seeBucketExistsMethod(String bucketName, int providedAwait) {

        try {
            awaitCustom(providedAwait, awaitPollInterval).untilAsserted(() ->
                    assertBooleans(true, bucketExistingStatus(bucketName)));
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    "Bucket '%s' does not exist within %s"
                            .formatted(bucketName, formatMilliseconds(providedAwait)));
        }
    }

    protected void seeBucketDoesNotExistMethod(String bucketName, int providedAwait) {

        try {
            awaitCustom(providedAwait, awaitPollInterval).untilAsserted(() ->
                    assertBooleans(false, bucketExistingStatus(bucketName)));
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    "Bucket '%s' unexpectedly exists within %s"
                            .formatted(bucketName, formatMilliseconds(providedAwait)));
        }
    }

    protected void seeObjectExistsMethod(String bucketName, String objectName, int providedAwait) {

        try {
            awaitCustom(providedAwait, awaitPollInterval).untilAsserted(() ->
                    assertBooleans(true, objectExistsStatusMethod(bucketName, objectName)));
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    "Object '%s' does not exist in bucket '%s' within %s"
                            .formatted(objectName, bucketName, formatMilliseconds(providedAwait)));
        }
    }

    protected void seeObjectDoesNotExistMethod(String bucketName, String objectName, int providedAwait) {

        try {
            awaitCustom(providedAwait, awaitPollInterval).untilAsserted(() ->
                    assertBooleans(false, objectExistsStatusMethod(bucketName, objectName)));
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    "Object '%s' exists in bucket '%s' within %s"
                            .formatted(objectName, bucketName, formatMilliseconds(providedAwait)));
        }

    }

    protected void seeObjectsCountIsExactlyMethod(String bucketName, int expectedCount, int providedAwait) {
        awaitEquals(expectedCount, () -> getObjectsCountInBucketMethod(bucketName), providedAwait, awaitPollInterval, SUBJECT, CONTAINER, bucketName);
    }

    protected void seeObjectsCountIsGreaterThanMethod(String bucketName, int minCount, int providedAwait) {
        awaitGraterThan(minCount, () -> getObjectsCountInBucketMethod(bucketName), providedAwait, awaitPollInterval, SUBJECT, CONTAINER, bucketName);
    }

    protected void seeObjectsCountIsLessThanMethod(String bucketName, int maxCount, int providedAwait) {
        awaitLessThan(maxCount, () -> getObjectsCountInBucketMethod(bucketName), providedAwait, awaitPollInterval, SUBJECT, CONTAINER, bucketName);
    }

    protected void seeObjectSizeExactlyMethod(String bucketName, String objectName, long expectedSize) {
        assertEquals(
                expectedSize,
                getObjectSizeMethod(bucketName, objectName),
                "Object '%s' size in bucket '%s' expected to be EXACTLY <%s>, but got <%s>"
                        .formatted(objectName, bucketName, formatBytes(expectedSize), formatBytes(getObjectSizeMethod(bucketName, objectName))));
    }

    protected void seeObjectSizeIsGreaterThanMethod(String bucketName, String objectName, long minSize) {

        try {
            Assertions.assertTrue(
                    getObjectSizeMethod(bucketName, objectName) > minSize);
        } catch (AssertionError e) {
            throw new AssertionError(
                    "Object '%s' size in bucket '%s' expected to be GREATER <%s>, but got <%s>"
                            .formatted(objectName, bucketName, formatBytes(minSize), formatBytes(getObjectSizeMethod(bucketName, objectName))));
        }
    }

    protected void seeObjectSizeIsLessThanMethod(String bucketName, String objectName, long maxSize) {

        try {
            Assertions.assertTrue(
                    getObjectSizeMethod(bucketName, objectName) < maxSize);
        } catch (AssertionError e) {
            throw new AssertionError(
                    "Object '%s' size in bucket '%s' expected to be LESS <%s>, but got <%s>"
                            .formatted(objectName, bucketName, formatBytes(maxSize), formatBytes(getObjectSizeMethod(bucketName, objectName))));
        }
    }

    // private sub-methods

    private StatObjectResponse getStatObjectResponse(String bucketName, String objectName) {
        try {
            return minioCl.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            throw new MinioHelperException(e);
        }
    }

    private Iterable<Result<Item>> getObjectsListItemsMethod(String bucketName) {
        return minioCl.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).recursive(true).build());
    }

    private boolean bucketExistingStatus(String bucketName) {

        try {
            return minioCl.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new MinioHelperException(e);
        }

    }

}
