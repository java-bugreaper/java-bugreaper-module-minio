package net.bugreaper.modules.minio.setup;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import net.bugreaper.core.assertable.AssertableStringList;
import net.bugreaper.modules.minio.exceptions.MinioHelperException;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
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
import static net.bugreaper.core.utils.AwaitUtils.awaitCustom;
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
     * default ms await in tests
     */
    protected volatile int awaitMs = 2000;

    /**
     * default buffer for file download in bytes
     */
    protected volatile int downloadBufferSize = 1024 * 10;

    /**
     * default max file size for upload
     */
    protected volatile int maxUploadFileSize = 1024 * 20;

    /**
     * default max object size for download
     */
    protected volatile int maxDownloadObjectSize = 1024 * 50;

    
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
                    MessageFormat.format("Upload aborted, file <{0}> size:<{1}> more maximum in config {2}bytes, can be changed by .setMaxUploadSize(int maxUploadSize)",
                            resourceFilePath, getResourceFileSize(filePathName), maxUploadFileSize));
        }

        try (FileInputStream fis = new FileInputStream(resourceFilePath)) {
            minioCl.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(fis, fis.available(), -1)
                            .contentType(contentType)
                            .build());

            logger.debug("Object '{}' uploaded successfully to bucket '{}'.", objectName, bucketName);

        } catch (MinioException e) {
            throw new MinioHelperException("Error occurred: " + e.getMessage() + "\nHTTP Trace: " + e.httpTrace());
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
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
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new MinioHelperException("Error: " + e.getMessage());
        }
    }

    // Download/Read

    protected void downloadObjectBySharedLinkMethod(String urlLink, String filePath){
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
            throw new MinioHelperException(MessageFormat.format("Download aborted, object <{0}> size:<{1}> more maximum in config {2}bytes, can be changed by .setMaxDownloadFileSize(int maxDownloadObjectSize)",
                    objectName, getObjectSizeMethod(bucketName, objectName), maxDownloadObjectSize));
        }
    }


    // Create/Delete

    protected void createBucketMethod(String bucketName) {

        try {
            minioCl.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            logger.info("Bucket <{}> successfully created", bucketName);
        } catch (ErrorResponseException e) {
            if ("BucketAlreadyOwnedByYou".equals(e.errorResponse().code())) {
                logger.info("Bucket <{}> already exist", bucketName);
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
            logger.info("Bucket <{}> successfully deleted", bucketName);

        } catch (ErrorResponseException e) {
            if ("BucketNotEmpty".equals(e.errorResponse().code())) {
                throw new MinioHelperException(String.format("Bucket <%s> not empty", bucketName), e);

            } else if ("NoSuchBucket".equals(e.errorResponse().code())) {
                throw new MinioHelperException(String.format("Bucket <%s> does not exist", bucketName), e);

            } else {
                throw new MinioHelperException(String.format("Error occurred while delete bucket <%s>", bucketName), e);
            }
        } catch (Exception e) {
            throw new MinioHelperException(String.format("Error occurred while delete bucket <%s>", bucketName), e);
        }

    }

    protected void deleteObjectFromBucketMethod(String bucketName, String objectName) {

        try {
            minioCl.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());

            logger.info("Object <{}> in <{}> successfully deleted", objectName, bucketName);
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
                logger.debug("Object {} for delete", item.objectName());
                minioCl.removeObject(
                        RemoveObjectArgs.builder().bucket(bucketName).object(item.objectName()).build());
                count++;
            }
            logger.info("Objects({}) in <{}> successfully deleted", count, bucketName);
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
                throw new MinioHelperException("Bucket <" + bucketName + "> does not exist", e);

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
            logger.info("Objects list in bucket <{}>:\n{}", bucketName, objectsList);
            attachFromList(String.format("Objects(%d) list from bucket(%s):", objectsList.size(), bucketName), objectsList);
        }

        return objectsList;
    }


    // Get info

    //not need step
    protected long getObjectSizeMethod(String bucketName, String objectName) {

        long objectSize = getStatObjectResponse(bucketName, objectName).size();

        logger.debug("Object <{}> size is: {} bytes", objectName, objectSize);

        return objectSize;

    }
    
    protected AssertableStringList getBucketsListMethod() {

        try {
            List<Bucket> bucketList = minioCl.listBuckets();


            ArrayList<String> actualList = new ArrayList<>();

            if (bucketList.isEmpty()) {
                logger.info("No buckets found.");
            } else {
                logger.info("Buckets in MinIO:");
                for (Bucket bucket : bucketList) {
                    actualList.add(bucket.name());
                    logger.info("Name: {}, Created: {}", bucket.name(), bucket.creationDate());
                }
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

        try {
            awaitCustom(providedAwait).untilAsserted(() ->
                    assertIntEquals(0, getObjectsCountInBucketMethod(bucketName)));
        } catch (ConditionTimeoutException e) {

            //allure report & info log
            getObjectsListArrayMethod(bucketName, true);

            throw new AssertionError(
                    MessageFormat.format(
                            "Bucket <{0}> expected to be empty but has {1} object/s within {2}",
                            bucketName, getObjectsCountInBucketMethod(bucketName), formatMilliseconds(providedAwait)));
        }

    }
    
    protected void seeBucketIsNotEmptyMethod(String bucketName, int providedAwait) {

        try {
            awaitCustom(providedAwait).untilAsserted(() ->
                    assertGreaterThanExpected(0, getObjectsCountInBucketMethod(bucketName)));
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    MessageFormat.format(
                            "Bucket <{0}> expected to be not empty but has no objects within {1}",
                            bucketName, formatMilliseconds(providedAwait)));
        }

    }
    
    protected void seeBucketExistsMethod(String bucketName, int providedAwait) {

        try {
            awaitCustom(providedAwait).untilAsserted(() ->
                    assertBooleans(true, bucketExistingStatus(bucketName)));
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    MessageFormat.format(
                            "Bucket <{0}> does not exist within {1}",
                            bucketName, formatMilliseconds(providedAwait)));
        }
    }
    
    protected void seeBucketDoesNotExistMethod(String bucketName, int providedAwait) {

        try {
            awaitCustom(providedAwait).untilAsserted(() ->
                    assertBooleans(false, bucketExistingStatus(bucketName)));
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    MessageFormat.format(
                            "Bucket <{0}> unexpected exists within {1}",
                            bucketName, formatMilliseconds(providedAwait)));
        }
    }
    
    protected void seeObjectExistsMethod(String bucketName, String objectName, int providedAwait) {

        try {
            awaitCustom(providedAwait).untilAsserted(() ->
                    assertBooleans(true, objectExistsStatusMethod(bucketName, objectName)));
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    MessageFormat.format(
                            "Object <{0}> does not exist in bucket <{1}> within {2}",
                            objectName, bucketName, formatMilliseconds(providedAwait)));
        }
    }
    
    protected void seeObjectDoesNotExistMethod(String bucketName, String objectName, int providedAwait) {

        try {
            awaitCustom(providedAwait).untilAsserted(() ->
                    assertBooleans(false, objectExistsStatusMethod(bucketName, objectName)));
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    MessageFormat.format(
                            "Object <{0}> exists in bucket <{1}> within {2}",
                            objectName, bucketName, formatMilliseconds(providedAwait)));
        }

    }
    
    protected void seeObjectsCountIsExactlyMethod(String bucketName, int expectedCount, int providedAwait) {

        try {
            awaitCustom(providedAwait).untilAsserted(() ->
                    assertIntEquals(expectedCount, getObjectsListArrayNoReportMethod(bucketName).size()));
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    MessageFormat.format(
                            "Count objects from bucket <{0}> expected to be EXACTLY <{1}> but got <{2}> within {3}",
                            bucketName, expectedCount, getObjectsListArrayNoReportMethod(bucketName).size(), formatMilliseconds(providedAwait)));
        }

    }
    
    protected void seeObjectsCountIsGreaterThanMethod(String bucketName, int minCount, int providedAwait) {

        try {
            awaitCustom(providedAwait).untilAsserted(() ->
                    assertGreaterThanExpected(minCount, getObjectsListArrayNoReportMethod(bucketName).size()));
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    MessageFormat.format(
                            "Count objects from bucket <{0}> expected to be GREATER than <{1}> but got <{2}> within {3}",
                            bucketName, minCount, getObjectsListArrayNoReportMethod(bucketName).size(), formatMilliseconds(providedAwait)));
        }
    }
    
    protected void seeObjectsCountIsLessThanMethod(String bucketName, int maxCount, int providedAwait) {

        try {
            awaitCustom(providedAwait).untilAsserted(() ->
                    assertLessThanExpected(maxCount, getObjectsListArrayNoReportMethod(bucketName).size()));
        } catch (ConditionTimeoutException e) {
            throw new AssertionError(
                    MessageFormat.format(
                            "Count objects from bucket <{0}> expected to be LESS than <{1}> but got <{2}> within {3}",
                            bucketName, maxCount, getObjectsListArrayNoReportMethod(bucketName).size(), formatMilliseconds(providedAwait)));
        }

    }
    
    protected void seeObjectSizeExactlyMethod(String bucketName, String objectName, long expectedSize) {
        assertEquals(
                expectedSize,
                getObjectSizeMethod(bucketName, objectName),
                MessageFormat.format("Object <{0}> size from bucket <{1}> expected to be equal <{2}> but got <{3}>",
                        objectName, bucketName, formatBytes(expectedSize), formatBytes(getObjectSizeMethod(bucketName, objectName))));
    }
    
    protected void seeObjectSizeIsGreaterThanMethod(String bucketName, String objectName, long minSize) {

        try {
            Assertions.assertTrue(
                    getObjectSizeMethod(bucketName, objectName) > minSize);
        } catch (AssertionFailedError e) {
            throw new AssertionError(MessageFormat.format("Object <{0}> size from bucket <{1}> expected to be greater <{2}> but got <{3}>",
                    objectName, bucketName, formatBytes(minSize), formatBytes(getObjectSizeMethod(bucketName, objectName))));
        }
    }
    
    protected void seeObjectSizeIsLessThanMethod(String bucketName, String objectName, long maxSize) {

        try {
            Assertions.assertTrue(
                    getObjectSizeMethod(bucketName, objectName) < maxSize);
        } catch (AssertionFailedError e) {
            throw new AssertionError(MessageFormat.format("Object <{0}> size from bucket <{1}> expected to be less <{2}> but got <{3}>",
                    objectName, bucketName, formatBytes(maxSize), formatBytes(getObjectSizeMethod(bucketName, objectName))));
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
