package io.bugreaper.modules.minio;


import io.bugreaper.modules.minio.exceptions.MinioHelperException;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testcontainers.MinioSetup;

import java.util.concurrent.CompletableFuture;

import static java.lang.Thread.sleep;
import static io.bugreaper.core.filereaders.ResourcesFileReader.createResourceFileWithSize;
import static org.junit.jupiter.api.Assertions.*;


@SuppressWarnings("squid:S2699")
class MinioConfigureTests {

    private static final Minio minio = MinioSetup.getInstance().getMinio().withDownloadBufferSize(1000);
    private static final Minio minioAwait = MinioSetup.getInstance().getMinio().withAwaitMs(200);

    private static final Minio minioDownload = MinioSetup.getInstance().getMinio()
            .withMaxDownloadObjectSize(10);

    private static final String DEFAULT_BUCKET = "bucket-configured";
    private static final String TEST_FILE = "data/test_file_1.txt";


    @BeforeAll
    static void createDefaultBucket() {
        minio.createBucket(DEFAULT_BUCKET);
    }

    @BeforeEach
    void cleanBucket() {
        minio.cleanBucket(DEFAULT_BUCKET);
    }



    @Test
    void objectCountAssertFailedTest() {
        var bucket = "count-bucket-2000";
        minio.createBucket(bucket);

        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "test/object2.txt");

        Throwable exception = assertThrows(ConditionTimeoutException.class, () ->
                minio.assertCountObjectsInBucketExactly(bucket,3));

        MatcherAssert.assertThat(
                "Error on assert objects count in bucket",
                exception.getMessage(),
                StringContains.containsString("Count objects from bucket <count-bucket-2000> expected be exactly <3> ==> expected: <3> but was: <2> within 2 seconds."));
    }

    @Test
    void objectCountAssertFailedCustomAwaitTest() {
        var bucket = "count-bucket-200";
        minioAwait.createBucket(bucket);

        minioAwait.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");


        Throwable exception = assertThrows(ConditionTimeoutException.class, () ->
                minioAwait.assertCountObjectsInBucketExactly(bucket,2));

        MatcherAssert.assertThat(
                "Error on assert objects count in bucket",
                exception.getMessage(),
                StringContains.containsString("Count objects from bucket <count-bucket-200> expected be exactly <2> ==> expected: <2> but was: <1> within 200 milliseconds."));
    }
    @Test
    void objectCountAssertPassedAwaitTest() {
        var bucket = "count-bucket-2000-pass";
        minio.createBucket(bucket);
        minio.cleanBucket(bucket);

        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "test/object2.txt");

        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> minio.assertCountObjectsInBucketExactly(bucket,3));
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> pushWithSleep(bucket));

        CompletableFuture.allOf(future1, future2).join();
    }

    @SuppressWarnings("squid:S2925")
    private void pushWithSleep(String bucket){
        try {
            sleep(700);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        minio.uploadFileToBucket(bucket, TEST_FILE, "object3.txt");
    }

    @Test
    void maxUploadTest() {

        final Minio minioUpload = MinioSetup.getInstance().getMinio().withMaxUploadSize(30);

        var bucket = "count-upload";
        minioUpload.createBucket(bucket);

        String fileName = "temp/test_size_30.txt";

        createResourceFileWithSize(fileName, 30);

        minioUpload.uploadFileToBucket(bucket, fileName, "object1.txt");

        String fileName2 = "temp/test_size_31.txt";
        createResourceFileWithSize(fileName2, 31);

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minioUpload.uploadFileToBucket(bucket, fileName2, "object2.txt"));

        MatcherAssert.assertThat(
                "Error on abort file upload (max size limit)",
                exception.getMessage(),
                StringContains.containsString("size:<31> more maximum in config 30bytes"));
    }

    @Test
    void maxObjectReadTest() {
        var bucket = "count-download";
        minioDownload.createBucket(bucket);

        String fileName = "temp/test_size_10.txt";

        createResourceFileWithSize(fileName, 10);

        minioDownload.uploadFileToBucket(bucket, fileName, "object1.txt");
        minioDownload.readObjectFromBucket(bucket, "object1.txt");

        String fileName2 = "temp/test_size_11.txt";
        createResourceFileWithSize(fileName2, 11);

        minioDownload.uploadFileToBucket(bucket, fileName2, "object2.txt");

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minioDownload.readObjectFromBucket(bucket, "object2.txt"));

        MatcherAssert.assertThat(
                "Error on abort object read (max size limit)",
                exception.getMessage(),
                StringContains.containsString("Download aborted, object <object2.txt> size:<11> more maximum in config 10bytes"));
    }

    @Test
    void maxObjectDownloadTest() {
        var bucket = "count-download2";
        minioDownload.createBucket(bucket);

        String fileName = "temp/test_size_10.txt";

        createResourceFileWithSize(fileName, 10);

        minioDownload.uploadFileToBucket(bucket, fileName, "object1.txt");
        minioDownload.downloadObjectFromBucket(bucket, "object1.txt", "temp/saved1.txt");

        String fileName2 = "temp/test_size_11.txt";
        createResourceFileWithSize(fileName2, 11);

        minioDownload.uploadFileToBucket(bucket, fileName2, "object2.txt");

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minioDownload.downloadObjectFromBucket(bucket, "object2.txt", "temp/saved2.txt"));

        MatcherAssert.assertThat(
                "Error on abort object read (max size limit)",
                exception.getMessage(),
                StringContains.containsString("Download aborted, object <object2.txt> size:<11> more maximum in config 10bytes"));
    }

}
