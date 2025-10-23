package io.bugreaper.modules.minio;

import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import testcontainers.MinioSetup;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class MinioAssertsCatchTests {

    private final Minio minio = MinioSetup.getInstance().getMinio();

    private static final String TEST_FILE = "data/test_file_1.txt";

    @Test
    void seeBucketExistsFailedTest() {
        var bucket = "not-exists-bucket";

        Throwable exception = assertThrows(AssertionFailedError.class, () ->
                minio.seeBucketExists(bucket));

        assertEquals(
                String.format("Bucket <%s> not exists", bucket),
                exception.getMessage(),
                "Error on check bucket exists fail");
    }

    @Test
    void seeBucketNotExistsFailedTest() {
        var bucket = "exists-bucket";

        minio.createBucket(bucket);

        Throwable exception = assertThrows(AssertionFailedError.class, () ->
                minio.seeBucketNotExists(bucket));

        assertEquals(
                String.format("Bucket <%s> exists", bucket),
                exception.getMessage(),
                "Error on check bucket not exists fail");
    }

    @Test
    void seObjectExistsFailedTest() {
        var bucket = "for-object";
        var object = "object_test_n";
        minio.createBucket(bucket);

        Throwable exception = assertThrows(AssertionFailedError.class, () ->
                minio.seeObjectExists(bucket, object));

        assertEquals(
                String.format("Object <%s> not exists in bucket <%s>", object, bucket),
                exception.getMessage(),
                "Error on check object exists fail");
    }

    @Test
    void seeObjectNotExistsFailedTest() {
        var bucket = "for-object";
        var object = "object_test_y";

        minio.createBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, object);


        Throwable exception = assertThrows(AssertionFailedError.class, () ->
                minio.seeObjectNotExists(bucket, object));

        assertEquals(
                String.format("Object <%s> exists in bucket <%s>", object, bucket),
                exception.getMessage(),
                "Error on check object not exists fail");
    }


    @Test
    void objectCountAssertFailedTest() {
        var bucket = "count-bucket";
        minio.createBucket(bucket);

        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "test/object2.txt");

        Throwable exception = assertThrows(ConditionTimeoutException.class, () ->
                minio.assertCountObjectsInBucketExactly(bucket,3));

        MatcherAssert.assertThat(
                "Error on assert objects count in bucket",
                exception.getMessage(),
                StringContains.containsString("Count objects from bucket <count-bucket> expected be exactly <3> ==> expected: <3> but was: <2>"));
    }

    @Test
    void greaterLessFailedTest() {
        String bucket = "bucket-greater";
        minio.createBucket(bucket);
        minio.cleanBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "object2.txt");

        Throwable exception = assertThrows(ConditionTimeoutException.class, () ->
                minio.assertCountObjectsInBucketLess(bucket, 1));

        MatcherAssert.assertThat(
                "Error on assert objects count greater in bucket",
                exception.getMessage(),
                StringContains.containsString("Count objects from bucket <bucket-greater> expected be less <1> but was <2>"));
    }

    @Test
    void greaterCountFailedTest() {
        String bucket = "bucket-less";
        minio.createBucket(bucket);
        minio.cleanBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "object2.txt");

        Throwable exception = assertThrows(ConditionTimeoutException.class, () ->
                minio.assertCountObjectsInBucketGreater(bucket, 2));

        MatcherAssert.assertThat(
                "Error on assert objects count greater in bucket",
                exception.getMessage(),
                StringContains.containsString("Count objects from bucket <bucket-less> expected be greater <2> but was <2>"));
    }


}
