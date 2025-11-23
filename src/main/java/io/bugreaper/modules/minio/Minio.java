package io.bugreaper.modules.minio;

import io.bugreaper.core.assertable.AssertableStringList;
import io.bugreaper.core.config.ConfigLoader;
import io.bugreaper.core.config.YamlUtils;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import io.qameta.allure.Step;
import io.bugreaper.modules.minio.exceptions.MinioHelperException;
import io.bugreaper.modules.minio.interfaces.MinioConfig;
import io.bugreaper.modules.minio.interfaces.MinioInt;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Assertions;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.bugreaper.core.allurereporter.AllureReporter.attachJson;
import static io.bugreaper.core.assertions.Asserts.*;
import static io.bugreaper.core.filereaders.pathfinder.ProjectPaths.getTestResourcesPath;
import static io.bugreaper.core.mappers.StringMappers.formatMilliseconds;
import static java.time.Duration.ofMillis;
import static io.bugreaper.core.allurereporter.AllureReporter.attachFromList;
import static io.bugreaper.core.filereaders.ResourcesFileReader.getResourceFileSize;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Class consists methods that operate with Minio
 *
 * <p><b>Buckets/Objects Interaction:</b>
 * {@link Minio#cleanBucket(String)},
 * {@link Minio#createBucket(String)},
 * {@link Minio#deleteEmptyBucket(String)},
 * {@link Minio#deleteFilledBucket(String)},
 * {@link Minio#deleteObjectFromBucket(String, String)},
 * {@link Minio#shareObjectInBucket(String, String)},
 *
 * <p><b>Asserts:</b>
 * {@link Minio#seeBucketExists(String)},
 * {@link Minio#seeBucketIsEmpty(String)},
 * {@link Minio#seeBucketIsNotEmpty(String)},
 * {@link Minio#seeBucketDoesNotExist(String)},
 * {@link Minio#seeObjectExists(String, String)},
 * {@link Minio#seeObjectDoesNotExist(String, String)},
 * {@link Minio#seeObjectSizeExactly(String, String, long)},
 * {@link Minio#seeObjectSizeIsGreaterThan(String, String, long)},
 * {@link Minio#seeObjectSizeIsLessThan(String, String, long)},
 * {@link Minio#seeObjectsCountIsExactly(String, int)},
 * {@link Minio#seeObjectsCountIsGreaterThan(String, int)},
 * {@link Minio#seeObjectsCountIsLessThan(String, int)}
 *
 * <p><b>Asserts:</b>
 * {@link Minio#uploadFileToBucket(String, String)}
 * {@link Minio#uploadFileToBucket(String, String, String)}
 * {@link Minio#uploadFileToBucket(String, String, String, String)}
 *
 * <p><b>Download data:</b>
 * {@link Minio#downloadObjectFromBucket(String, String, String)}
 *
 * <p><b>Get data:</b>
 * {@link Minio#getBucketsList()},
 * {@link Minio#getObjectsCountInBucket(String)},
 * {@link Minio#getObjectSize(String, String)},
 * {@link Minio#getObjectsList(String)},
 * {@link Minio#objectExistsStatus(String, String)},
 * {@link Minio#readObjectFromBucket(String, String)},
 *
 * <p> Await for some counts assert default: {@link Minio#awaitMs}, can be changed by: {@link Minio#withAwaitMs(int)}
 * <p> Buffer for file download default: {@link Minio#downloadBufferSize}, can be changed by: {@link Minio#withDownloadBufferSize(int)}
 * <p> Max read/download object size default: {@link Minio#maxDownloadObjectSize}, can be changed by: {@link Minio#withMaxDownloadObjectSize(int)}
 * <p> Max upload file size default: {@link Minio#maxUploadFileSize}, can be changed by: {@link Minio#withMaxUploadSize(int)}
 *
 * @author Oleksii Betin "ambu550"
 * @since 1.0.0
 */
@SuppressWarnings("squid:S5960")
public class Minio implements MinioInt, MinioConfig {

    private static final Logger logger = LoggerFactory.getLogger("bugreaper-module-minio");

    private String host;
    private int port;
    private String username;
    private String password;

    private MinioClient minioCl;

    private final String resPath = getTestResourcesPath();

    /**
     * default ms await in tests
     */
    private int awaitMs = 2000;

    /**
     * default buffer for file download in bytes
     */
    private int downloadBufferSize = 1024 * 10;

    /**
     * default max file size for upload
     */
    private int maxUploadFileSize = 1024 * 20;

    /**
     * default max object size for download
     */
    private int maxDownloadObjectSize = 1024 * 50;

    /**
     * This constructor initializes client for interaction with Minio
     *
     * @param host host of Minio ("http://your-minio-host")
     * @param port port of Minio
     * @param username admin username
     * @param password  admin password
     */
    public Minio(String host, int port, String username, String password) {
        this.minioCl = createConnect(host, port, username, password);
    }

    /**
     * Constructs a Minio client configuration.
     *
     * <p>Loads configuration values from a YAML file.</p>
     *
     * <p><b>Default file:</b> {@code bugreaper.yml}</p>
     * <p><b>Custom file:</b> using {@code -DbugreaperEnv=test} loads {@code bugreaper-test.yml}</p>
     *
     * <p><b>Required configuration keys:</b></p>
     * <ul>
     *     <li>{@code modules.minio.host}</li>
     *     <li>{@code modules.minio.port}</li>
     *     <li>{@code modules.minio.username}</li>
     *     <li>{@code modules.minio.password}</li>
     * </ul>
     *
     * <p><b>Optional configuration keys:</b></p>
     * <ul>
     *     <li>{@code modules.minio.await}</li>
     *     <li>{@code modules.minio.max-upload-file-size}</li>
     *     <li>{@code modules.minio.max-download-file-size}</li>
     * </ul>
     *
     * <p>Missing required keys will result in configuration errors.
     * Missing optional keys will fall back to predefined defaults.</p>
     */
    public Minio() {
        loadFromYaml();
    }

    private void loadFromYaml() {

        Map<String, Object> rawData = ConfigLoader.loadYaml();

        //required config fields
        this.host = YamlUtils.getStringValueByPath(rawData, "modules.minio.host");
        this.port = YamlUtils.getIntegerValueByPath(rawData, "modules.minio.port");
        this.username = YamlUtils.getStringValueByPath(rawData, "modules.minio.username");
        this.password = YamlUtils.getStringValueByPath(rawData, "modules.minio.password");

        this.minioCl = createConnect(host, port, username, password);

        //optional config fields
        Object awaitVal = YamlUtils.getValueByPath(rawData, "modules.minio.await", true);
        if (awaitVal instanceof Number number) {
            withAwaitMs(number.intValue());
        }
        Object maxUploadFileSizeVal = YamlUtils.getValueByPath(rawData, "modules.minio.max-upload-file-size", true);
        if (maxUploadFileSizeVal instanceof Number number) {
            withMaxUploadSize(number.intValue());
        }
        Object maxDownloadObjectSizeVal = YamlUtils.getValueByPath(rawData, "modules.minio.max-download-file-size", true);
        if (maxDownloadObjectSizeVal instanceof Number number) {
            withMaxDownloadObjectSize(number.intValue());
        }


    }

    private MinioClient createConnect(String host, int port, String username, String password) {
        return MinioClient.builder()
                .endpoint(host + ":" + port)
                .credentials(username, password)
                .build();
    }

    //setters

    public Minio withDownloadBufferSize(int downloadBufferSize) {
        if (downloadBufferSize < 1){
            throw new IllegalArgumentException("downloadBufferSize too small (can`t bee less 1)");
        }
        this.downloadBufferSize = downloadBufferSize;
        return this;
    }

    public Minio withAwaitMs(int awaitMs) {
        if (awaitMs < 200){
            throw new IllegalArgumentException("awaitMs too small (can`t bee less 200ms)");
        }
        this.awaitMs = awaitMs;
        return this;
    }

    public Minio withMaxUploadSize(int maxUploadSize) {
        if (maxUploadSize < 1){
            throw new IllegalArgumentException("maxUploadSize too small (can`t bee less 1)");
        }
        this.maxUploadFileSize = maxUploadSize;
        return this;
    }

    public Minio withMaxDownloadObjectSize(int maxDownloadObjectSize) {
        if (maxDownloadObjectSize < 1){
            throw new IllegalArgumentException("maxDownloadObjectSize too small (can`t bee less 1)");
        }
        this.maxDownloadObjectSize = maxDownloadObjectSize;
        return this;
    }

    //getters

    /**
     * Returns the configured await timeout in milliseconds.
     *
     * @return await timeout in milliseconds
     */
    public int getAwaitMs() { return awaitMs; }


    public String getConfigSummary() {
        String info = String.format("""
        %s:
            host=%s
            port=%d
            username=%s
            password=%s
            awaitMs=%d
            downloadBufferSize=%d
            maxUploadFileSize=%d
            maxDownloadObjectSize=%d%n""",
                this.getClass().getSimpleName(), host, port, username, password,
                awaitMs, downloadBufferSize, maxUploadFileSize, maxDownloadObjectSize);

        logger.info(info);
        return info;
    }

    // Upload

    @Step("(Minio) Upload file: {filePathName} to bucket:<{bucketName}>")
    public void uploadFileToBucket(String bucketName, String filePathName) {

        Path path = Paths.get(filePathName);
        String fileName = path.getFileName().toString();

        uploadFileToBucket(bucketName, filePathName, fileName);
    }

    @Step("(Minio) Upload file: {filePathName} to bucket:<{bucketName}> like object:<{objectName}>")
    public void uploadFileToBucket(String bucketName, String filePathName, String objectName) {
        uploadFileToBucket(bucketName, filePathName, objectName, "text/plain");
    }

    @Step("(Minio) Upload file: <{filePathName}> to object: <{objectName}> in bucket: <{bucketName}>")
    public void uploadFileToBucket(String bucketName, String filePathName, String objectName, String contentType) {

        String resourceFilePath = resPath + filePathName;

        if (getResourceFileSize(filePathName) > maxUploadFileSize) {
            throw new MinioHelperException(
                    MessageFormat.format("Upload aborted, file <{0}> size:<{1}> more maximum in config {2}bytes, can be changed by .withMaxUploadSize(int maxUploadSize)",
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

    @Step("(Minio) Share object: <{objectName}> from bucket: <{bucketName}>")
    public String shareObjectInBucket(String bucketName, String objectName) {
        try {

            String link = minioCl.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(7, TimeUnit.DAYS)
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

    @Step("(Minio) Download object: <{objectName}> from bucket: <{bucketName}> to file: <{filePathName}>")
    public void downloadObjectFromBucket(String bucketName, String objectName, String filePathName) {

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

    //bug new line in end!
    @Step("(Minio) Read object: <{objectName}> from bucket: <{bucketName}>")
    public String readObjectFromBucket(String bucketName, String objectName) {

        checkDownloadSize(bucketName, objectName);
        String content = readObjectFromBucketMethod(bucketName, objectName);

        attachJson(objectName + " content:", content);

        return content;
    }

    private String readObjectFromBucketMethod(String bucketName, String objectName) {

        try (InputStream stream = minioCl.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build())) {

            return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining(System.lineSeparator()));

        } catch (Exception e) {
            throw new MinioHelperException(e);
        }

    }

    private void checkDownloadSize(String bucketName, String objectName) {
        if (getObjectSize(bucketName, objectName) > maxDownloadObjectSize) {
            throw new MinioHelperException(MessageFormat.format("Download aborted, object <{0}> size:<{1}> more maximum in config {2}bytes, can be changed by .withMaxDownloadFileSize(int maxDownloadObjectSize)",
                    objectName, getObjectSize(bucketName, objectName), maxDownloadObjectSize));
        }
    }


    // Create/Delete


    @Step("(Minio) Create bucket: <{bucketName}>")
    public void createBucket(String bucketName) {

        try {
            minioCl.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            logger.info("Bucket <{}> successfully created", bucketName);
        } catch (ErrorResponseException e) {
            if (e.errorResponse().message().contains("already own it")) {
                logger.info("Bucket <{}> already exist", bucketName);
            } else {
                throw new MinioHelperException("Error occurred: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new MinioHelperException("Error: " + e.getMessage());
        }

    }

    @Step("(Minio) Delete empty: <{bucketName}>")
    public void deleteEmptyBucket(String bucketName) {

        try {
            minioCl.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
            logger.info("Bucket <{}> successfully deleted", bucketName);

        } catch (ErrorResponseException e) {
            if ("The bucket you tried to delete is not empty".equals(e.errorResponse().message())) {
                throw new MinioHelperException(String.format("Bucket <%s> not empty", bucketName), e);

            } else if ("The specified bucket does not exist".equals(e.errorResponse().message())) {
                throw new MinioHelperException(String.format("Bucket <%s> does not exist", bucketName), e);

            } else {
                throw new MinioHelperException(String.format("Error occurred while delete bucket <%s>", bucketName), e);
            }
        } catch (Exception e) {
            throw new MinioHelperException(String.format("Error occurred while delete bucket <%s>", bucketName), e);
        }

    }

    @Step("(Minio) Delete bucket: <{bucketName}>")
    public void deleteFilledBucket(String bucketName) {
        cleanBucket(bucketName);
        deleteEmptyBucket(bucketName);
    }

    @Step("(Minio) Delete object: <{objectName}> from bucket: <{bucketName}>")
    public void deleteObjectFromBucket(String bucketName, String objectName) {

        try {
            minioCl.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());

            logger.info("Object <{}> in <{}> successfully deleted", objectName, bucketName);
        } catch (Exception e) {
            throw new MinioHelperException(e);
        }

    }

    @Step("(Minio) Clean bucket: <{bucketName}>")
    public void cleanBucket(String bucketName) {

        Iterable<Result<Item>> results = getObjectsListMethod(bucketName);

        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                logger.debug("Object {} for delete", item.objectName());
                minioCl.removeObject(
                        RemoveObjectArgs.builder().bucket(bucketName).object(item.objectName()).build());
            }
            logger.info("Objects in <{}> successfully deleted", bucketName);
        } catch (Exception e) {
            throw new MinioHelperException(e);
        }

    }


    // Statuses

    //not need step
    public boolean objectExistsStatus(String bucketName, String objectName) {
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

            } else if ("The specified bucket does not exist".equals(e.errorResponse().message())) {
                throw new MinioHelperException("Bucket <" + bucketName + "> does not exist", e);

            } else {
                throw new MinioHelperException("Error occurred while checking object existence", e);
            }

        } catch (Exception e) {
            throw new MinioHelperException("Error occurred while checking object existence", e);
        }
    }

    @Step("(Minio) Grab objects list from bucket: <{bucketName}>")
    public AssertableStringList getObjectsList(String bucketName) {
        return new AssertableStringList(getObjectsList(bucketName, true));
    }

    //no report no step
    private ArrayList<String> getObjectsListNoReport(String bucketName) {
        return getObjectsList(bucketName, false);
    }

    //not need step
    private ArrayList<String> getObjectsList(String bucketName, boolean allure) {

        Iterable<Result<Item>> results = getObjectsListMethod(bucketName);

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
            attachFromList(String.format("Objects(%d) list from bucket(%s):", objectsList.size(), bucketName), objectsList);
        }

        return objectsList;
    }


    // Get info

    //not need step
    public long getObjectSize(String bucketName, String objectName) {

        long objectSize = getStatObjectResponse(bucketName, objectName).size();

        logger.debug("Object <{}> size is: {} bytes", objectName, objectSize);

        return objectSize;

    }

    @Step("(Minio) Show buckets list")
    public AssertableStringList getBucketsList() {

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
    public int getObjectsCountInBucket(String bucketName) {
        return getObjectsListNoReport(bucketName).size();
    }


    // Asserts

    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> is empty")
    public void seeBucketIsEmpty(String bucketName) {

        if (getObjectsCountInBucket(bucketName) != 0) {
            fail(String.format("Bucket <%s> is not empty", bucketName));
        }

    }

    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> is not empty")
    public void seeBucketIsNotEmpty(String bucketName) {

        if (getObjectsCountInBucket(bucketName) == 0) {
            fail(String.format("Bucket <%s> is empty", bucketName));
        }

    }

    @Override
    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> exists")
    public void seeBucketExists(String bucketName) {

        if (!bucketExistingStatus(bucketName)) {
            fail(String.format("Bucket <%s> does not exist", bucketName));
        }

    }

    @Override
    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> does not exist")
    public void seeBucketDoesNotExist(String bucketName) {

        if (bucketExistingStatus(bucketName)) {
            fail(String.format("Bucket <%s> exists", bucketName));
        }

    }

    @Override
    @Step("(Minio)[ASSERT] Object: <{objectName}> exists in bucket <{bucketName}>")
    public void seeObjectExists(String bucketName, String objectName) {

        if (!objectExistsStatus(bucketName, objectName)) {
            fail(String.format("Object <%s> does not exist in bucket <%s>", objectName, bucketName));
        }

    }

    @Override
    @Step("(Minio)[ASSERT] Object: <{objectName}> does not exist in bucket <{bucketName}>")
    public void seeObjectDoesNotExist(String bucketName, String objectName) {

        if (objectExistsStatus(bucketName, objectName)) {
            fail(String.format("Object <%s> exists in bucket <%s>", objectName, bucketName));
        }

    }

    @Override
    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> has exactly {expectedCount} objects")
    public void seeObjectsCountIsExactly(String bucketName, int expectedCount) {

        try {
            await().with()
                    .atMost(ofMillis(awaitMs)).untilAsserted(() ->
                            assertIntEquals(expectedCount, getObjectsListNoReport(bucketName).size()));
        }catch (ConditionTimeoutException e){
            throw new ConditionTimeoutException(
                    MessageFormat.format(
                            "Count objects from bucket <{0}> expected to be EXACTLY <{1}> but got <{2}> within {3}",
                            bucketName, expectedCount, getObjectsListNoReport(bucketName).size(), formatMilliseconds(awaitMs)));
        }

    }

    @Override
    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> has great then {minCount} objects")
    public void seeObjectsCountIsGreaterThan(String bucketName, int minCount) {

        try {
            await().with()
                    .atMost(ofMillis(awaitMs)).untilAsserted(() ->
                            assertGreaterThanExpected(minCount, getObjectsListNoReport(bucketName).size()));
        }catch (ConditionTimeoutException e){
            throw new ConditionTimeoutException(
                    MessageFormat.format(
                            "Count objects from bucket <{0}> expected to be GREATER than <{1}> but got <{2}> within {3}",
                            bucketName, minCount, getObjectsListNoReport(bucketName).size(), formatMilliseconds(awaitMs)));
        }
    }

    @Override
    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> has less then {maxCount} objects")
    public void seeObjectsCountIsLessThan(String bucketName, int maxCount) {

        try {
            await().with()
                    .atMost(ofMillis(awaitMs)).untilAsserted(() ->
                            assertLessThanExpected(maxCount, getObjectsListNoReport(bucketName).size()));
        }catch (ConditionTimeoutException e){
            throw new ConditionTimeoutException(
                    MessageFormat.format(
                            "Count objects from bucket <{0}> expected to be LESS than <{1}> but got <{2}> within {3}",
                            bucketName, maxCount, getObjectsListNoReport(bucketName).size(), formatMilliseconds(awaitMs)));
        }

    }


    @Override
    @Step("(Minio)[ASSERT] Object: <{objectName}> from bucket <{bucketName}> size exactly: {expectedSize}")
    public void seeObjectSizeExactly(String bucketName, String objectName, long expectedSize) {
        assertEquals(
                expectedSize,
                getObjectSize(bucketName, objectName),
                MessageFormat.format("Object <{0}> size from bucket <{1}> expected to be equal <{2}> bytes but got <{3}>",
                        objectName, bucketName, expectedSize, getObjectSize(bucketName, objectName)));
    }

    @Override
    @Step("(Minio)[ASSERT] Object: <{objectName}> from bucket <{bucketName}> size greater: {minSize}")
    public void seeObjectSizeIsGreaterThan(String bucketName, String objectName, long minSize) {
        Assertions.assertTrue(
                getObjectSize(bucketName, objectName) > minSize,
                MessageFormat.format("Object <{0}> size from bucket <{1}> expected to be greater <{2}> bytes but got <{3}>",
                        objectName, bucketName, minSize, getObjectSize(bucketName, objectName)));
    }

    @Override
    @Step("(Minio)[ASSERT] Object: <{objectName}> from bucket <{bucketName}> size less: {maxSize}")
    public void seeObjectSizeIsLessThan(String bucketName, String objectName, long maxSize) {
        Assertions.assertTrue(
                getObjectSize(bucketName, objectName) < maxSize,
                MessageFormat.format("Object <{0}> size from bucket <{1}> expected to be less <{2}> bytes but got <{3}>",
                        objectName, bucketName, maxSize, getObjectSize(bucketName, objectName)));
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

    private Iterable<Result<Item>> getObjectsListMethod(String bucketName) {
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
