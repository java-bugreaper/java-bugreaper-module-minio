package io.bugreaper.modules.minio;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testcontainers.MinioSetup;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SuppressWarnings("squid:S2699")
class MinioConfigTests {

    private static final Minio minio = MinioSetup.getInstance().getMinio();

    private static final String DEFAULT_BUCKET = "new-bucket";
    private static final String TEST_FILE = "data/test_file_1.txt";

    private static final String CI = System.getenv("CI");
    public static final String PROPERTY = "bugreaperEnv";


    @BeforeAll
    static void createDefaultBucket() {
        minio.createBucket(DEFAULT_BUCKET);
    }

    @BeforeEach
    void cleanBucket() {
        minio.cleanBucket(DEFAULT_BUCKET);
    }


    @Test
    void testConfigWithAllFields() {
        if(Objects.equals(CI, "true")){
            System.setProperty(PROPERTY, "docker");
        }else {
            System.clearProperty(PROPERTY);
        }

        Minio minioConfig = new Minio();
        minioConfig.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "test_obj.txt");
        minioConfig.seeObjectExists(DEFAULT_BUCKET, "test_obj.txt");

        assertEquals(300, minioConfig.getAwaitMs());

        assertEquals("""
                        Minio:
                            awaitMs=300
                            downloadBufferSize=10240
                            maxUploadFileSize=1024
                            maxDownloadObjectSize=2048
                        """,
                minioConfig.getConfigSummary());
    }

    @Test
    void testConfigWithRequiredFieldsOnly() {
        if(Objects.equals(CI, "true")){
            System.setProperty(PROPERTY, "docker-noopt");
        }else {
            System.setProperty(PROPERTY, "noopt");
        }

        Minio minioConfig = new Minio();
        assertEquals("""
                        Minio:
                            awaitMs=2000
                            downloadBufferSize=10240
                            maxUploadFileSize=20480
                            maxDownloadObjectSize=51200
                        """,
                minioConfig.getConfigSummary());
    }

}
