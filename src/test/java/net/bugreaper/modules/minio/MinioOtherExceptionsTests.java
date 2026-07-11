package net.bugreaper.modules.minio;


import net.bugreaper.modules.minio.exceptions.MinioHelperException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import testcontainers.MinioContainerSetup;

import static org.junit.jupiter.api.Assertions.assertThrows;


@Isolated
class MinioOtherExceptionsTests extends MinioContainerSetup {


    @Test
    void wrongPasswordErrorTest() {

        final Minio minioWrong = getMinio("admin", "dummy");

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minioWrong.createBucket("some"));

        MatcherAssert.assertThat(
                "Error on wrong password",
                exception.getMessage(),
                StringContains.containsString("The request signature we calculated does not match the signature you provided"));
    }

    @Test
    void wrongUserErrorTest() {

        final Minio minioWrong = getMinio("dummy", "password");

        Throwable exception = assertThrows(MinioHelperException.class, () ->
                minioWrong.downloadObjectFromBucket("some","object","/temp/file_wrong"));

        MatcherAssert.assertThat(
                "Error on wrong password",
                exception.getMessage(),
                StringContains.containsString("The Access Key Id you provided does not exist"));
    }
}
