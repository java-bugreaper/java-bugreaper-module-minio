package net.bugreaper.modules.minio;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testcontainers.MinioSetup;

import static net.bugreaper.core.filereaders.ResourcesFileReader.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SuppressWarnings("squid:S2699")
class MinioBaseTests {

    private static final Minio minio = MinioSetup.getInstance().getMinio();

    private static final String DEFAULT_BUCKET = "new-bucket";
    private static final String TEST_FILE = "data/test_file_1.txt";
    private static final String TEST_FILE_LINES = "data/test_file_with_lines.txt";
    private static final String TEST_FILE_LINES2 = "data/test_file_with_lines2.txt";


    @BeforeAll
    static void createDefaultBucket() {
        minio.createBucket(DEFAULT_BUCKET);
    }

    @BeforeEach
    void cleanBucket() {
        minio.cleanBucket(DEFAULT_BUCKET);
    }

    @Test
    void createExistingFilledBucketTest() {
        String bucket = "recreate";
        minio.createBucket(bucket);
        minio.uploadFileToBucket(bucket, "data/test_file_1.txt");
        minio.seeBucketIsNotEmpty(bucket);

        minio.createBucket(bucket);
        minio.seeBucketIsNotEmpty(bucket);
    }

    @Test
    void pushFileSameNameTest() {
        minio.uploadFileToBucket(DEFAULT_BUCKET, "data/test_file_1.txt");
        minio.seeObjectExists(DEFAULT_BUCKET, "test_file_1.txt");
    }

    @Test
    void pushFileAndRemoveObjectTest() {
        String obj = "dir/test.obj";

        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, obj);
        minio.seeObjectExists(DEFAULT_BUCKET, obj);

        minio.deleteObjectFromBucket(DEFAULT_BUCKET, obj);
        minio.seeObjectDoesNotExist(DEFAULT_BUCKET, obj);
    }

    @Test
    void pushFileWithOtherObjectNameTest() {
        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "test_obj.txt");
        minio.seeObjectExists(DEFAULT_BUCKET, "test_obj.txt");
    }

    @Test
    void pushFileWithOtherObjectDirNameTest() {
        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "some_new_dir/object.txt");
        minio.seeObjectExists(DEFAULT_BUCKET, "some_new_dir/object.txt");
    }

    @Test
    void readFileTest() {

        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "object.txt");

        assertEquals(
                minio.readObjectFromBucket(DEFAULT_BUCKET, "object.txt"),
                readResourceFile(TEST_FILE),
                "Object read content successful");
    }

    @Test
    void readFileWithLinesTest() {

        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE_LINES, "object2.txt");

        assertEquals(
                minio.readObjectFromBucket(DEFAULT_BUCKET, "object2.txt"),
                readResourceFile(TEST_FILE_LINES),
                "Object read content successful");
    }

    @Test
    void downloadFileTest() {
        var file = "temp/saved_file_txt";

        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "object.txt");
        minio.downloadObjectFromBucket(DEFAULT_BUCKET, "object.txt", file);

        assertEquals(readResourceFile(file), readResourceFile(TEST_FILE), "File read content successful");
    }

    @Test
    void downloadFileWithLinesTest() {

        var file = "temp/saved_file2_txt";

        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE_LINES2, "object.txt");
        minio.downloadObjectFromBucket(DEFAULT_BUCKET, "object.txt", file);

        assertEquals(readResourceFile(file), readResourceFile(TEST_FILE_LINES2), "File read content successful");
    }

    @Test
    void checkObjectExistsTest() {

        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "object.txt");

        var actual = minio.objectExistsStatus(DEFAULT_BUCKET, "object.txt");
        var expected = true;

        minio.seeObjectExists(DEFAULT_BUCKET, "object.txt");

        assertEquals(expected, actual, "Get object exists status:" + expected);
    }

    @Test
    void checkObjectNotExistsTest() {
        var actual = minio.objectExistsStatus(DEFAULT_BUCKET, "not_exist.txt");
        var expected = false;

        minio.seeObjectDoesNotExist(DEFAULT_BUCKET, "not_exist.txt");

        assertEquals(expected, actual, "Get object exists status:" + expected);
    }


    @Test
    void getObjectSizeTest() {
        String fileName = "temp/test_size_1.txt";
        long expectedBytes = 177;

        createResourceFileWithSize(fileName, expectedBytes);

        minio.uploadFileToBucket(DEFAULT_BUCKET, fileName, "object_size.txt");

        long actual = minio.getObjectSize(DEFAULT_BUCKET, "object_size.txt");

        assertEquals(expectedBytes, actual, "Get object size: " + expectedBytes);
    }

    @Test
    void seeObjectSizeExactlyGreaterLessTest() {
        String fileName = "temp/test_size.txt";
        String object = "object_size.txt";
        long expectedBytes = 180;

        createResourceFileWithSize(fileName, expectedBytes);

        minio.uploadFileToBucket(DEFAULT_BUCKET, fileName, object);

        minio.seeObjectSizeExactly(DEFAULT_BUCKET, object, expectedBytes);
        minio.seeObjectSizeIsGreaterThan(DEFAULT_BUCKET, object, expectedBytes - 1);
        minio.seeObjectSizeIsLessThan(DEFAULT_BUCKET, object, expectedBytes + 1);

    }

    @Test
    void getObjectsListTest() {
        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "test/object2.txt");
        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "object1.txt");


        minio.getObjectsList(DEFAULT_BUCKET)
                .seeListHasExactlyCount(2)
                .seeListAnyEquals("object1.txt")
                .seeListAnyContains("test/object2.txt");
    }

    @Test
    void getObjectsCountTest() {

        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "test/object2.txt");

        assertEquals(2, (minio.getObjectsCountInBucket(DEFAULT_BUCKET)), "Get objects list count");
    }

    @Test
    void getObjectsCountAssetTest() {

        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "test/object2.txt");

        minio.seeObjectsCountIsExactly(DEFAULT_BUCKET, 2);
    }

    @Test
    void cleanBucketTest() {
        var bucket = "some-bucket";
        minio.createBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.seeBucketIsNotEmpty(bucket);
        minio.cleanBucket(bucket);

        assertEquals(0, minio.getObjectsCountInBucket(bucket), "Bucket cleaned");
    }

    @Test
    void seeBucketCreateTwice() {
        String bucket = "double-bucket";
        minio.createBucket(bucket);
        minio.createBucket(bucket);
    }

    @Test
    void seeBucketCreateAndExistsEmptyTest() {
        String bucket = "new-bucket";
        minio.createBucket(bucket);
        minio.seeBucketExists(bucket);
        minio.seeBucketIsEmpty(bucket);
    }

    @Test
    void createBucketAndDeleteTest() {
        String bucket = "bucket-for-delete";
        minio.createBucket(bucket);
        minio.deleteEmptyBucket(bucket);
        minio.seeBucketDoesNotExist(bucket);
    }

    @Test
    void deleteFilledBucketTest() {
        String bucket = "bucket-for-delete-filled";
        minio.createBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.seeBucketIsNotEmpty(bucket);

        minio.deleteFilledBucket(bucket);
        minio.seeBucketDoesNotExist(bucket);

    }

    @Test
    void greaterCountTest() {
        String bucket = "bucket-greater";
        minio.createBucket(bucket);
        minio.cleanBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "object2.txt");

        minio.seeObjectsCountIsGreaterThan(bucket, 1);

    }


    @Test
    void lessCountTest() {
        String bucket = "bucket-less";
        minio.createBucket(bucket);
        minio.cleanBucket(bucket);
        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "object2.txt");

        minio.seeObjectsCountIsLessThan(bucket, 3);

    }

    @Test
    void createAndShareObjectTest() {
        var object = "object_share";

        minio.createBucket(DEFAULT_BUCKET);
        minio.cleanBucket(DEFAULT_BUCKET);
        minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, object);

        //share object
        String url = minio.shareObjectInBucket(DEFAULT_BUCKET, object);

        //read file check
        String content = minio.getObjectsBySharedLink(url);
        assertEquals(readResourceFile(TEST_FILE), content);


        //download file check
        String contentFile = "temp/shared_file.txt";
        minio.downloadObjectBySharedLink(url, contentFile);
        assertEquals(readResourceFile(TEST_FILE), readResourceFile(contentFile));

    }

    @Test
    void getBucketListTest() {

        var bucket = "bucket-test-1";

        minio.createBucket(bucket);

        minio.getBucketsList()
                .seeListAnyEquals(bucket)
                .seeListAnyContains("bucket-test");
    }

}
