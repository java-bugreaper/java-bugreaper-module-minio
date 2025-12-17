package net.bugreaper.modules.minio;


import net.bugreaper.modules.minio.exceptions.MinioHelperException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testcontainers.MinioSetup;

import java.text.MessageFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class MinioExceptionsTests {

    private final Minio minio = MinioSetup.getInstance().getMinio();

    private static final String DEFAULT_BUCKET = "new-bucket-ex";
    private static final String TEST_FILE = "data/test_file_1.txt";


    @BeforeAll
    static void createDefaultBucket() {
        MinioSetup.getInstance().getMinio().createBucket(DEFAULT_BUCKET);
    }

    @BeforeEach
    void cleanBucket() {
        minio.cleanBucket(DEFAULT_BUCKET);
    }


    @Test
    void checkObjectExistsErrorTest() {
        var bucketName = "my-bucket-wrong";

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minio.objectExistsStatus(bucketName, "object.txt"));

        assertEquals(
                MessageFormat.format("Bucket <{0}> does not exist", bucketName),
                exception.getMessage(),
                "Error on check object in tot existing bucket");
    }

    @Test
    void createBucketErrorTest() {
        var bucket = "not_bucket";

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minio.createBucket(bucket));

        MatcherAssert.assertThat(
                "Error on check object in not existing bucket",
                exception.getMessage(),
                StringContains.containsString(String.format("bucket name '%s' does not follow Amazon S3 standards", bucket)));
    }


    @Test
    void seeBucketDirErrorTest() {
        var bucketDir = "bucket/folder";

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minio.seeBucketExists(bucketDir));

        MatcherAssert.assertThat(
                "Error on check object in tot existing bucket",
                exception.getMessage(),
                StringContains.containsString(String.format("bucket name '%s' does not follow Amazon S3 standards", bucketDir)));
    }

    @Test
    void createBucketWithFileAndDeleteErrorTest() {
        String bucket = "bucket-for-delete2";

        minio.createBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE);
        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minio.deleteEmptyBucket(bucket));

        assertEquals(
                String.format("Bucket <%s> not empty", bucket),
                exception.getMessage(),
                "Error on try delete not empty bucket");
    }

    @Test
    void deleteNotExistingBucketErrorTest() {
        String bucket = "not-exist-delete";

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minio.deleteEmptyBucket(bucket));

        assertEquals(
                String.format("Bucket <%s> does not exist", bucket),
                exception.getMessage(),
                "Error on try delete not empty bucket");
    }

    @Test
    void uploadToExistingBucketErrorTest() {
        String bucket = "not-exist-upload";

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minio.uploadFileToBucket(bucket, TEST_FILE));

        MatcherAssert.assertThat(
                "Error on wrong bucket to upload",
                exception.getMessage(),
                StringContains.containsString("Error occurred: The specified bucket does not exist"));
    }

}
