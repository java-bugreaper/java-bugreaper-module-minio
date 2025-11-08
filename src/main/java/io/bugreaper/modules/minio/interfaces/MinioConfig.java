package io.bugreaper.modules.minio.interfaces;

import io.bugreaper.modules.minio.Minio;


/**
 * Interface for Minio config methods
 *
 * @author ambu550
 */
public interface MinioConfig {


    /**
     * Configure download buffer size
     *
     * @param downloadBufferSize buffer size for file upload
     * @return this instance {@link Minio}
     */
    Minio withDownloadBufferSize(int downloadBufferSize);

    /**
     * Configure await in asserts with await
     *
     * @param awaitMs ms await
     * @return this instance {@link Minio}
     */
    Minio withAwaitMs(int awaitMs);

    /**
     * Configure max size for file to upload
     *
     * @param maxUploadSize max size in bytes for file to upload
     * @return this instance {@link Minio}
     */
    Minio withMaxUploadSize(int maxUploadSize);

    /**
     * Configure max size for file to download/read
     *
     * @param maxDownloadObjectSize max size in bytes for object to download/read
     * @return this instance {@link Minio}
     */
    Minio withMaxDownloadObjectSize(int maxDownloadObjectSize);

}
