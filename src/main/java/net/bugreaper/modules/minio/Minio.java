package net.bugreaper.modules.minio;

import io.qameta.allure.Step;
import net.bugreaper.modules.minio.interfaces.MinioConfig;
import net.bugreaper.modules.minio.interfaces.MinioInt;
import net.bugreaper.core.assertable.AssertableStringList;
import net.bugreaper.core.config.YamlUtils;
import net.bugreaper.modules.minio.setup.MinioAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class consists methods that operate with Minio
 *
 * <p>For one instance run recommended: {@code Minio minio = Minio.getInstance();}</p>
 *
 *
 * <p> Await for some asserts default: {@link Minio#awaitMs}, can be changed by: {@link Minio#setAwaitMs(int)}
 * <p> Buffer for file download default: {@link Minio#downloadBufferSize}, can be changed by: {@link Minio#setDownloadBufferSize(int)}
 * <p> Max read/download object size default: {@link Minio#maxDownloadObjectSize}, can be changed by: {@link Minio#setMaxDownloadObjectSize(int)}
 * <p> Max upload file size default: {@link Minio#maxUploadFileSize}, can be changed by: {@link Minio#setMaxUploadSize(int)}
 *
 * @author Oleksii Betin "ambu550"
 * @since 1.0.0
 */
@SuppressWarnings("squid:S5960")
public class Minio extends MinioAbstract implements MinioInt, MinioConfig {

    private static final Logger logger = LoggerFactory.getLogger("bugreaper-module-minio");

    private static Minio instance;

    /**
     * specific ms await in specific assert (configure with {@link #withAwaitMs(int)})
     */
    private final ThreadLocal<Integer> specificAwaitMs = ThreadLocal.withInitial(() -> 0);


    /**
     * This constructor initializes client for interaction with Minio
     *
     * @param url      host of Minio {@code  "http://your-minio-host"}
     * @param port     port of Minio
     * @param username admin username
     * @param password admin password
     */
    public Minio(String url, int port, String username, String password) {
        super(url, port, username, password);
    }

    /**
     * Returns the instance of {@link Minio} with config builder {@link #Minio()}.
     * <p>
     * This implementation is thread-safe using method-level synchronization.
     *
     * @return the singleton instance of {@link Minio}
     * @see #Minio() config setup
     */
    public static synchronized Minio getInstance() {
        if (instance == null) {
            instance = new Minio();
        }

        return instance;
    }

    /**
     * Constructs a Minio client configuration.
     *
     * <p>Loads configuration values from a YAML file.</p>
     *
     * <p><b>Default file:</b> {@code bugreaper.yml}</p>
     * <p><b>Custom file:</b> using {@code -DbugreaperEnv=test} loads {@code bugreaper-test.yml}</p>
     *
     * <pre>
     * modules:
     *   minio:
     *     url: http://localhost
     *     port: 29000
     *     username: admin
     *     password: password
     *     await: 300 # optional
     *     max-upload-file-size: 1024 # optional
     *     max-download-file-size: 2048 # optional
     * </pre>
     *
     * <p>Missing required keys will result in configuration errors.
     * Missing optional keys will fall back to predefined defaults.</p>
     */
    public Minio() {
        loadFromYaml();
        createConnect(this.url, this.port, this.username, this.password);
    }

    private void loadFromYaml() {

        //required config fields
        this.url = YamlUtils.getStringValueByPath("modules.minio.url");
        this.port = YamlUtils.getIntegerValueByPath("modules.minio.port");
        this.username = YamlUtils.getStringValueByPath("modules.minio.username");
        this.password = YamlUtils.getStringValueByPath("modules.minio.password");

        //optional config fields
        Object awaitVal = YamlUtils.getValueByPath("modules.minio.await", true);
        if (awaitVal instanceof Number number) {
            setAwaitMs(number.intValue());
        }
        Object maxUploadFileSizeVal = YamlUtils.getValueByPath("modules.minio.max-upload-file-size", true);
        if (maxUploadFileSizeVal instanceof Number number) {
            setMaxUploadSize(number.intValue());
        }
        Object maxDownloadObjectSizeVal = YamlUtils.getValueByPath("modules.minio.max-download-file-size", true);
        if (maxDownloadObjectSizeVal instanceof Number number) {
            setMaxDownloadObjectSize(number.intValue());
        }

    }

    //setters

    @Override
    public Minio setDownloadBufferSize(int downloadBufferSize) {
        if (downloadBufferSize < 1) {
            throw new IllegalArgumentException("downloadBufferSize too small (can`t bee less 1)");
        }
        this.downloadBufferSize = downloadBufferSize;
        return this;
    }

    @Override
    public Minio setAwaitMs(int awaitMs) {
        if (awaitMs < 200) {
            throw new IllegalArgumentException("awaitMs too small (can`t bee less 200ms)");
        }
        this.awaitMs = awaitMs;
        return this;
    }

    @Override
    public Minio setMaxUploadSize(int maxUploadSize) {
        if (maxUploadSize < 1) {
            throw new IllegalArgumentException("maxUploadSize too small (can`t bee less 1)");
        }
        this.maxUploadFileSize = maxUploadSize;
        return this;
    }

    @Override
    public Minio setMaxDownloadObjectSize(int maxDownloadObjectSize) {
        if (maxDownloadObjectSize < 1) {
            throw new IllegalArgumentException("maxDownloadObjectSize too small (can`t bee less 1)");
        }
        this.maxDownloadObjectSize = maxDownloadObjectSize;
        return this;
    }

    @Override
    public Minio withAwaitMs(int specificAwaitMs) {
        if (specificAwaitMs < 200) {
            throw new IllegalArgumentException("specificAwaitMs too small (can`t bee less 200ms)");
        }
        this.specificAwaitMs.set(specificAwaitMs);
        return this;
    }

    //getters

    @Override
    public String getConfigSummary() {
        String info = String.format("""
                        %s:
                            url=%s
                            port=%d
                            username=%s
                            password=%s
                            awaitMs=%d
                            downloadBufferSize=%d
                            maxUploadFileSize=%d
                            maxDownloadObjectSize=%d%n""",
                this.getClass().getSimpleName(), url, port, username, password,
                awaitMs, downloadBufferSize, maxUploadFileSize, maxDownloadObjectSize);

        logger.info(info);
        return info;
    }

    // Upload

    @Override
    @Step("(Minio) Upload file: {filePathName} to bucket: <{bucketName}>")
    public void uploadFileToBucket(String bucketName, String filePathName) {
        uploadFileToBucketMethod(bucketName, filePathName);
    }

    @Override
    @Step("(Minio) Upload file: {filePathName} to bucket: <{bucketName}> like object: <{objectName}>")
    public void uploadFileToBucket(String bucketName, String filePathName, String objectName) {
        uploadFileToBucketMethod(bucketName, filePathName, objectName, "text/plain");
    }

    @Override
    @Step("(Minio) Upload file: <{filePathName}> to object: <{objectName}> in bucket: <{bucketName}>")
    public void uploadFileToBucket(String bucketName, String filePathName, String objectName, String contentType) {
        uploadFileToBucketMethod(bucketName, filePathName, objectName, contentType);
    }

    @Override
    @Step("(Minio) Share object: <{objectName}> from bucket: <{bucketName}>")
    public String shareObjectInBucket(String bucketName, String objectName) {
       return shareObjectInBucketMethod(bucketName,objectName);
    }

    // Download/Read

    @Override
    @Step("(Minio) Download object by shared link")
    public void downloadObjectBySharedLink(String urlLink, String filePath) {
        downloadObjectBySharedLinkMethod(urlLink, filePath);
    }

    @Override
    @Step("(Minio) Download object: <{objectName}> from bucket: <{bucketName}> to file: <{filePathName}>")
    public void downloadObjectFromBucket(String bucketName, String objectName, String filePathName) {
        downloadObjectFromBucketMethod(bucketName, objectName, filePathName);
    }

    //bug new line in end!
    @Override
    @Step("(Minio) Read object: <{objectName}> from bucket: <{bucketName}>")
    public String readObjectFromBucket(String bucketName, String objectName) {
        return readObjectFromBucketMethod(bucketName, objectName);
    }


    // Create/Delete

    @Override
    @Step("(Minio) Create bucket: <{bucketName}>")
    public void createBucket(String bucketName) {
        createBucketMethod(bucketName);
    }

    @Override
    @Step("(Minio) Delete empty bucket: <{bucketName}>")
    public void deleteEmptyBucket(String bucketName) {

        deleteEmptyBucketMethod(bucketName);

    }

    @Override
    @Step("(Minio) Delete bucket: <{bucketName}>")
    public void deleteFilledBucket(String bucketName) {
        cleanBucket(bucketName);
        deleteEmptyBucket(bucketName);
    }

    @Override
    @Step("(Minio) Delete object: <{objectName}> from bucket: <{bucketName}>")
    public void deleteObjectFromBucket(String bucketName, String objectName) {
        deleteObjectFromBucketMethod(bucketName, objectName);
    }

    @Override
    @Step("(Minio) Clean bucket: <{bucketName}>")
    public void cleanBucket(String bucketName) {
        cleanBucketMethod(bucketName);
    }

    // Get

    //not need step
    @Override
    public boolean objectExistsStatus(String bucketName, String objectName) {
        return objectExistsStatusMethod(bucketName, objectName);
    }

    @Override
    @Step("(Minio) Grab objects list from bucket: <{bucketName}>")
    public AssertableStringList getObjectsList(String bucketName) {
        return getObjectsListForAssertMethod(bucketName);
    }

    //not need step
    @Override
    public long getObjectSize(String bucketName, String objectName) {
        return getObjectSizeMethod(bucketName, objectName);
    }

    @Override
    @Step("(Minio) Get buckets list")
    public AssertableStringList getBucketsList() {
        return getBucketsListMethod();
    }

    //not need step
    @Override
    public int getObjectsCountInBucket(String bucketName) {
        return getObjectsCountInBucketMethod(bucketName);
    }

    @Override
    @Step("(Minio) Get object by shared link")
    public String getObjectBySharedLink(String urlLink) {
        return getObjectsBySharedLinkMethod(urlLink);
    }


    // Asserts

    @Override
    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> is empty")
    public void seeBucketIsEmpty(String bucketName) {
        seeBucketIsEmptyMethod(bucketName, await());
    }

    @Override
    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> is not empty")
    public void seeBucketIsNotEmpty(String bucketName) {
        seeBucketIsNotEmptyMethod(bucketName, await());
    }

    @Override
    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> exists")
    public void seeBucketExists(String bucketName) {
        seeBucketExistsMethod(bucketName, await());
    }

    @Override
    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> does not exist")
    public void seeBucketDoesNotExist(String bucketName) {
        seeBucketDoesNotExistMethod(bucketName, await());
    }

    @Override
    @Step("(Minio)[ASSERT] Object: <{objectName}> exists in bucket <{bucketName}>")
    public void seeObjectExists(String bucketName, String objectName) {
        seeObjectExistsMethod(bucketName, objectName, await());
    }

    @Override
    @Step("(Minio)[ASSERT] Object: <{objectName}> does not exist in bucket <{bucketName}>")
    public void seeObjectDoesNotExist(String bucketName, String objectName) {
        seeObjectDoesNotExistMethod(bucketName, objectName, await());
    }

    @Override
    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> has exactly {expectedCount} objects")
    public void seeObjectsCountIsExactly(String bucketName, int expectedCount) {
        seeObjectsCountIsExactlyMethod(bucketName, expectedCount, await());
    }

    @Override
    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> has greater than {minCount} objects")
    public void seeObjectsCountIsGreaterThan(String bucketName, int minCount) {
        seeObjectsCountIsGreaterThanMethod(bucketName, minCount, await());
    }

    @Override
    @Step("(Minio)[ASSERT] Bucket: <{bucketName}> has less than {maxCount} objects")
    public void seeObjectsCountIsLessThan(String bucketName, int maxCount) {
        seeObjectsCountIsLessThanMethod(bucketName, maxCount, await());
    }


    @Override
    @Step("(Minio)[ASSERT] Object: <{objectName}> from bucket <{bucketName}> size exactly: {expectedSize} bytes")
    public void seeObjectSizeExactly(String bucketName, String objectName, long expectedSize) {
        seeObjectSizeExactlyMethod(bucketName, objectName, expectedSize);
    }

    @Override
    @Step("(Minio)[ASSERT] Object: <{objectName}> from bucket <{bucketName}> size greater: {minSize} bytes")
    public void seeObjectSizeIsGreaterThan(String bucketName, String objectName, long minSize) {
        seeObjectSizeIsGreaterThanMethod(bucketName, objectName, minSize);
    }

    @Override
    @Step("(Minio)[ASSERT] Object: <{objectName}> from bucket <{bucketName}> size less: {maxSize} bytes")
    public void seeObjectSizeIsLessThan(String bucketName, String objectName, long maxSize) {
        seeObjectSizeIsLessThanMethod(bucketName, objectName, maxSize);
    }

    // private sub-methods

    private int await() {
        if (specificAwaitMs.get() != 0) {
            int result = specificAwaitMs.get();
            specificAwaitMs.remove();
            return result;
        } else {
            return awaitMs;
        }
    }

}
