package net.bugreaper.modules.minio.interfaces;

import net.bugreaper.core.exceptions.BaseUrlException;
import net.bugreaper.modules.filehelper.FileHelper;
import net.bugreaper.modules.minio.exceptions.MinioHelperException;
import net.bugreaper.core.assertable.AssertableStringList;


/**
 * Interface defines methods for facilitating helper interactions.
 * Validates that all required methods are implemented.
 */
public interface MinioInt {


    /**
     * Deletes all objects from the bucket.
     *
     * @param bucketName bucket name
     * @throws MinioHelperException if deleting the objects fails
     */
    void cleanBucket(String bucketName);

    /**
     * Creates a bucket.
     *
     * <p>No error is thrown if the bucket already exists; existing objects are preserved.</p>
     *
     * @param bucketName bucket name
     * @throws MinioHelperException if bucket creation fails
     */
    void createBucket(String bucketName);

    /**
     * Deletes an empty bucket.
     *
     * <p>The bucket must be empty before deletion.</p>
     *
     * @param bucketName bucket name
     * @throws MinioHelperException if the bucket is not empty or another error occurs
     */
    void deleteEmptyBucket(String bucketName);

    /**
     * Deletes a bucket, whether empty or containing objects.
     *
     * <p>All objects are deleted before the bucket is removed.</p>
     *
     * @param bucketName bucket name
     * @throws MinioHelperException if a MinIO error occurs
     */
    void deleteFilledBucket(String bucketName);

    /**
     * Deletes an object from a bucket.
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @throws MinioHelperException if a MinIO error occurs
     */
    void deleteObjectFromBucket(String bucketName, String objectName);

    /**
     * Downloads a file from the specified URL and saves it to the given local file path
     * in test resources.
     *
     * <p><i>The downloaded file can be verified using {@link FileHelper} core features.</i></p>
     *
     * <pre>{@code
     * Minio minio = Minio.getInstance();
     * FileHelper fh = FileHelper.getInstance();
     *
     * minio.downloadObjectBySharedLink("http://...", "minio/my-file.txt");
     * fh.seeFileContainString("minio/my-file.txt", "Hello");
     * }</pre>
     *
     * @param urlLink  URL of the shared file to download
     * @param filePath local file path, including the file name, relative to test resources
     *                 (for example: {@code "minio/my-file.txt"})
     * @throws BaseUrlException if the file download fails
     */
    void downloadObjectBySharedLink(String urlLink, String filePath);

    /**
     * Downloads an object from a bucket and saves it to a file in test resources.
     *
     * <p><i>The downloaded file can be verified using {@link FileHelper} core features.</i></p>
     *
     * <pre>{@code
     * Minio minio = Minio.getInstance();
     * FileHelper fh = FileHelper.getInstance();
     *
     * minio.downloadObjectFromBucket("my-bucket", "my-file.txt", "minio/my-file.txt");
     * fh.seeFileContainString("minio/my-file.txt", "Hello");
     * }</pre>
     *
     * @param bucketName   bucket name
     * @param objectName   object path/name
     * @param filePathName local file path, including the file name, relative to test resources
     *                     (for example: {@code "minio/my-file.txt"})
     * @throws MinioHelperException if a MinIO error occurs
     */
    void downloadObjectFromBucket(String bucketName, String objectName, String filePathName);

    /**
     * Returns a list of bucket names.
     *
     * @return {@link AssertableStringList} containing bucket names
     *
     * <p>Example:
     * <pre>{@code
     * minio.getBucketsList()
     *  .seeListHasExactlyCount(2)
     *  .seeListAnyEquals("my-bucket");
     * }</pre>
     *
     * <p>Multiple assertions can be performed on the returned list.
     *
     * @throws MinioHelperException if a MinIO error occurs
     */
    AssertableStringList getBucketsList();

    /**
     * Returns the number of objects in a bucket.
     *
     * @param bucketName bucket name
     * @return number of objects in the bucket
     * @throws MinioHelperException if a MinIO error occurs
     */
    int getObjectsCountInBucket(String bucketName);

    /**
     * Executes an HTTP GET request to the specified URL and returns the response body as a string.
     *
     * @param urlLink URL of the shared object
     * @return response body as a string
     * @throws BaseUrlException if the request fails
     */
    String getObjectBySharedLink(String urlLink);

    /**
     * Returns the size of an object in bytes.
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @return object size in bytes
     * @throws MinioHelperException if a MinIO error occurs
     */
    long getObjectSize(String bucketName, String objectName);

    /**
     * Returns a list of object names, including their paths, from a bucket.
     *
     * @param bucketName bucket name
     * @return {@link AssertableStringList} containing object names and paths
     *
     * <p>Example:
     * <pre>{@code
     * minio.getObjectsList("my-bucket")
     *  .seeListHasExactlyCount(2)
     *  .seeListAnyEquals("object1.txt");
     * }</pre>
     *
     * <p>Multiple assertions can be performed on the returned list.
     *
     * @throws MinioHelperException if a MinIO error occurs
     */
    AssertableStringList getObjectsList(String bucketName);

    /**
     * Returns whether an object exists in a bucket.
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @return {@code true} if the object exists, {@code false} otherwise
     * @throws MinioHelperException if a MinIO error occurs
     */
    boolean objectExistsStatus(String bucketName, String objectName);

    /**
     * Reads an object from a bucket and returns its contents as a string.
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @return object contents as a string
     * @throws MinioHelperException if a MinIO error occurs
     */
    String readObjectFromBucket(String bucketName, String objectName);

    /**
     * Creates a shareable URL for an object that is valid for 12 hours.
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @return URL for downloading the object
     * @throws MinioHelperException if a MinIO error occurs
     */
    String shareObjectInBucket(String bucketName, String objectName);

    /**
     * Uploads a file to a bucket using the file name as the object name.
     *
     * @param bucketName bucket name
     * @param filePath   path to the file in test resources
     * @throws MinioHelperException if a MinIO error occurs
     */
    void uploadFileToBucket(String bucketName, String filePath);

    /**
     * Uploads a file to a bucket using a custom object name.
     *
     * @param bucketName bucket name
     * @param filePath   path to the file in test resources
     * @param objectName custom object path/name
     * @throws MinioHelperException if a MinIO error occurs
     */
    void uploadFileToBucket(String bucketName, String filePath, String objectName);

    /**
     * Uploads a file to a bucket using a custom object name and content type.
     *
     * @param bucketName  bucket name
     * @param filePath    path to the file in test resources
     * @param objectName  custom object path/name
     * @param contentType object content type
     * @throws MinioHelperException if a MinIO error occurs
     */
    void uploadFileToBucket(String bucketName, String filePath, String objectName, String contentType);

}
