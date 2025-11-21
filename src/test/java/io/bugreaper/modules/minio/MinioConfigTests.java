package io.bugreaper.modules.minio;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import testcontainers.MinioSetup;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SuppressWarnings("squid:S2699")
class MinioConfigTests {

    private static final Minio minio = MinioSetup.getInstance().getMinio();

    private static final String DEFAULT_BUCKET = "new-bucket";
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
    @Order(1)
    void testConfigWithRequiredFields() {
        //config in bugreaper.yml / bugreaper-docker.yml for-ci
        Minio minioConfig = new Minio();
        minioConfig.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "test_obj.txt");
        minioConfig.seeObjectExists(DEFAULT_BUCKET, "test_obj.txt");

        assertEquals(300, minioConfig.getAwaitMs());
    }

}
