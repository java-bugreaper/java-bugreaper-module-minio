package io.bugreaper.modules.minio;


import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import testcontainers.MinioSetup;

import static org.junit.jupiter.api.Assertions.assertThrows;


@SuppressWarnings("squid:S2699")
class MinioConfigureValidationTests {

    @Test
    void configMinusAwaitTest() {

        Minio test = MinioSetup.getInstance().getMinio();

        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                test.withAwaitMs(-1));

        MatcherAssert.assertThat(
                "Error on config .withAwaitMs negative validation",
                exception.getMessage(),
                StringContains.containsString("awaitMs too small (can`t bee less 200ms)"));
    }

    @Test
    void configMinusDownloadBufferTest() {

        Minio test = MinioSetup.getInstance().getMinio();

        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                test.withDownloadBufferSize(-1));

        MatcherAssert.assertThat(
                "Error on config .withDownloadBufferSize negative validation",
                exception.getMessage(),
                StringContains.containsString("downloadBufferSize too small (can`t bee less 1)"));
    }

    @Test
    void configMinusUploadSizeTest() {

        Minio test = MinioSetup.getInstance().getMinio();

        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                test.withMaxUploadSize(-1));

        MatcherAssert.assertThat(
                "Error on config .withMaxUploadSize negative validation",
                exception.getMessage(),
                StringContains.containsString("maxUploadSize too small (can`t bee less 1)"));
    }

    @Test
    void configMinusMaxDownloadObjectSizeTest() {

        Minio test = MinioSetup.getInstance().getMinio();

        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                test.withMaxDownloadObjectSize(-1));

        MatcherAssert.assertThat(
                "Error on config .withMaxDownloadObjectSize negative validation",
                exception.getMessage(),
                StringContains.containsString("maxDownloadObjectSize too small (can`t bee less 1)"));
    }

}
