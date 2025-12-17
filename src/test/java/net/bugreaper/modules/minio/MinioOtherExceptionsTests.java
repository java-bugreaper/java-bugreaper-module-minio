package net.bugreaper.modules.minio;


import net.bugreaper.modules.minio.exceptions.MinioHelperException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import testcontainers.MinioSetup;

import static org.junit.jupiter.api.Assertions.assertThrows;


class MinioOtherExceptionsTests {


    @Test
    void wrongPasswordErrorTest() {

        final Minio minioWrong = MinioSetup.getInstance().getMinio("admin", "dummy");

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minioWrong.createBucket("some"));

        MatcherAssert.assertThat(
                "Error on wrong password",
                exception.getMessage(),
                StringContains.containsString("The request signature we calculated does not match the signature you provided"));
    }

    @Test
    void wrongUserErrorTest() {

        final Minio minioWrong = MinioSetup.getInstance().getMinio("dummy", "password");

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minioWrong.downloadObjectFromBucket("some","object","/temp/file_wrong"));

        MatcherAssert.assertThat(
                "Error on wrong password",
                exception.getMessage(),
                StringContains.containsString("The Access Key Id you provided does not exist"));
    }
}
