package net.bugreaper.modules.minio.interfaces;

import net.bugreaper.modules.minio.exceptions.MinioHelperException;


/**
 * Interface defines methods for facilitating helper  assertions.
 * Validates that all required methods are implemented.
 */
public interface MinioAsserts {


    /**
     * Asserts that the bucket contains exactly the expected number of objects.
     * <p><b>Uses await.</b></p>
     *
     * @param bucketName    bucket name
     * @param expectedCount expected number of objects
     * @throws AssertionError       if the assertion fails
     * @throws MinioHelperException if a MinIO error occurs
     */
    void seeObjectsCountIsExactly(String bucketName, int expectedCount);

    /**
     * Asserts that the number of objects in bucket is greater than the given value.
     *
     * <p><b>Uses await.</b></p>
     *
     * @param bucketName bucket name
     * @param minCount   minimum object count
     * @throws AssertionError       if the assertion fails
     * @throws MinioHelperException if a MinIO error occurs
     */
    void seeObjectsCountIsGreaterThan(String bucketName, int minCount);

    /**
     * Asserts that the number of objects in bucket is less than the given value.
     *
     * <p><b>Uses await.</b></p>
     *
     * @param bucketName bucket name
     * @param maxCount   maximum object count
     * @throws AssertionError       if the assertion fails
     * @throws MinioHelperException if a MinIO error occurs
     */
    void seeObjectsCountIsLessThan(String bucketName, int maxCount);

    /**
     * Asserts that a bucket exists.
     *
     * <p><b>Uses await.</b></p>
     *
     * @param bucketName bucket name
     * @throws AssertionError       if the assertion fails
     * @throws MinioHelperException if a MinIO error occurs
     */
    void seeBucketExists(String bucketName);

    /**
     * Asserts that a bucket is empty
     *
     * <p><b>Uses await.</b></p>
     *
     * @param bucketName bucket name
     * @throws AssertionError       if the assertion fails
     * @throws MinioHelperException if a MinIO error occurs
     */
    void seeBucketIsEmpty(String bucketName);

    /**
     * Asserts that a bucket is not empty
     *
     * <p><b>Uses await.</b></p>
     *
     * @param bucketName bucket name
     * @throws AssertionError       if the assertion fails
     * @throws MinioHelperException if a MinIO error occurs
     */
    void seeBucketIsNotEmpty(String bucketName);

    /**
     * Asserts that a bucket does not exist
     *
     * <p><b>Uses await.</b></p>
     *
     * @param bucketName bucket name
     * @throws AssertionError       if the assertion fails
     * @throws MinioHelperException if a MinIO error occurs
     */
    void seeBucketDoesNotExist(String bucketName);

    /**
     * Asserts that an object exists in the bucket.
     *
     * <p><b>Uses await.</b></p>
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @throws AssertionError       if the assertion fails
     * @throws MinioHelperException if a MinIO error occurs
     */
    void seeObjectExists(String bucketName, String objectName);

    /**
     * Asserts that an object does not exist in the bucket.
     *
     * <p><b>Uses await.</b></p>
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @throws AssertionError       if the assertion fails
     * @throws MinioHelperException if a MinIO error occurs
     */
    void seeObjectDoesNotExist(String bucketName, String objectName);

    /**
     * Asserts that the object size in the bucket exactly matches the expected size.
     *
     * @param bucketName   bucket name
     * @param objectName   object path/name
     * @param expectedSize expected object size in bytes
     * @throws AssertionError       if the assertion fails
     * @throws MinioHelperException if a MinIO error occurs
     */
    void seeObjectSizeExactly(String bucketName, String objectName, long expectedSize);

    /**
     * Asserts that the object size in the bucket is greater than the specified minimum size.
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @param minSize    minimum object size in bytes
     * @throws AssertionError       if the assertion fails
     * @throws MinioHelperException if a MinIO error occurs
     */
    void seeObjectSizeIsGreaterThan(String bucketName, String objectName, long minSize);

    /**
     * Asserts that the object size in the bucket is less than the specified minimum size.
     *
     * @param bucketName bucket name
     * @param objectName object path/name
     * @param maxSize    maximum object size in bytes
     * @throws AssertionError       if the assertion fails
     * @throws MinioHelperException if a MinIO error occurs
     */
    void seeObjectSizeIsLessThan(String bucketName, String objectName, long maxSize);

}
