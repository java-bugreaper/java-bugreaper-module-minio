package net.bugreaper.modules.minio;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import testcontainers.MinioSetup;

import java.util.concurrent.CompletableFuture;

import static java.lang.Thread.sleep;


@SuppressWarnings("squid:S2699")
@Execution(ExecutionMode.CONCURRENT)
class MinioParallelTests {

    private static final Minio minio = MinioSetup.getInstance().getMinio().setDownloadBufferSize(1000);

    Minio minioConf = Minio.getInstance();

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
    void objectCountAssertPassedAwaitTest() {
        var bucket = "count-bucket-2000-pass";
        minio.createBucket(bucket);
        minio.cleanBucket(bucket);

        minio.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");
        minio.uploadFileToBucket(bucket, TEST_FILE, "test/object2.txt");

        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> minio.seeObjectsCountIsExactly(bucket, 3));
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> pushWithSleep(minio, bucket));

        CompletableFuture.allOf(future1, future2).join();
    }

    @Test
    void parallel1() {
        var bucket = "parallel1";
        minioConf.createBucket(bucket);
        minioConf.cleanBucket(bucket);


        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> minioConf.seeBucketIsNotEmpty(bucket));
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> pushWithSleep(minioConf, bucket));

        CompletableFuture.allOf(future1, future2).join();
    }

    @Test
    void parallel2() {
        var bucket = "parallel2";
        minioConf.createBucket(bucket);
        minioConf.cleanBucket(bucket);

        minioConf.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");

        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> minioConf.seeObjectsCountIsGreaterThan(bucket, 1));
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> pushWithSleep(minioConf, bucket));
        CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> minioConf.seeObjectExists(bucket, "object3.txt"));

        CompletableFuture.allOf(future1, future2, future3).join();
    }


    @SuppressWarnings("squid:S2925")
    private void pushWithSleep(Minio obj, String bucket) {
        try {
            sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        obj.uploadFileToBucket(bucket, TEST_FILE, "object3.txt");
    }

}
