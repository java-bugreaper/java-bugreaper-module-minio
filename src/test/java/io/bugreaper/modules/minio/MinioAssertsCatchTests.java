package io.bugreaper.modules.minio;

import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import testcontainers.MinioSetup;


import static io.bugreaper.core.filereaders.ResourcesFileReader.createResourceFileWithSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class MinioAssertsCatchTests {

    private final Minio minio = MinioSetup.getInstance().getMinio();

    private static final String TEST_FILE = "data/test_file_1.txt";

    private static final String TEST_BUCKET = "bucket-test";

    @Test
    void seeBucketExistsFailedTest() {
        var bucket = "not-exists-bucket";

        Throwable exception = assertThrows(AssertionFailedError.class, () ->
                minio.seeBucketExists(bucket));

        assertEquals(
                String.format("Bucket <%s> does not exist", bucket),
                exception.getMessage(),
                "Error on check bucket exists fail");
    }

    @Test
    void seeBucketDoesNotExistFailedTest() {
        var bucket = "exists-bucket";

        minio.createBucket(bucket);

        Throwable exception = assertThrows(AssertionFailedError.class, () ->
                minio.seeBucketDoesNotExist(bucket));

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
                String.format("Object <%s> does not exist in bucket <%s>", object, bucket),
                exception.getMessage(),
                "Error on check object exists fail");
    }

    @Test
    void seeObjectDoesNotExistFailedTest() {
        var bucket = "for-object";
        var object = "object_test_y";

        minio.createBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, object);


        Throwable exception = assertThrows(AssertionFailedError.class, () ->
                minio.seeObjectDoesNotExist(bucket, object));

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
                minio.seeObjectsCountIsExactly(bucket,3));

        MatcherAssert.assertThat(
                "Error on assert objects count in bucket",
                exception.getMessage(),
                is("Count objects from bucket <count-bucket> expected to be EXACTLY <3> but got <2> within 2 seconds"));
    }

    @Test
    void greaterLessFailedTest() {
        String bucket = "bucket-less";
        minio.createBucket(bucket);
        minio.cleanBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "object2.txt");

        Throwable exception = assertThrows(ConditionTimeoutException.class, () ->
                minio.seeObjectsCountIsLessThan(bucket, 1));

        MatcherAssert.assertThat(
                "Error on assert objects count greater in bucket",
                exception.getMessage(),
                is("Count objects from bucket <bucket-less> expected to be LESS than <1> but got <2> within 2 seconds"));
    }

    @Test
    void greaterCountFailedTest() {
        String bucket = "bucket-greater";
        minio.createBucket(bucket);
        minio.cleanBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "object2.txt");

        Throwable exception = assertThrows(ConditionTimeoutException.class, () ->
                minio.seeObjectsCountIsGreaterThan(bucket, 2));

        MatcherAssert.assertThat(
                "Error on assert objects count greater in bucket",
                exception.getMessage(),
                is("Count objects from bucket <bucket-greater> expected to be GREATER than <2> but got <2> within 2 seconds"));
    }

    @Test
    void seeObjectSizeFailedTest() {
        String fileName = "temp/test_size.txt";
        String object = "object_size.txt";
        long expectedBytes = 180;

        minio.createBucket(TEST_BUCKET);

        createResourceFileWithSize(fileName, expectedBytes);

        minio.uploadFileToBucket(TEST_BUCKET, fileName, object);

        Throwable exception = assertThrows(AssertionFailedError.class, () ->
                minio.seeObjectSizeExactly(TEST_BUCKET, object, expectedBytes - 1 ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exception.getMessage(),
                StringContains.containsString("Object <object_size.txt> size from bucket <bucket-test> expected to be equal <179> bytes but got <180>"));

        Throwable exceptionLess = assertThrows(AssertionFailedError.class, () ->
                minio.seeObjectSizeIsLessThan(TEST_BUCKET, object, expectedBytes ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exceptionLess.getMessage(),
                StringContains.containsString("Object <object_size.txt> size from bucket <bucket-test> expected to be less <180> bytes but got <180>"));

        Throwable exceptionGreater = assertThrows(AssertionFailedError.class, () ->
                minio.seeObjectSizeIsGreaterThan(TEST_BUCKET, object, expectedBytes ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exceptionGreater.getMessage(),
                StringContains.containsString("Object <object_size.txt> size from bucket <bucket-test> expected to be greater <180> bytes but got <180>"));
    }


}
