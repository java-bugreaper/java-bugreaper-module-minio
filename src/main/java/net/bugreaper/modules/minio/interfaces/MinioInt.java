package net.bugreaper.modules.minio.interfaces;

import net.bugreaper.core.exceptions.BaseUrlException;
import net.bugreaper.modules.minio.exceptions.MinioHelperException;
import net.bugreaper.core.assertable.AssertableStringList;


/**
 * Interface defines methods for facilitating helper interactions and assertions.
 * Validates that all required methods are implemented.
 */
public interface MinioInt {

    /**
     * Assert number of objects in bucket exactly as expected (with await)
     *
     * @param bucketName    bucket name
     * @param expectedCount expected count
     * @throws AssertionError       on assert fail
     * @throws MinioHelperException on other Minio errors
     */
    void seeObjectsCountIsExactly(String bucketName, int expectedCount);

    /**
     * Assert number of objects in bucket greater than minSize (with await)
     *
     * @param bucketName bucket name
     * @param minCount   minimum count
     * @throws AssertionError       on assert fail
     * @throws MinioHelperException on other Minio errors
     */
    void seeObjectsCountIsGreaterThan(String bucketName, int minCount);

    /**
     * Assert number of objects in bucket less than maxCount (with await)
     *
     * @param bucketName bucket name
     * @param maxCount   expected count
     * @throws AssertionError       on assert fail
     * @throws MinioHelperException on other Minio errors
     */
    void seeObjectsCountIsLessThan(String bucketName, int maxCount);

    /**
     * Delete all objects in bucket
     *
     * @param bucketName bucket name
     * @throws MinioHelperException if failed to clean
     */
    void cleanBucket(String bucketName);

    /**
     * Create bucket
     * <p> No error if bucket already exists (is ignored, the objects remain)
     *
     * @param bucketName bucket name
     * @throws MinioHelperException if failed to create
     */
    void createBucket(String bucketName);

    /**
     * Delete bucket (must be empty)
     *
     * @param bucketName bucket name
     * @throws MinioHelperException if bucket not empty or other error
     */
    void deleteEmptyBucket(String bucketName);

    /**
     * Delete bucket (empty or filled)
     * <p>clean bucket before delete
     *
     * @param bucketName bucket name
     * @throws MinioHelperException on Minio errors
     */
    void deleteFilledBucket(String bucketName);

    /**
     * Delete object in bucket
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @throws MinioHelperException on Minio errors
     */
    void deleteObjectFromBucket(String bucketName, String objectName);

    /**
     * Downloads a file from the given URL and saves it to the specified local file path <b>in test resources</b>.
     *
     * <p><i>You can check this file with core features</i></p>
     * <pre>
     * Minio minio = Minio.getInstance();
     * FileHelper fileHelper = new FileHelper();
     *
     * minio.downloadObjectBySharedLink("http://...". "minio/my-file.txt");
     * fileHelper.seeFileContainString("minio/my-file.txt", "Hello");
     * </pre>
     *
     * @param urlLink   the URL of the shared file to download
     * @param filePath file path (including file name) in test-resources (example: "minio/my-file.txt")
     * @throws BaseUrlException on download error
     */
    void downloadObjectBySharedLink(String urlLink, String filePath);

    /**
     * Download object to file in test resources
     *
     * <p><i>You can check this file with core features</i></p>
     * <pre>
     * Minio minio = Minio.getInstance();
     * FileHelper fileHelper = new FileHelper();
     *
     * minio.downloadObjectBySharedLink("...". "minio/my-file.txt");
     * fileHelper.seeFileContainString("minio/my-file.txt", "Hello")
     * </pre>
     * @param bucketName   bucket name
     * @param objectName   object path/name
     * @param filePathName file path (including file name) in test-resources (example: "minio/my-file.txt")
     * @throws MinioHelperException on Minio errors
     */
    void downloadObjectFromBucket(String bucketName, String objectName, String filePathName);

    /**
     * Return list of buckets
     *
     * @return {@link AssertableStringList}
     * <p> EXAMPLE:
     * <p>getBucketsList().seeListAnyEquals("bucket.txt")
     * <p><b>can be provided multiple asserts with list</b>
     * @throws MinioHelperException on Minio errors
     */
    AssertableStringList getBucketsList();

    /**
     * Return objects count in bucket
     *
     * @param bucketName bucket name
     * @return int with objects count
     * @throws MinioHelperException on Minio errors
     */
    int getObjectsCountInBucket(String bucketName);

    /**
     * Executes an HTTP GET request to the given URL and returns the response body as a string.
     *
     * @param urlLink the URL of the shared file to download
     * @return String with data from object
     */
    String getObjectsBySharedLink(String urlLink);

    /**
     * Return object size in bytes
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @return long with object size
     * @throws MinioHelperException on Minio errors
     */
    long getObjectSize(String bucketName, String objectName);

    /**
     * Return list of objects(with path) from bucket
     *
     * @param bucketName bucket name
     * @return {@link AssertableStringList}
     * <p> EXAMPLE:
     * <p>getObjectsList("my-bucket").seeListAnyEquals("object1.txt")
     * <p><b>can be provided multiple asserts with list</b>
     * @throws MinioHelperException on Minio errors
     */
    AssertableStringList getObjectsList(String bucketName);

    /**
     * Return exists status of object
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @return boolean true=exists, false=not exists
     * @throws MinioHelperException on other Minio errors
     */
    boolean objectExistsStatus(String bucketName, String objectName);

    /**
     * Read object from bucket
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @return String with data
     * @throws MinioHelperException on other Minio errors
     */
    String readObjectFromBucket(String bucketName, String objectName);

    /**
     * Assert that bucket exists
     * <p><b>with await</b>
     *
     * @param bucketName bucket name
     * @throws AssertionError       on assert fail
     * @throws MinioHelperException on other Minio errors
     */
    void seeBucketExists(String bucketName);

    /**
     * Assert that bucket is empty
     * <p><b>with await</b>
     *
     * @param bucketName bucket name
     * @throws AssertionError       on assert fail
     * @throws MinioHelperException on other Minio errors
     */
    void seeBucketIsEmpty(String bucketName);

    /**
     * Assert that bucket is not empty
     * <p><b>with await</b>
     *
     * @param bucketName bucket name
     * @throws AssertionError       on assert fail
     * @throws MinioHelperException on other Minio errors
     */
    void seeBucketIsNotEmpty(String bucketName);

    /**
     * Assert that bucket does not exist
     * <p><b>with await</b>
     *
     * @param bucketName bucket name
     * @throws AssertionError       on assert fail
     * @throws MinioHelperException on other Minio errors
     */
    void seeBucketDoesNotExist(String bucketName);

    /**
     * Assert that object exists in bucket
     * <p><b>with await</b>
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @throws AssertionError       on assert fail
     * @throws MinioHelperException on other Minio errors
     */
    void seeObjectExists(String bucketName, String objectName);

    /**
     * Assert that object does not exist (with await)
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @throws AssertionError       on assert fail
     * @throws MinioHelperException on other Minio errors
     */
    void seeObjectDoesNotExist(String bucketName, String objectName);

    /**
     * Assert size of object in bucket exactly as expected
     *
     * @param bucketName   bucket name
     * @param objectName   object path/name
     * @param expectedSize expected size in bytes
     * @throws AssertionError       on assert fail
     * @throws MinioHelperException on other Minio errors
     */
    void seeObjectSizeExactly(String bucketName, String objectName, long expectedSize);

    /**
     * Assert size of object in bucket greater than minSize
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @param minSize    minimum size in bytes
     * @throws AssertionError       on assert fail
     * @throws MinioHelperException on other Minio errors
     */
    void seeObjectSizeIsGreaterThan(String bucketName, String objectName, long minSize);

    /**
     * Assert size of objects in bucket less than maxSize
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @param maxSize    maximum size in bytes
     * @throws AssertionError       on assert fail
     * @throws MinioHelperException on other Minio errors
     */
    void seeObjectSizeIsLessThan(String bucketName, String objectName, long maxSize);

    /**
     * Share (for 12 hours) object and return url
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @return String with link for object download
     * @throws MinioHelperException on other Minio errors
     */
    String shareObjectInBucket(String bucketName, String objectName);

    /**
     * Upload object to bucket with same name as file
     *
     * @param bucketName bucket name
     * @param filePath   path to file in test resources
     * @throws MinioHelperException on Minio errors
     */
    void uploadFileToBucket(String bucketName, String filePath);

    /**
     * Upload object to bucket with custom name
     *
     * @param bucketName bucket name
     * @param filePath   path to file in test resources
     * @param objectName provide object path/name
     * @throws MinioHelperException on Minio errors
     */
    void uploadFileToBucket(String bucketName, String filePath, String objectName);

    /**
     * Upload object to bucket with custom name and type
     *
     * @param bucketName  bucket name
     * @param filePath    path to file in test resources
     * @param objectName  provide object path/name
     * @param contentType content type
     * @throws MinioHelperException on Minio errors
     */
    void uploadFileToBucket(String bucketName, String filePath, String objectName, String contentType);

}
