package net.bugreaper.modules.minio;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import testcontainers.MinioContainerSetup;


import static net.bugreaper.core.filereaders.ResourcesFileReader.createResourceFileWithSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SuppressWarnings("java:S5778")
@Isolated
class MinioAssertsCatchTests extends MinioContainerSetup {

    private final Minio minio = getMinio();
    private final Minio minioFastAwait = getMinio().setAwaitMs(300);

    private static final String TEST_FILE = "data/test_file_1.txt";

    private static final String TEST_BUCKET = "bucket-test";

    @Test
    void seeBucketEmptyFailedTest() {
        var bucket = "not-empty";

        minioFastAwait.createBucket(bucket);
        minioFastAwait.cleanBucket(bucket);
        minioFastAwait.uploadFileToBucket(bucket, TEST_FILE, "test1");
        minioFastAwait.uploadFileToBucket(bucket, TEST_FILE, "test2");


        Throwable exception1 = assertThrows(AssertionError.class, () ->
                minioFastAwait.withAwaitMs(200).seeBucketIsEmpty(bucket));

        assertEquals(
                String.format("Expected bucket '%s' to be EMPTY, but got <2> objects within 200 milliseconds", bucket),
                exception1.getMessage());

        Throwable exception2 = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeBucketIsEmpty(bucket));

        assertEquals(
                String.format("Expected bucket '%s' to be EMPTY, but got <2> objects within 300 milliseconds", bucket),
                exception2.getMessage());
    }

    @Test
    void seeBucketNotEmptyFailedTest() {
        var bucket = "empty-bucket";

        minioFastAwait.createBucket(bucket);
        minioFastAwait.cleanBucket(bucket);

        Throwable exception1 = assertThrows(AssertionError.class, () ->
                minioFastAwait.withAwaitMs(210).seeBucketIsNotEmpty(bucket));

        assertEquals(
                String.format("Expected bucket '%s' to be NOT EMPTY, but got no objects within 210 milliseconds", bucket),
                exception1.getMessage());

        Throwable exception2 = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeBucketIsNotEmpty(bucket));

        assertEquals(
                String.format("Expected bucket '%s' to be NOT EMPTY, but got no objects within 300 milliseconds", bucket),
                exception2.getMessage());
    }

    @Test
    void seeBucketExistsFailedTest() {
        var bucketNotExists = "not-exists-bucket";

        Throwable exception1 = assertThrows(AssertionError.class, () ->
                minioFastAwait.withAwaitMs(220).seeBucketExists(bucketNotExists));

        assertEquals(
                String.format("Bucket '%s' does not exist within 220 milliseconds", bucketNotExists),
                exception1.getMessage(),
                "Error on check bucket exists fail");

        Throwable exception2 = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeBucketExists(bucketNotExists));

        assertEquals(
                String.format("Bucket '%s' does not exist within 300 milliseconds", bucketNotExists),
                exception2.getMessage(),
                "Error on check bucket exists fail");
    }

    @Test
    void seeBucketDoesNotExistFailedTest() {
        var bucket = "exists-bucket";

        minioFastAwait.createBucket(bucket);

        Throwable exception1 = assertThrows(AssertionError.class, () ->
                minioFastAwait.withAwaitMs(230).seeBucketDoesNotExist(bucket));

        assertEquals(
                String.format("Bucket '%s' unexpectedly exists within 230 milliseconds", bucket),
                exception1.getMessage(),
                "Error on check bucket not exists fail");

        Throwable exception2 = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeBucketDoesNotExist(bucket));

        assertEquals(
                String.format("Bucket '%s' unexpectedly exists within 300 milliseconds", bucket),
                exception2.getMessage(),
                "Error on check bucket not exists fail");
    }

    @Test
    void seObjectExistsFailedTest() {
        var bucket = "for-object";
        var object = "object_test_n";

        minioFastAwait.createBucket(bucket);
        minioFastAwait.cleanBucket(bucket);
        minioFastAwait.seeObjectDoesNotExist(bucket, object);

        Throwable exception1 = assertThrows(AssertionError.class, () ->
                minioFastAwait.withAwaitMs(210).seeObjectExists(bucket, object));

        assertEquals(
                String.format("Object '%s' does not exist in bucket '%s' within 210 milliseconds", object, bucket),
                exception1.getMessage(),
                "Error on check object exists fail");

        Throwable exception2 = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeObjectExists(bucket, object));

        assertEquals(
                String.format("Object '%s' does not exist in bucket '%s' within 300 milliseconds", object, bucket),
                exception2.getMessage(),
                "Error on check object exists fail");
    }

    @Test
    void seObjectExistsFailedOtherDirTest() {
        var bucket = "for-object2";
        var object = "object_test_n";

        minioFastAwait.createBucket(bucket);
        minioFastAwait.cleanBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, "dir/" + object);

        minioFastAwait.withAwaitMs(250).seeObjectDoesNotExist(bucket, object);

        Throwable exception1 = assertThrows(AssertionError.class, () ->
                minioFastAwait.withAwaitMs(350).seeObjectExists(bucket, object));

        assertEquals(
                String.format("Object '%s' does not exist in bucket '%s' within 350 milliseconds", object, bucket),
                exception1.getMessage(),
                "Error on check object exists fail");

        Throwable exception2 = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeObjectExists(bucket, object));

        assertEquals(
                String.format("Object '%s' does not exist in bucket '%s' within 300 milliseconds", object, bucket),
                exception2.getMessage(),
                "Error on check object exists fail");
    }

    @Test
    void specificAwaitTest() {
        var bucket = "for-with-await";
        var object = "object_test_dummy_a";

        minioFastAwait.createBucket(bucket);
        minioFastAwait.cleanBucket(bucket);

        Throwable exception = assertThrows(AssertionError.class, () ->
                minioFastAwait.withAwaitMs(400).seeObjectExists(bucket, object));

        assertEquals(
                String.format("Object '%s' does not exist in bucket '%s' within 400 milliseconds", object, bucket),
                exception.getMessage(),
                "await from specific global");

        minioFastAwait.cleanBucket(bucket);
        Throwable exception2 = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeObjectExists(bucket, object));

        assertEquals(
                String.format("Object '%s' does not exist in bucket '%s' within 300 milliseconds", object, bucket),
                exception2.getMessage(),
                "await rollback to global");
    }

    @Test
    void seObjectNotExistsFailedTest() {
        var bucket = "for-object";
        var object = "object_test_y";

        minio.createBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, object);

        Throwable exception1 = assertThrows(AssertionError.class, () ->
                minioFastAwait.withAwaitMs(200).seeObjectDoesNotExist(bucket, object));


        assertEquals(
                String.format("Object '%s' exists in bucket '%s' within 200 milliseconds", object, bucket),
                exception1.getMessage(),
                "Error on check object not exists fail");

        Throwable exception2 = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeObjectDoesNotExist(bucket, object));

        assertEquals(
                String.format("Object '%s' exists in bucket '%s' within 300 milliseconds", object, bucket),
                exception2.getMessage(),
                "Error on check object not exists fail");
    }


    @Test
    void objectCountAssertFailedTest() {
        var bucket = "count-bucket";
        minio.createBucket(bucket);

        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "test/object2.txt");

        Throwable exception1 = assertThrows(AssertionError.class, () ->
                minio.withAwaitMs(200).seeObjectsCountIsExactly(bucket,3));

        MatcherAssert.assertThat(
                "Error on assert objects count in bucket",
                exception1.getMessage(),
                is("Expected EXACTLY <3> objects in bucket 'count-bucket', but got <2> within 200 milliseconds"));

        Throwable exception2 = assertThrows(AssertionError.class, () ->
                minio.seeObjectsCountIsExactly(bucket,3));

        MatcherAssert.assertThat(
                "Error on assert objects count in bucket",
                exception2.getMessage(),
                is("Expected EXACTLY <3> objects in bucket 'count-bucket', but got <2> within 2 seconds"));
    }

    @Test
    void greaterLessFailedTest() {
        String bucket = "bucket-less";
        minio.createBucket(bucket);
        minio.cleanBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "object2.txt");


        Throwable exception1 = assertThrows(AssertionError.class, () ->
                minio.withAwaitMs(200).seeObjectsCountIsLessThan(bucket, 2));

        MatcherAssert.assertThat(
                exception1.getMessage(),
                is("Expected the number of objects in bucket 'bucket-less' to be LESS than <2>, but got <2> within 200 milliseconds"));

        Throwable exception2 = assertThrows(AssertionError.class, () ->
                minio.seeObjectsCountIsLessThan(bucket, 1));

        MatcherAssert.assertThat(
                exception2.getMessage(),
                is("Expected the number of objects in bucket 'bucket-less' to be LESS than <1>, but got <2> within 2 seconds"));
    }

    @Test
    void greaterCountFailedTest() {
        String bucket = "bucket-greater";
        minio.createBucket(bucket);
        minio.cleanBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "object2.txt");


        Throwable exception1 = assertThrows(AssertionError.class, () ->
                minio.withAwaitMs(200).seeObjectsCountIsGreaterThan(bucket, 3));

        MatcherAssert.assertThat(
                "Error on assert objects count greater in bucket",
                exception1.getMessage(),
                is("Expected the number of objects in bucket 'bucket-greater' to be GREATER than <3>, but got <2> within 200 milliseconds"));

        Throwable exception2 = assertThrows(AssertionError.class, () ->
                minio.seeObjectsCountIsGreaterThan(bucket, 2));

        MatcherAssert.assertThat(
                "Error on assert objects count greater in bucket",
                exception2.getMessage(),
                is("Expected the number of objects in bucket 'bucket-greater' to be GREATER than <2>, but got <2> within 2 seconds"));
    }

    @Test
    void seeObjectSizeFailedTest() {
        String fileName = "temp/test_size.txt";
        String object = "object_size.txt";
        long expectedBytes = 180;

        minio.createBucket(TEST_BUCKET);

        createResourceFileWithSize(fileName, expectedBytes);

        minio.uploadFileToBucket(TEST_BUCKET, fileName, object);

        Throwable exception = assertThrows(AssertionError.class, () ->
                minio.seeObjectSizeExactly(TEST_BUCKET, object, expectedBytes - 1 ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exception.getMessage(),
                StringContains.containsString("Object 'object_size.txt' size in bucket 'bucket-test' expected to be EXACTLY <179 bytes>, but got <180 bytes>"));

        Throwable exceptionLess = assertThrows(AssertionError.class, () ->
                minio.seeObjectSizeIsLessThan(TEST_BUCKET, object, expectedBytes ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exceptionLess.getMessage(),
                is("Object 'object_size.txt' size in bucket 'bucket-test' expected to be LESS <180 bytes>, but got <180 bytes>"));

        Throwable exceptionGreater = assertThrows(AssertionError.class, () ->
                minio.seeObjectSizeIsGreaterThan(TEST_BUCKET, object, 1024 ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exceptionGreater.getMessage(),
                is("Object 'object_size.txt' size in bucket 'bucket-test' expected to be GREATER <1Kb>, but got <180 bytes>"));
    }

    @Test
    void seeObjectSizeFailedV2Test() {
        String fileName = "temp/test_size2.txt";
        String object = "object_size2.txt";
        long expectedBytes = 180;

        minio.createBucket(TEST_BUCKET);

        createResourceFileWithSize(fileName, expectedBytes);

        minio.uploadFileToBucket(TEST_BUCKET, fileName, object);

        Throwable exception = assertThrows(AssertionError.class, () ->
                minio.seeObjectSizeExactly(TEST_BUCKET, object, expectedBytes + 1 ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exception.getMessage(),
                StringContains.containsString("Object 'object_size2.txt' size in bucket 'bucket-test' expected to be EXACTLY <181 bytes>, but got <180 bytes>"));

        Throwable exceptionLess = assertThrows(AssertionError.class, () ->
                minio.seeObjectSizeIsLessThan(TEST_BUCKET, object, 121 ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exceptionLess.getMessage(),
                is("Object 'object_size2.txt' size in bucket 'bucket-test' expected to be LESS <121 bytes>, but got <180 bytes>"));

        Throwable exceptionGreater = assertThrows(AssertionError.class, () ->
                minio.seeObjectSizeIsGreaterThan(TEST_BUCKET, object, 1024*1024+1 ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exceptionGreater.getMessage(),
                is("Object 'object_size2.txt' size in bucket 'bucket-test' expected to be GREATER <1Mb 1 byte>, but got <180 bytes>"));
    }

}
