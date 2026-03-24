package net.bugreaper.modules.minio;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import testcontainers.MinioSetup;


import static net.bugreaper.core.filereaders.ResourcesFileReader.createResourceFileWithSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SuppressWarnings("java:S5778")
class MinioAssertsCatchTests {

    private final Minio minio = MinioSetup.getInstance().getMinio();
    private final Minio minioFastAwait = MinioSetup.getInstance().getMinio().setAwaitMs(300);

    private static final String TEST_FILE = "data/test_file_1.txt";

    private static final String TEST_BUCKET = "bucket-test";

    @Test
    void seeBucketEmptyFailedTest() {
        var bucket = "not-empty";

        minioFastAwait.createBucket(bucket);
        minioFastAwait.cleanBucket(bucket);
        minioFastAwait.uploadFileToBucket(bucket, TEST_FILE, "test1");
        minioFastAwait.uploadFileToBucket(bucket, TEST_FILE, "test2");

        Throwable exception = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeBucketIsEmpty(bucket));

        assertEquals(
                String.format("Bucket <%s> expected to be empty but has 2 object/s within 300 milliseconds", bucket),
                exception.getMessage());
    }

    @Test
    void seeBucketNotEmptyFailedTest() {
        var bucket = "empty-bucket";

        minioFastAwait.createBucket(bucket);
        minioFastAwait.cleanBucket(bucket);

        Throwable exception = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeBucketIsNotEmpty(bucket));

        assertEquals(
                String.format("Bucket <%s> expected to be not empty but has no objects within 300 milliseconds", bucket),
                exception.getMessage());
    }

    @Test
    void seeBucketExistsFailedTest() {
        var bucketNotExists = "not-exists-bucket";

        Throwable exception = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeBucketExists(bucketNotExists));

        assertEquals(
                String.format("Bucket <%s> does not exist within 300 milliseconds", bucketNotExists),
                exception.getMessage(),
                "Error on check bucket exists fail");
    }

    @Test
    void seeBucketDoesNotExistFailedTest() {
        var bucket = "exists-bucket";

        minioFastAwait.createBucket(bucket);

        Throwable exception = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeBucketDoesNotExist(bucket));

        assertEquals(
                String.format("Bucket <%s> unexpected exists within 300 milliseconds", bucket),
                exception.getMessage(),
                "Error on check bucket not exists fail");
    }

    @Test
    void seObjectExistsFailedTest() {
        var bucket = "for-object";
        var object = "object_test_n";

        minioFastAwait.createBucket(bucket);
        minioFastAwait.cleanBucket(bucket);
        minioFastAwait.seeObjectDoesNotExist(bucket, object);

        Throwable exception = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeObjectExists(bucket, object));

        assertEquals(
                String.format("Object <%s> does not exist in bucket <%s> within 300 milliseconds", object, bucket),
                exception.getMessage(),
                "Error on check object exists fail");
    }

    @Test
    void seObjectExistsFailedOtherDirTest() {
        var bucket = "for-object2";
        var object = "object_test_n";

        minioFastAwait.createBucket(bucket);
        minioFastAwait.cleanBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, "dir/" + object);

        minioFastAwait.seeObjectDoesNotExist(bucket, object);
        Throwable exception = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeObjectExists(bucket, object));

        assertEquals(
                String.format("Object <%s> does not exist in bucket <%s> within 300 milliseconds", object, bucket),
                exception.getMessage(),
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
                String.format("Object <%s> does not exist in bucket <%s> within 400 milliseconds", object, bucket),
                exception.getMessage(),
                "await from specific global");

        minioFastAwait.cleanBucket(bucket);
        Throwable exception2 = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeObjectExists(bucket, object));

        assertEquals(
                String.format("Object <%s> does not exist in bucket <%s> within 300 milliseconds", object, bucket),
                exception2.getMessage(),
                "await rollback to global");
    }

    @Test
    void seObjectNotExistsFailedTest() {
        var bucket = "for-object";
        var object = "object_test_y";

        minio.createBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, object);


        Throwable exception = assertThrows(AssertionError.class, () ->
                minioFastAwait.seeObjectDoesNotExist(bucket, object));

        assertEquals(
                String.format("Object <%s> exists in bucket <%s> within 300 milliseconds", object, bucket),
                exception.getMessage(),
                "Error on check object not exists fail");
    }


    @Test
    void objectCountAssertFailedTest() {
        var bucket = "count-bucket";
        minio.createBucket(bucket);

        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "test/object2.txt");

        Throwable exception = assertThrows(AssertionError.class, () ->
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

        Throwable exception = assertThrows(AssertionError.class, () ->
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

        Throwable exception = assertThrows(AssertionError.class, () ->
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

        Throwable exception = assertThrows(AssertionError.class, () ->
                minio.seeObjectSizeExactly(TEST_BUCKET, object, expectedBytes - 1 ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exception.getMessage(),
                StringContains.containsString("Object <object_size.txt> size from bucket <bucket-test> expected to be equal <179> bytes but got <180>"));

        Throwable exceptionLess = assertThrows(AssertionError.class, () ->
                minio.seeObjectSizeIsLessThan(TEST_BUCKET, object, expectedBytes ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exceptionLess.getMessage(),
                is("Object <object_size.txt> size from bucket <bucket-test> expected to be less <180> bytes but got <180>"));

        Throwable exceptionGreater = assertThrows(AssertionError.class, () ->
                minio.seeObjectSizeIsGreaterThan(TEST_BUCKET, object, expectedBytes ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exceptionGreater.getMessage(),
                is("Object <object_size.txt> size from bucket <bucket-test> expected to be greater <180> bytes but got <180>"));
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
                StringContains.containsString("Object <object_size2.txt> size from bucket <bucket-test> expected to be equal <181> bytes but got <180>"));

        Throwable exceptionLess = assertThrows(AssertionError.class, () ->
                minio.seeObjectSizeIsLessThan(TEST_BUCKET, object, 121 ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exceptionLess.getMessage(),
                is("Object <object_size2.txt> size from bucket <bucket-test> expected to be less <121> bytes but got <180>"));

        Throwable exceptionGreater = assertThrows(AssertionError.class, () ->
                minio.seeObjectSizeIsGreaterThan(TEST_BUCKET, object, 215 ));

        MatcherAssert.assertThat(
                "Error on assert objects size",
                exceptionGreater.getMessage(),
                is("Object <object_size2.txt> size from bucket <bucket-test> expected to be greater <215> bytes but got <180>"));
    }

}
