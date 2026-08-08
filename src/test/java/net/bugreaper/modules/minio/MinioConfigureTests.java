package net.bugreaper.modules.minio;


import net.bugreaper.modules.minio.exceptions.MinioHelperException;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import testcontainers.MinioContainerSetup;

import static net.bugreaper.core.filereaders.ResourcesFileReader.createResourceFileWithSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;


@SuppressWarnings("squid:S2699")
@Isolated
class MinioConfigureTests extends MinioContainerSetup {

    private static final Minio minio = getMinio().setDownloadBufferSize(1000);


    private static final Minio minioDownload = getMinio()
            .setMaxDownloadObjectSize(10);

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

        Throwable exception = assertThrows(AssertionError.class, () ->
                minio.seeObjectsCountIsExactly(bucket,3));

        MatcherAssert.assertThat(
                "Error on assert objects count in bucket",
                exception.getMessage(),
                is("Expected EXACTLY <3> objects in bucket 'count-bucket-2000', but got <2> within 2 seconds"));
    }

    @Test
    void objectCountAssertFailedCustomAwaitTest() {
        final Minio minioAwait = getMinio().setAwaitMs(200);

        var bucket = "count-bucket-200";
        minioAwait.createBucket(bucket);

        minioAwait.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");


        Throwable exception = assertThrows(AssertionError.class, () ->
                minioAwait.seeObjectsCountIsExactly(bucket,2));

        MatcherAssert.assertThat(
                "Error on assert objects count in bucket",
                exception.getMessage(),
                is("Expected EXACTLY <2> objects in bucket 'count-bucket-200', but got <1> within 200 milliseconds"));
    }

    @Test
    void objectCountAssertFailedCustomAwait2Test() {
        final Minio minioAwait = getMinio().setAwaitMs(1200);

        var bucket = "count-bucket-1200";
        minioAwait.createBucket(bucket);

        minioAwait.uploadFileToBucket(bucket, TEST_FILE, "object1.txt");


        Throwable exception = assertThrows(AssertionError.class, () ->
                minioAwait.seeObjectsCountIsExactly(bucket,2));

        MatcherAssert.assertThat(
                "Error on assert objects count in bucket",
                exception.getMessage(),
                is("Expected EXACTLY <2> objects in bucket 'count-bucket-1200', but got <1> within 1 second 200 milliseconds"));
    }

    @Test
    void maxUploadTest() {

        final Minio minioUpload = getMinio().setMaxUploadSize(30);

        var bucket = "count-upload";
        minioUpload.createBucket(bucket);

        String fileName = "temp/test_size_30.txt";

        createResourceFileWithSize(fileName, 30);

        minioUpload.uploadFileToBucket(bucket, fileName, "object1.txt");

        String fileName2 = "temp/test_size_31.txt";
        createResourceFileWithSize(fileName2, 31);

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minioUpload.uploadFileToBucket(bucket, fileName2, "object2.txt"));


        assertEquals(
                "Upload aborted: file '%s/src/test/resources/temp/test_size_31.txt' size (31 bytes) exceeds the configured maximum (30 bytes). Use setMaxUploadSize to change the limit."
                        .formatted(System.getProperty("user.dir")),
                exception.getMessage());
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

        assertEquals(
                "Download aborted: file 'object2.txt' size (11 bytes) exceeds the configured maximum (10 bytes). Use setMaxDownloadFileSize to change the limit.",
                exception.getMessage());
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


        assertEquals(
                "Download aborted: file 'object2.txt' size (11 bytes) exceeds the configured maximum (10 bytes). Use setMaxDownloadFileSize to change the limit.",
                exception.getMessage());
    }

}
